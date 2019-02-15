package org.aion.api.server.types;

import org.aion.api.server.types.Fltr.Type;
import org.aion.type.api.interfaces.tx.TransactionExtend;
import org.aion.util.string.StringUtils;

public class EvtTx extends Evt {

    private final TransactionExtend tx;

    public EvtTx(TransactionExtend tx) {
        this.tx = tx;
    }

    @Override
    public Type getType() {
        return Type.TRANSACTION;
    }

    @Override
    public String toJSON() {
        return StringUtils.toJsonHex(tx.getTransactionHash());
    }
}
