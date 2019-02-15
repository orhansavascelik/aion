package org.aion.api.server.types;

import org.aion.mcf.evt.IListenerBase;
import org.aion.type.api.interfaces.common.Wrapper;

public class TxPendingStatus {

    private Wrapper txhash;
    private Wrapper socketId;
    private Wrapper msgHash;
    private Wrapper txResult;
    private String error;
    private static final int txRetCodeOffset = 102;

    /*  */
    /** @see IListenerBase DROPPED(0) NEW_PENDING(1) PENDING(2) INCLUDED(3) */
    int state;

    public TxPendingStatus(
            Wrapper txHash, Wrapper id, Wrapper msgHash, int v, Wrapper txRes, String error) {
        // TODO Auto-generated constructor stub
        this.txhash = txHash;
        this.socketId = id;
        this.msgHash = msgHash;
        this.state = v;
        this.txResult = txRes;
        this.error = error;
    }

    public byte[] getSocketId() {
        return this.socketId.getData();
    }

    public byte[] getMsgHash() {
        return this.msgHash.getData();
    }

    public int getPendStatus() {
        return this.state;
    }

    public byte[] getTxHash() {
        return this.txhash.getData();
    }

    public byte[] getTxResult() {
        return this.txResult.getData();
    }

    public String getError() {
        return this.error;
    }

    public int toTxReturnCode() {
        return this.state + txRetCodeOffset;
    }

    public boolean isEmpty() {
        return txhash == null && socketId == null;
    }
}
