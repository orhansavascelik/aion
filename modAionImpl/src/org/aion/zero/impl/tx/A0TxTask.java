package org.aion.zero.impl.tx;

import java.util.List;
import org.aion.mcf.tx.AbstractTxTask;
import org.aion.p2p.IP2pMgr;
import org.aion.p2p.Msg;
import org.aion.interfaces.tx.Transaction;

public class A0TxTask extends AbstractTxTask<Transaction, IP2pMgr> {

    public A0TxTask(Transaction _tx, IP2pMgr _p2pMgr, Msg _msg) {
        super(_tx, _p2pMgr, _msg);
    }

    public A0TxTask(List<Transaction> _tx, IP2pMgr _p2pMgr, Msg _msg) {
        super(_tx, _p2pMgr, _msg);
    }
}
