/*
 * Copyright (c) 2017-2018 Aion foundation.
 *
 *     This file is part of the aion network project.
 *
 *     The aion network project is free software: you can redistribute it
 *     and/or modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation, either version 3 of
 *     the License, or any later version.
 *
 *     The aion network project is distributed in the hope that it will
 *     be useful, but WITHOUT ANY WARRANTY; without even the implied
 *     warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *     See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with the aion network project source files.
 *     If not, see <https://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Aion foundation.
 */

package org.aion.api.server;

import static org.aion.evtmgr.impl.evt.EventTx.STATE.GETSTATE;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.aion.api.server.nrgprice.NrgOracle;
import org.aion.api.server.types.ArgTxCall;
import org.aion.api.server.types.Fltr;
import org.aion.api.server.types.SyncInfo;
import org.aion.api.server.types.TxRecpt;
import org.aion.base.type.Address;
import org.aion.base.type.ITransaction;
import org.aion.base.type.ITxReceipt;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.base.util.ByteUtil;
import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.evtmgr.IEvent;
import org.aion.evtmgr.IEventMgr;
import org.aion.evtmgr.IHandler;
import org.aion.evtmgr.impl.es.EventExecuteService;
import org.aion.evtmgr.impl.evt.EventBlock;
import org.aion.evtmgr.impl.evt.EventTx;
import org.aion.mcf.blockchain.TxResponse;
import org.aion.mcf.vm.Constants;
import org.aion.zero.impl.AionGenesis;
import org.aion.zero.impl.BlockContext;
import org.aion.zero.impl.Version;
import org.aion.zero.impl.blockchain.AionPendingStateImpl;
import org.aion.zero.impl.blockchain.IAionChain;
import org.aion.zero.impl.config.CfgAion;
import org.aion.zero.impl.core.IAionBlockchain;
import org.aion.zero.impl.db.AionBlockStore;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.impl.types.AionBlockSummary;
import org.aion.zero.impl.types.AionTxInfo;
import org.aion.zero.types.AionTransaction;
import org.aion.zero.types.AionTxReceipt;

public abstract class ApiAion extends Api {

    // these variables get accessed by the api worker threads.
    // need to guarantee one of:
    // 1. all access to variables protected by some lock
    // 2. underlying datastructure provides concurrency guarntees

    // delegate concurrency to underlying object
    private static NrgOracle NRG_ORACLE;
    protected IAionChain ac; // assumption: blockchainImpl et al. provide concurrency guarantee

    // using java.util.concurrent library objects
    protected AtomicLong fltrIndex; // AtomicLong
    protected Map<Long, Fltr> installedFilters; // ConcurrentHashMap
    protected Map<ByteArrayWrapper, AionTxReceipt> pendingReceipts; // Collections.synchronizedMap

    // 'safe-publishing' idiom
    private volatile double reportedHashrate = 0; // volatile, used only for 'publishing'

    // thread safe because value never changing, can be safely read by multiple threads
    protected final String[] compilers = new String[] {"solidity"};
    protected final short FLTRS_MAX = 1024;
    protected final String clientVersion = computeClientVersion();

    private ReentrantLock blockTemplateLock;
    private volatile BlockContext currentTemplate;
    private byte[] currentBestBlockHash;

    protected EventExecuteService ees;

    public ApiAion(final IAionChain _ac) {
        this.ac = _ac;
        this.installedFilters = new ConcurrentHashMap<>();
        this.fltrIndex = new AtomicLong(0);
        this.blockTemplateLock = new ReentrantLock();

        // register events
        IEventMgr evtMgr = this.ac.getAionHub().getEventMgr();
        evtMgr.registerEvent(
                Collections.singletonList(new EventTx(EventTx.CALLBACK.PENDINGTXUPDATE0)));
        evtMgr.registerEvent(
                Collections.singletonList(new EventBlock(EventBlock.CALLBACK.ONBLOCK0)));
    }

    public final class EpApi implements Runnable {
        boolean go = true;

        @Override
        public void run() {
            while (go) {
                try {
                    IEvent e = ees.take();
                    if (e.getEventType() == IHandler.TYPE.BLOCK0.getValue()
                            && e.getCallbackType() == EventBlock.CALLBACK.ONBLOCK0.getValue()) {
                        onBlock((AionBlockSummary) e.getFuncArgs().get(0));
                    } else if (e.getEventType() == IHandler.TYPE.TX0.getValue()) {
                        if (e.getCallbackType() == EventTx.CALLBACK.PENDINGTXUPDATE0.getValue()) {
                            pendingTxUpdate(
                                    (ITxReceipt) e.getFuncArgs().get(0),
                                    GETSTATE((int) e.getFuncArgs().get(1)));
                        } else if (e.getCallbackType()
                                == EventTx.CALLBACK.PENDINGTXRECEIVED0.getValue()) {
                            for (ITransaction tx : (List<ITransaction>) e.getFuncArgs().get(0)) {
                                pendingTxReceived(tx);
                            }
                        }
                    } else if (e.getEventType() == IHandler.TYPE.POISONPILL.getValue()) {
                        go = false;
                    }
                } catch (Exception e) {
                    LOG.debug("EpApi - excepted out", e);
                }
            }
        }
    }

    protected abstract void onBlock(AionBlockSummary cbs);

    protected abstract void pendingTxReceived(ITransaction _tx);

    protected abstract void pendingTxUpdate(ITxReceipt _txRcpt, EventTx.STATE _state);

    // General Level
    public byte getApiVersion() {
        return 2;
    }

    protected Map<Long, Fltr> getInstalledFltrs() {
        return installedFilters;
    }

    public String getCoinbase() {
        String coinbase = CfgAion.inst().getConsensus().getMinerAddress();
        return TypeConverter.toJsonHex(coinbase);
    }

    @Override
    public AionBlock getBestBlock() {
        return this.ac.getBlockchain().getBestBlock();
    }

    protected BlockContext getBlockTemplate() {

        blockTemplateLock.lock();
        try {
            AionBlock bestBlock =
                    ((AionPendingStateImpl) ac.getAionHub().getPendingState()).getBestBlock();
            byte[] bestBlockHash = bestBlock.getHeader().getMineHash();

            if (currentBestBlockHash == null
                    || !Arrays.equals(bestBlockHash, currentBestBlockHash)) {

                // Record new best block on the chain
                currentBestBlockHash = bestBlockHash;

                // Generate new block template
                AionPendingStateImpl.TransactionSortedSet ret =
                        new AionPendingStateImpl.TransactionSortedSet();
                ret.addAll(ac.getAionHub().getPendingState().getPendingTransactions());

                currentTemplate =
                        ac.getAionHub()
                                .getBlockchain()
                                .createNewBlockContext(bestBlock, new ArrayList<>(ret), false);
            }
        } finally {
            blockTemplateLock.unlock();
        }

        return currentTemplate;
    }

    public AionBlock getBlockByHash(byte[] hash) {
        return this.ac.getBlockchain().getBlockByHash(hash);
    }

    @Override
    public AionBlock getBlock(long blkNr) {
        if (blkNr == -1) {
            return this.ac.getBlockchain().getBestBlock();
        } else if (blkNr > 0) {
            return this.ac.getBlockchain().getBlockByNumber(blkNr);
        } else if (blkNr == 0) {
            AionGenesis genBlk = CfgAion.inst().getGenesis();
            return new AionBlock(genBlk.getHeader(), genBlk.getTransactionsList());
        } else {
            LOG.debug("ApiAion.getBlock - incorrect argument");
            return null;
        }
    }

    protected Map.Entry<AionBlock, BigInteger> getBlockWithTotalDifficulty(long blkNr) {
        if (blkNr > 0) {
            return ((AionBlockStore) this.ac.getBlockchain().getBlockStore())
                    .getChainBlockByNumberWithTotalDifficulty(blkNr);
        } else if (blkNr == 0) {
            AionGenesis genBlk = CfgAion.inst().getGenesis();
            return Map.entry(
                    new AionBlock(genBlk.getHeader(), genBlk.getTransactionsList()),
                    genBlk.getDifficultyBI());
        } else {
            LOG.debug("ApiAion.getBlock - incorrect argument");
            return null;
        }
    }

    protected SyncInfo getSync() {
        SyncInfo sync = new SyncInfo();
        sync.done = this.ac.isSyncComplete();
        sync.chainStartingBlkNumber = this.ac.getInitialStartingBlockNumber().orElse(0L);
        sync.networkBestBlkNumber = this.ac.getNetworkBestBlockNumber().orElse(0L);
        sync.chainBestBlkNumber = this.ac.getLocalBestBlockNumber().orElse(0L);
        return sync;
    }

    protected AionTransaction getTransactionByBlockHashAndIndex(byte[] hash, long index) {
        AionBlock pBlk = this.getBlockByHash(hash);
        if (pBlk == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error(
                        "ApiAion.getTransactionByBlockHashAndIndex - can't find the block by the block hash");
            }
            return null;
        }

        List<AionTransaction> txList = pBlk.getTransactionsList();
        AionTransaction tx = txList.get((int) index);
        if (tx == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Can't find the transaction!");
            }
            return null;
        }

        TxRecpt receipt = this.getTransactionReceipt(tx.getHash());
        // @Jay this should not happen!
        // TODO
        if (receipt == null) {
            throw new NullPointerException();
        }

        tx.setBlockNumber(pBlk.getNumber());
        tx.setBlockHash(pBlk.getHash());
        tx.setTxIndexInBlock(index);
        tx.setNrgConsume(receipt.nrgUsed);
        return tx;
    }

    protected AionTransaction getTransactionByBlockNumberAndIndex(long blkNr, long index) {
        AionBlock pBlk = this.getBlock(blkNr);
        if (pBlk == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error(
                        "ApiAion.getTransactionByBlockNumberAndIndex - can't find the block by the block number");
            }
            return null;
        }

        List<AionTransaction> txList = pBlk.getTransactionsList();
        AionTransaction tx = txList.get((int) index);
        if (tx == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Can't find the transaction by the txIndex");
            }
            return null;
        }

        TxRecpt receipt = this.getTransactionReceipt(tx.getHash());
        // The receipt shouldn't be null!
        if (receipt == null) {
            throw new NullPointerException();
        }

        tx.rlpParse();
        tx.setBlockNumber(pBlk.getNumber());
        tx.setBlockHash(pBlk.getHash());
        tx.setTxIndexInBlock(index);
        tx.setNrgConsume(receipt.nrgUsed);
        return tx;
    }

    protected long getBlockTransactionCountByNumber(long blkNr) {
        AionBlock pBlk = this.getBlock(blkNr);
        if (pBlk == null) {
            LOG.error(
                    "ApiAion.getTransactionByBlockNumberAndIndex - can't find the block by the block number");
            return -1;
        }

        return pBlk.getTransactionsList().size();
    }

    protected long getTransactionCountByHash(byte[] hash) {
        AionBlock pBlk = this.getBlockByHash(hash);
        if (pBlk == null) {
            LOG.error(
                    "ApiAion.getTransactionByBlockNumberAndIndex - can't find the block by the block number");
            return -1;
        }
        return pBlk.getTransactionsList().size();
    }

    protected long getTransactionCount(Address addr, long blkNr) {
        AionBlock pBlk = this.getBlock(blkNr);
        if (pBlk == null) {
            LOG.error(
                    "ApiAion.getTransactionByBlockNumberAndIndex - can't find the block by the block number");
            return -1;
        }
        long cnt = 0;
        List<AionTransaction> txList = pBlk.getTransactionsList();
        for (AionTransaction tx : txList) {
            if (addr.equals(tx.getFrom())) {
                cnt++;
            }
        }
        return cnt;
    }

    protected AionTransaction getTransactionByHash(byte[] hash) {
        TxRecpt txRecpt = this.getTransactionReceipt(hash);

        if (txRecpt == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Can't find the transaction receipt by the txhash.");
            }
            return null;
        } else {
            AionTransaction atx =
                    this.getTransactionByBlockNumberAndIndex(
                            txRecpt.blockNumber, txRecpt.transactionIndex);

            if (atx == null) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Can't find the transaction by the blocknumber and the txIndex.");
                }
                return null;
            }

            atx.setNrgConsume(txRecpt.nrgUsed);
            return atx;
        }
    }

    public byte[] getCode(Address addr) {
        return this.ac.getRepository().getCode(addr);
    }

    /* NOTE: only use this if you need receipts for one or small number transactions in a block.
     * (since there is n^2 work happening here to compute cumulative nrg)
     * For use cases where you need all the transaction receipts in a block, please use a different
     * strategy,
     */
    protected TxRecpt getTransactionReceipt(byte[] txHash) {
        if (txHash == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("<get-transaction-receipt msg=tx-hash-null>");
            }
            return null;
        }

        AionTxInfo txInfo = this.ac.getAionHub().getBlockchain().getTransactionInfo(txHash);
        if (txInfo == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("<get-transaction-receipt msg=tx-info-null>");
            }
            return null;
        }
        AionBlock block =
                this.ac.getAionHub().getBlockchain().getBlockByHash(txInfo.getBlockHash());

        if (block == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("<get-transaction-receipt msg=block-null>");
            }
            return null;
        }

        // need to return txes only from main chain
        AionBlock mainBlock =
                this.ac.getAionHub().getBlockchain().getBlockByNumber(block.getNumber());
        if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
            LOG.debug("<get-transaction-receipt msg=hash-not-match>");
            return null;
        }

        // @Jay
        // TODO : think the good way to calculate the cumulated nrg use
        long cumulateNrg = 0L;
        for (AionTransaction atx : block.getTransactionsList()) {

            // @Jay: This should not happen!
            byte[] hash = atx.getHash();
            if (hash == null) {
                throw new NullPointerException();
            }

            AionTxInfo info = this.ac.getAionHub().getBlockchain().getTransactionInfo(hash);

            // @Jay: This should not happen!
            if (info == null) {
                throw new NullPointerException();
            }

            cumulateNrg += info.getReceipt().getEnergyUsed();
            if (Arrays.equals(txHash, hash)) {
                break;
            }
        }

        return new TxRecpt(block, txInfo, cumulateNrg, true);
    }

    protected byte[] doCall(ArgTxCall _params) {
        AionTransaction tx =
                new AionTransaction(
                        _params.getNonce().toByteArray(),
                        _params.getTo(),
                        _params.getValue().toByteArray(),
                        _params.getData(),
                        _params.getNrg(),
                        _params.getNrgPrice());
        AionTxReceipt rec =
                this.ac.callConstant(tx, this.ac.getAionHub().getBlockchain().getBestBlock());
        return rec.getExecutionResult();
    }

    protected long estimateNrg(ArgTxCall params) {
        Address fromAddr =
                (params.getFrom().isEmptyAddress()) ? Address.ZERO_ADDRESS() : params.getFrom();

        long nrg =
                (params.getTo().isEmptyAddress())
                        ? Constants.NRG_TX_CREATE_MAX
                        : Constants.NRG_TRANSACTION_MAX;

        AionTransaction tx =
                new AionTransaction(
                        params.getNonce().toByteArray(),
                        fromAddr,
                        params.getTo(),
                        params.getValue().toByteArray(),
                        params.getData(),
                        nrg,
                        params.getNrgPrice());

        AionTxReceipt receipt =
                this.ac.callConstant(tx, this.ac.getAionHub().getBlockchain().getBestBlock());
        return receipt.getEnergyUsed();
    }

    protected ApiTxResponse createContract(ArgTxCall _params) {

        if (_params == null) {
            return (new ApiTxResponse(TxResponse.INVALID_TX));
        }

        Address from = _params.getFrom();

        if (from == null || from.isEmptyAddress()) {
            LOG.error("<create-contract msg=invalid-from-address>");
            return (new ApiTxResponse(TxResponse.INVALID_FROM));
        }

        ECKey key = this.getAccountKey(from.toString());

        if (key == null) {
            LOG.debug("ApiAion.createContract - null key");
            return (new ApiTxResponse(TxResponse.INVALID_ACCOUNT));
        }

        try {
            synchronized (pendingState) {
                byte[] nonce =
                        !(_params.getNonce().equals(BigInteger.ZERO))
                                ? _params.getNonce().toByteArray()
                                : pendingState
                                        .bestPendingStateNonce(Address.wrap(key.getAddress()))
                                        .toByteArray();

                AionTransaction tx =
                        new AionTransaction(
                                nonce,
                                from,
                                null,
                                _params.getValue().toByteArray(),
                                _params.getData(),
                                _params.getNrg(),
                                _params.getNrgPrice());
                tx.sign(key);

                TxResponse rsp = pendingState.addPendingTransaction(tx);

                return new ApiTxResponse(rsp, tx.getHash(), tx.getContractAddress());
            }
        } catch (Exception ex) {
            LOG.error("ApiAion.createContract - exception: [{}]", ex.getMessage());
            return new ApiTxResponse(TxResponse.EXCEPTION, ex);
        }
    }

    // Transaction Level
    public BigInteger getBalance(String _address) {
        return this.ac.getRepository().getBalance(Address.wrap(_address));
    }

    public BigInteger getBalance(Address _address) {
        return this.ac.getRepository().getBalance(_address);
    }

    public BigInteger getNonce(String _address) {
        return this.ac.getRepository().getNonce(Address.wrap(_address));
    }

    public BigInteger getNonce(Address _address) {
        return this.ac.getRepository().getNonce(_address);
    }

    protected ApiTxResponse sendTransaction(ArgTxCall _params) {

        if (_params == null) {
            return (new ApiTxResponse(TxResponse.INVALID_TX));
        }

        Address from = _params.getFrom();

        if (from == null || from.isEmptyAddress()) {
            LOG.error("<send-transaction msg=invalid-from-address>");
            return (new ApiTxResponse(TxResponse.INVALID_FROM));
        }

        ECKey key = this.getAccountKey(from.toString());
        if (key == null) {
            LOG.error("<send-transaction msg=account-not-found>");
            return (new ApiTxResponse(TxResponse.INVALID_ACCOUNT));
        }

        try {
            synchronized (pendingState) {
                // TODO : temp set nrg & price to 1
                byte[] nonce =
                        (!_params.getNonce().equals(BigInteger.ZERO))
                                ? _params.getNonce().toByteArray()
                                : pendingState
                                        .bestPendingStateNonce(Address.wrap(key.getAddress()))
                                        .toByteArray();

                AionTransaction tx =
                        new AionTransaction(
                                nonce,
                                _params.getTo(),
                                _params.getValue().toByteArray(),
                                _params.getData(),
                                _params.getNrg(),
                                _params.getNrgPrice());
                tx.sign(key);

                return (new ApiTxResponse(pendingState.addPendingTransaction(tx), tx.getHash()));
            }
        } catch (Exception ex) {
            LOG.error("ApiAion.sendTransaction exception: [{}]", ex.getMessage());
            return (new ApiTxResponse(TxResponse.EXCEPTION, ex));
        }
    }

    protected ApiTxResponse sendTransaction(byte[] signedTx) {
        if (signedTx == null) {
            return (new ApiTxResponse(TxResponse.INVALID_TX));
        }

        AionTransaction tx = new AionTransaction(signedTx);
        try {
            return (new ApiTxResponse(pendingState.addPendingTransaction(tx), tx.getHash()));
        } catch (Exception ex) {
            LOG.error("<send-transaction exception>", ex);
            return (new ApiTxResponse(TxResponse.EXCEPTION, ex));
        }
    }

    protected AionTransaction signTransaction(ArgTxCall _params, String _address) {
        Address address;
        if (_address == null || _address.isEmpty()) {
            LOG.error("<sign-transaction msg=invalid-signing-address>");
            return null;
        } else {
            address = Address.wrap(_address);
        }

        ECKey key = getAccountKey(address.toString());
        if (key == null) {
            LOG.error("<sign-transaction msg=account-not-unlocked>");
            return null;
        }

        try {
            synchronized (pendingState) {
                byte[] nonce =
                        (!_params.getNonce().equals(BigInteger.ZERO))
                                ? _params.getNonce().toByteArray()
                                : pendingState
                                        .bestPendingStateNonce(Address.wrap(key.getAddress()))
                                        .toByteArray();

                AionTransaction tx =
                        new AionTransaction(
                                nonce,
                                _params.getTo(),
                                _params.getValue().toByteArray(),
                                _params.getData(),
                                _params.getNrg(),
                                _params.getNrgPrice());
                tx.sign(key);

                return tx;
            }
        } catch (Exception ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to sign the transaction");
            }
            return null;
        }
    }

    // --Commented out by Inspection START (02/02/18 6:58 PM):
    // public String getNodeId() {
    // return CfgAion.inst().getId();
    // }
    // --Commented out by Inspection STOP (02/02/18 6:58 PM)

    protected String[] getBootNodes() {
        return CfgAion.inst().getNodes();
    }

    //    private synchronized BigInteger getTxNonce(ECKey key) {
    //        return pendingState.bestPendingStateNonce();
    //    }

    //    private synchronized BigInteger getTxNonce(ECKey key, boolean add) {
    //        return add ? nm.getNonceAndAdd(Address.wrap(key.getAddress())) :
    // nm.getNonce(Address.wrap(key.getAddress()));
    //    }

    public boolean isMining() {
        return this.ac.getBlockMiner().isMining();
    }

    protected int peerCount() {
        return this.ac.getAionHub().getP2pMgr().getActiveNodes().size();
    }

    // follows the ethereum standard for web3 compliance. DO NOT DEPEND ON IT.
    // Will be changed to Aion-defined spec later
    // https://github.com/ethereum/wiki/wiki/Client-Version-Strings
    private String computeClientVersion() {
        try {
            return Stream.of(
                            "Aion(J)",
                            "v" + Version.KERNEL_VERSION,
                            System.getProperty("os.name"),
                            "Java-" + System.getProperty("java.version"))
                    .collect(Collectors.joining("/"));
        } catch (Exception e) {
            LOG.debug("client version string generation failed", e);
        }

        return ("Aion(J)/v" + Version.KERNEL_VERSION);
    }

    // create a comma-separated string of supported p2p wire protocol versions
    // mainly to keep compatibility with eth_protocolVersion which returns a String
    protected String p2pProtocolVersion() {
        try {
            List<Short> p2pVersions = this.ac.getAionHub().getP2pMgr().versions();
            int i = 0;
            StringBuilder b = new StringBuilder();
            for (Short v : p2pVersions) {
                b.append(ByteUtil.byteArrayToInt(ByteUtil.shortToBytes(v)));
                i++;
                if (i < p2pVersions.size()) {
                    b.append(",");
                }
            }
            return b.toString();
        } catch (Exception e) {
            LOG.error("p2p protocol versions string generation failed");
            return null;
        }
    }

    protected String chainId() {
        return (this.ac.getAionHub().getP2pMgr().chainId() + "");
    }

    public String getHashrate() {
        double hashrate = 0;

        // add the the hashrate computed by the internal CPU miner
        if (isMining()) {
            hashrate += this.ac.getBlockMiner().getHashrate();
        }

        hashrate += reportedHashrate;

        return Double.toString(hashrate);
    }

    // hashrate in sol/s should just be a hexadecimal representation of a BigNumber
    // right now, assuming only one external miner is connected to the kernel
    // this needs to change in the future when this client needs to support multiple external miners
    protected boolean setReportedHashrate(String hashrate, String clientId) {
        try {
            reportedHashrate = Double.parseDouble(hashrate);
            return true;
        } catch (Exception e) {
            LOG.debug("api - setReportedHashrate(): bad string supplied", e);
        }

        return false;
    }

    // Returns a fully initialized NrgOracle object.
    protected void initNrgOracle(IAionChain _ac) {
        if (NRG_ORACLE != null) return;

        IAionBlockchain bc = (IAionBlockchain) _ac.getBlockchain();
        long nrgPriceDefault = CfgAion.inst().getApi().getNrg().getNrgPriceDefault();
        long nrgPriceMax = CfgAion.inst().getApi().getNrg().getNrgPriceMax();

        NrgOracle.Strategy oracleStrategy = NrgOracle.Strategy.SIMPLE;
        if (CfgAion.inst().getApi().getNrg().isOracleEnabled()) {
            oracleStrategy = NrgOracle.Strategy.BLK_PRICE;
        }

        NRG_ORACLE = new NrgOracle(bc, nrgPriceDefault, nrgPriceMax, oracleStrategy);
    }

    protected long getRecommendedNrgPrice() {
        if (NRG_ORACLE != null) {
            return NRG_ORACLE.getNrgPrice();
        } else {
            return CfgAion.inst().getApi().getNrg().getNrgPriceDefault();
        }
    }

    // leak the oracle instance. NrgOracle is threadsafe, so safe to do this, but bad design
    protected NrgOracle getNrgOracle() {
        return NRG_ORACLE;
    }

    protected long getDefaultNrgLimit() {
        return 90_000L;
    }

    protected void startES(String thName) {
        ees = new EventExecuteService(100_000, thName, Thread.MIN_PRIORITY, LOG);
        ees.setFilter(setEvtfilter());
        ees.start(new EpApi());
    }

    private Set<Integer> setEvtfilter() {
        Set<Integer> eventSN = new HashSet<>();
        int sn = IHandler.TYPE.TX0.getValue() << 8;
        eventSN.add(sn + EventTx.CALLBACK.PENDINGTXRECEIVED0.getValue());
        eventSN.add(sn + EventTx.CALLBACK.PENDINGTXUPDATE0.getValue());

        sn = IHandler.TYPE.BLOCK0.getValue() << 8;
        eventSN.add(sn + EventBlock.CALLBACK.ONBLOCK0.getValue());

        return eventSN;
    }

    protected void shutDownES() {
        ees.shutdown();
    }
}
