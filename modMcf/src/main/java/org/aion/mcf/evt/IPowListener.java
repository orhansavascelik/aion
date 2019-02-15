package org.aion.mcf.evt;

import java.util.List;
import org.aion.type.api.interfaces.block.Block;
import org.aion.type.api.interfaces.tx.TransactionExtend;
import org.aion.type.api.interfaces.tx.TxExecSummary;
import org.aion.mcf.type.AbstractBlockSummary;
import org.aion.mcf.type.AbstractTxReceipt;

/**
 * POW listener interface.
 *
 * @param <BLK>
 * @param <TX>
 * @param <TXR>
 * @param <BS>
 */
public interface IPowListener<
                BLK extends Block<?, ?>,
                TX extends TransactionExtend,
                TXR extends AbstractTxReceipt<?>,
                BS extends AbstractBlockSummary<?, ?, ?, ?>>
        extends IListenerBase<BLK, TX, TXR, BS> {
    void onBlock(BS blockSummary);

    void onPeerDisconnect(String host, long port);

    void onPendingTransactionsReceived(List<TX> transactions);

    void onSyncDone();

    void onNoConnections();

    void onVMTraceCreated(String transactionHash, String trace);

    void onTransactionExecuted(TxExecSummary summary);
}
