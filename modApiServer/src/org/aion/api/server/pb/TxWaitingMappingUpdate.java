package org.aion.api.server.pb;

import org.aion.type.ByteArrayWrapper;
import org.aion.type.api.interfaces.common.Wrapper;
import org.aion.zero.types.AionTxReceipt;

public class TxWaitingMappingUpdate {
    Wrapper txHash;
    AionTxReceipt txReceipt;
    int pState;

    public TxWaitingMappingUpdate(Wrapper txHashW, int state, AionTxReceipt txReceipt) {
        this.txHash = txHashW;
        this.pState = state;
        this.txReceipt = txReceipt;
    }

    public Wrapper getTxHash() {
        return txHash;
    }

    public AionTxReceipt getTxReceipt() {
        return txReceipt;
    }

    public Wrapper getTxResult() {
        return ByteArrayWrapper.wrap(txReceipt.getTransactionOutput());
    }

    public int getState() {
        return pState;
    }

    public boolean isDummy() {
        return txHash == null && pState == 0 && txReceipt == null;
    }
}
