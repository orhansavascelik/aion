package org.aion.zero.impl.blockchain;

import java.math.BigInteger;
import java.util.List;
import org.aion.interfaces.db.Repository;
import org.aion.mcf.blockchain.IChainInstancePOW;
import org.aion.mcf.blockchain.IPowChain;
import org.aion.types.Address;
import org.aion.interfaces.tx.Transaction;
import org.aion.zero.impl.AionHub;
import org.aion.zero.impl.query.QueryInterface;
import org.aion.zero.types.A0BlockHeader;
import org.aion.zero.types.AionTxReceipt;
import org.aion.zero.types.AionBlock;

/** Aion chain interface. */
public interface IAionChain extends IChainInstancePOW, QueryInterface {

    IPowChain<org.aion.zero.impl.types.AionBlock, A0BlockHeader> getBlockchain();

    void close();

    Transaction createTransaction(BigInteger nonce, Address to, BigInteger value, byte[] data);

    void broadcastTransaction(Transaction transaction);

    AionTxReceipt callConstant(Transaction tx, AionBlock block);

    Repository<?, ?> getRepository();

    Repository<?, ?> getPendingState();

    Repository<?, ?> getSnapshotTo(byte[] root);

    List<Transaction> getWireTransactions();

    List<Transaction> getPendingStateTransactions();

    AionHub getAionHub();

    void exitOn(long number);

    long estimateTxNrg(Transaction tx, AionBlock block);
}
