package org.aion.zero.impl.core;

import java.util.List;
import org.aion.interfaces.db.Repository;
import org.aion.mcf.core.IBlockchain;
import org.aion.interfaces.tx.Transaction;
import org.aion.zero.impl.BlockContext;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.impl.types.AionTxInfo;
import org.aion.zero.types.A0BlockHeader;
import org.aion.zero.types.AionTxReceipt;

/** aion blockchain interface. */
public interface IAionBlockchain
        extends IBlockchain<AionBlock, A0BlockHeader, Transaction, AionTxReceipt, AionTxInfo> {

    AionBlock createNewBlock(
            AionBlock parent, List<Transaction> transactions, boolean waitUntilBlockTime);

    BlockContext createNewBlockContext(
            AionBlock parent, List<Transaction> transactions, boolean waitUntilBlockTime);

    AionBlock getBestBlock();

    AionBlock getBlockByNumber(long num);

    /**
     * Recovery functionality for rebuilding the world state.
     *
     * @return {@code true} if the recovery was successful, {@code false} otherwise
     */
    boolean recoverWorldState(Repository repository, AionBlock block);

    /**
     * Recovery functionality for recreating the block info in the index database.
     *
     * @return {@code true} if the recovery was successful, {@code false} otherwise
     */
    boolean recoverIndexEntry(Repository repository, AionBlock block);

    /**
     * Heuristic for skipping the call to tryToConnect with very large or very small block number.
     */
    boolean skipTryToConnect(long blockNumber);
}
