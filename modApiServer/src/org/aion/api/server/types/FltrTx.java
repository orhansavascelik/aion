package org.aion.api.server.types;

import org.aion.type.api.type.ITransaction;

public class FltrTx extends Fltr {

    public FltrTx() {
        super(Fltr.Type.TRANSACTION);
    }

    @Override
    public boolean onTransaction(ITransaction tx) {
        add(new EvtTx(tx));
        return true;
    }
}
