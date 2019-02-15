package org.aion.api.server.types;

import org.aion.type.api.interfaces.tx.TransactionExtend;

public class FltrTx extends Fltr {

    public FltrTx() {
        super(Fltr.Type.TRANSACTION);
    }

    @Override
    public boolean onTransaction(TransactionExtend tx) {
        add(new EvtTx(tx));
        return true;
    }
}
