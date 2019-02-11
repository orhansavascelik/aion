package org.aion.api.server.zmq;

import static org.aion.api.server.pb.ApiAion0.JAVAAPI_VAR;
import static org.zeromq.ZMQ.DEALER;
import static org.zeromq.ZMQ.ROUTER;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.aion.api.server.ApiUtil;
import org.aion.api.server.pb.IHdlr;
import org.aion.api.server.pb.Message;
import org.aion.api.server.types.EvtContract;
import org.aion.api.server.types.Fltr;
import org.aion.api.server.types.TxPendingStatus;
import org.aion.type.api.util.ByteUtil;
import org.aion.type.api.util.Hex;
import org.aion.log.AionLoggerFactory;
import org.aion.log.LogEnum;
import org.aion.mcf.config.CfgApiZmq;
import org.slf4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class ProtocolProcessor implements Runnable {

    protected static final Logger LOG = AionLoggerFactory.getLogger(LogEnum.API.name());
    private static final String AION_ZMQ_WK_TH = "inproc://aionZmqWkTh";
    private static final String AION_ZMQ_CB_TH = "inproc://aionZmqCbTh";
    private static final String AION_ZMQ_EV_TH = "inproc://aionZmqEvTh";
    private static final String AION_ZMQ_HB_TH = "inproc://aionZmqHbTh";
    private final Path PATH;
    private static final long zmqHWM = 100_000;
    private static final int SOCKETID_LEN = 5;
    private static final int SOCKET_RECV_TIMEOUT = 3000;

    private final IHdlr handler;
    private CfgApiZmq cfgApi;
    private AtomicBoolean shutDown = new AtomicBoolean();
    private byte[] curvePubKey;
    private byte[] curveSecKey;

    public ProtocolProcessor(IHdlr _handler, final CfgApiZmq cfg) {
        this.handler = _handler;
        this.cfgApi = cfg;

        String storageDir = System.getProperty("local.storage.dir");
        if (storageDir == null || storageDir.equalsIgnoreCase("")) {
            storageDir = System.getProperty("user.dir");
        }

        String curveKeyPath = storageDir + "/" + CfgApiZmq.ZMQ_KEY_DIR;
        PATH = Paths.get(curveKeyPath);
    }

    public void shutdown() throws InterruptedException {
        handler.shutDown();
        shutDown.set(true);
        Thread.sleep(SOCKET_RECV_TIMEOUT);
        Proxy.shutdown();
    }

    @Override
    public void run() {
        LOG.info("Starting Aion Api Server <port={}>", cfgApi.getPort());
        String bindAddr = "tcp://" + cfgApi.getIp() + ":" + cfgApi.getPort();
        int msgTh = 5;

        try {
            // create context.
            Context ctx = ZMQ.context(1);

            // create router sock.
            Socket feSock = ctx.socket(ROUTER);
            if (cfgApi.isSecureConnectEnabledEnabled()) {
                // Currently the system will only load the first pair of the key files.
                loadCurveKeyPair();

                if (curveSecKey != null && curvePubKey != null) {
                    feSock.setZAPDomain("global".getBytes());
                    feSock.setCurveServer(true);
                    feSock.setCurvePublicKey(curvePubKey);
                    feSock.setCurveSecretKey(curveSecKey);
                    LOG.info("Secure connection enabled!");
                } else {
                    LOG.info(
                            "Can't find the keyfile for setup the connection. Secure connection disabled!");
                }
            } else {
                LOG.info("Secure connection disabled!");
            }

            feSock.setSndHWM(zmqHWM);
            feSock.bind(bindAddr);

            Socket wkSocks = ctx.socket(DEALER);
            wkSocks.bind(AION_ZMQ_WK_TH);

            Socket cbSock = ctx.socket(DEALER);
            cbSock.bind(AION_ZMQ_CB_TH);

            Socket evSock = ctx.socket(DEALER);
            evSock.bind(AION_ZMQ_EV_TH);

            Socket hbSock = ctx.socket(DEALER);
            hbSock.bind(AION_ZMQ_HB_TH);

            ExecutorService es = Executors.newFixedThreadPool(msgTh);
            es.execute(() -> callbackRun(ctx));
            es.execute(this::txWaitRun);
            es.execute(() -> eventRun(ctx));
            es.execute(() -> workerRun(ctx));
            es.execute(() -> hbRun(ctx));

            Proxy.proxy(feSock, wkSocks, cbSock, evSock, hbSock);

            if (LOG.isInfoEnabled()) {
                LOG.info("ProtocolProcessor.run thread finish.");
            }

            if (LOG.isInfoEnabled()) {
                LOG.info("Shutting down Zmq sockets...");
            }
            // Shutdown HdlrZmq
            ((HdlrZmq) handler).shutdown();
            // Shutdown ZmqSocket
            feSock.close();
            wkSocks.close();
            cbSock.close();
            evSock.close();
            hbSock.close();
            // Shutdown ExecutorService
            es.shutdown();

            ctx.close();
            if (LOG.isInfoEnabled()) {
                LOG.info("Shutdown Zmq sockets... Done!");
            }

        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("ProtocolProcessor.run exception: " + e.getMessage());
            }
        }
    }

    private void loadCurveKeyPair() {
        List<File> files = org.aion.type.api.io.File.getFiles(PATH);
        String nextLoad = "";
        for (File f : files) {
            if (f.getName().contains("zmqCurvePubkey")) {
                try {
                    curvePubKey = Files.readAllBytes(f.toPath());
                    nextLoad = f.getName().replace("zmqCurvePubkey", "zmqCurveSeckey");
                } catch (IOException e) {
                    LOG.error("Get zmqCurvePubkey exception! {}", e.toString());
                }
            } else if (f.getName().contains("zmqCurveSeckey")) {
                try {
                    curveSecKey = Files.readAllBytes(f.toPath());
                    nextLoad = f.getName().replace("zmqCurveSeckey", "zmqCurvePubkey");
                } catch (IOException e) {
                    LOG.error("Get zmqCurveSeckey exception! {}", e.toString());
                }
            } else if (nextLoad.contentEquals(f.getName())) {
                try {
                    if (nextLoad.contains("zmqCurveSeckey")) {
                        curveSecKey = Files.readAllBytes(f.toPath());
                    } else {
                        curvePubKey = Files.readAllBytes(f.toPath());
                    }
                } catch (IOException e) {
                    LOG.error("Get zmqCurveSeckey exception! {}", e.toString());
                }
                break;
            }
        }
    }

    private void eventRun(Context ctx) {
        Socket sock = ctx.socket(ZMQ.DEALER);
        sock.connect(AION_ZMQ_EV_TH);
        while (!shutDown.get()) {
            Map<Long, Fltr> filters = ((HdlrZmq) this.handler).getFilter();
            for (Long i : filters.keySet()) {
                Fltr f = filters.get(i);

                if (LOG.isTraceEnabled()) {
                    LOG.trace("ProtocolProcessor.eventRun fltr type:{}", f.getType());
                }

                if (f.getType() == Fltr.Type.EVENT) {
                    Object[] objs = f.poll();
                    List<Message.t_EventCt> al = new ArrayList<>();
                    for (Object obj : objs) {
                        al.add(((EvtContract) obj).getMsgEventCt());
                        if (LOG.isTraceEnabled()) {
                            LOG.trace(
                                    "ProtocolProcessor.eventRun fltr event[{}]",
                                    ((EvtContract) obj).toJSON());
                        }
                    }

                    if (!al.isEmpty()) {
                        Message.rsp_EventCtCallback ecb =
                                Message.rsp_EventCtCallback.newBuilder().addAllEc(al).build();
                        byte[] rsp = ((HdlrZmq) this.handler).toRspEvtMsg(ecb.toByteArray());

                        try {
                            byte[] socketId =
                                    ByteBuffer.allocate(5)
                                            .put(ByteUtil.longToBytes(i), 3, 5)
                                            .array();
                            sock.send(socketId, ZMQ.SNDMORE);
                            sock.send(rsp, ZMQ.DONTWAIT);
                        } catch (Exception e) {
                            LOG.error(
                                    "ProtocolProcessor.callbackRun sock.send exception: "
                                            + e.getMessage());
                        }
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOG.error("eventRun InterruptedException {}", e);
            }
        }
        sock.close();
        if (LOG.isDebugEnabled()) {
            LOG.debug("close eventRun sockets...");
        }
    }

    private void txWaitRun() {
        while (!shutDown.get()) {
            ((HdlrZmq) this.handler).getTxWait();
        }
    }

    private void callbackRun(Context ctx) {
        Socket sock = ctx.socket(ZMQ.DEALER);
        sock.connect(AION_ZMQ_CB_TH);

        while (!shutDown.get()) {
            TxPendingStatus tps;
            try {
                tps = ((HdlrZmq) this.handler).getTxStatusQueue().take();
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                if (LOG.isErrorEnabled()) {
                    LOG.error("queue take exception - [{}]", e1.getMessage());
                }
                continue;
            }

            if (tps.isEmpty()) {
                continue;
            }

            byte[] rsp =
                    tps.toTxReturnCode() != 105
                            ? ((HdlrZmq) this.handler)
                                    .toRspMsg(
                                            tps.getMsgHash(), tps.toTxReturnCode(), tps.getError())
                            : ((HdlrZmq) this.handler)
                                    .toRspMsg(
                                            tps.getMsgHash(),
                                            tps.toTxReturnCode(),
                                            tps.getError(),
                                            tps.getTxResult());
            if (LOG.isTraceEnabled()) {
                LOG.trace(
                        "callbackRun send. socketID: [{}], msgHash: [{}], txReturnCode: [{}]/n rspMsg: [{}]",
                        Hex.toHexString(tps.getSocketId()),
                        Hex.toHexString(tps.getMsgHash()),
                        tps.toTxReturnCode(),
                        Hex.toHexString(rsp));
            }
            try {
                sock.send(tps.getSocketId(), ZMQ.SNDMORE);
                sock.send(rsp, ZMQ.DONTWAIT);
            } catch (Exception e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(
                            "ProtocolProcessor.callbackRun sock.send exception: " + e.getMessage());
                }
            }
        }
        sock.close();
        if (LOG.isDebugEnabled()) {
            LOG.debug("close callbackRun sockets...");
        }
    }

    private void workerRun(ZMQ.Context ctx) {
        Socket sock = ctx.socket(ZMQ.DEALER);
        sock.connect(AION_ZMQ_WK_TH);
        sock.setReceiveTimeOut(SOCKET_RECV_TIMEOUT);

        while (!shutDown.get()) {
            try {
                byte[] socketId = sock.recv(0);
                if (LOG.isTraceEnabled()) {
                    LOG.trace(
                            "ProtocolProcessor.workerRun socketID: [{}]",
                            Hex.toHexString(socketId));
                }
                if (socketId != null && socketId.length == SOCKETID_LEN) {
                    byte[] req = sock.recv(0);
                    if (req != null) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace(
                                    "ProtocolProcessor.workerRun reqMsg: [{}]",
                                    Hex.toHexString(req));
                        }
                        byte[] rsp = ((HdlrZmq) this.handler).process(req, socketId);
                        if (LOG.isTraceEnabled()) {
                            LOG.trace(
                                    "ProtocolProcessor.workerRun rspMsg: [{}]",
                                    Hex.toHexString(rsp));
                        }

                        try {
                            sock.send(socketId, ZMQ.SNDMORE);
                            sock.send(rsp, ZMQ.DONTWAIT);
                        } catch (Exception e) {
                            if (LOG.isErrorEnabled()) {
                                LOG.error(
                                        "ProtocolProcessor.workerRun sock.send exception: "
                                                + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("ProtocolProcessor workerRun exception!! " + e.getMessage());
                }
            }
        }
        sock.close();
        if (LOG.isDebugEnabled()) {
            LOG.debug("close workerRun sockets...");
        }
    }

    private void hbRun(ZMQ.Context ctx) {
        Socket sock = ctx.socket(ZMQ.DEALER);
        sock.connect(AION_ZMQ_HB_TH);
        sock.setReceiveTimeOut(SOCKET_RECV_TIMEOUT);

        while (!shutDown.get()) {
            try {
                byte[] socketId = sock.recv(0);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("ProtocolProcessor.hbRun socketID: [{}]", Hex.toHexString(socketId));
                }
                if (socketId != null && socketId.length == SOCKETID_LEN) {
                    byte[] req = sock.recv(0);
                    if (req != null) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("ProtocolProcessor.hbRun reqMsg: [{}]", Hex.toHexString(req));
                        }
                        byte[] rsp =
                                ApiUtil.toReturnHeader(
                                        JAVAAPI_VAR, Message.Retcode.r_heartbeatReturn_VALUE);
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("ProtocolProcessor.hbRun rspMsg: [{}]", Hex.toHexString(rsp));
                        }

                        try {
                            sock.send(socketId, ZMQ.SNDMORE);
                            sock.send(rsp, ZMQ.DONTWAIT);
                        } catch (Exception e) {
                            if (LOG.isErrorEnabled()) {
                                LOG.error(
                                        "ProtocolProcessor.hbRun sock.send exception: "
                                                + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("ProtocolProcessor hbRun exception!! " + e.getMessage());
                }
            }
        }
        sock.close();
        if (LOG.isDebugEnabled()) {
            LOG.debug("close hbRun sockets...");
        }
    }
}
