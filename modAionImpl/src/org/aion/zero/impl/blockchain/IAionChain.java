package org.aion.zero.impl.blockchain;

import java.math.BigInteger;
import java.util.List;
import org.aion.type.api.interfaces.db.Repository;
import org.aion.mcf.blockchain.IChainInstancePOW;
import org.aion.mcf.blockchain.IPowChain;
import org.aion.type.api.interfaces.common.Address;
import org.aion.type.api.interfaces.tx.TransactionExtend;
import org.aion.zero.impl.AionHub;
import org.aion.zero.impl.query.QueryInterface;
import org.aion.zero.types.A0BlockHeader;
import org.aion.zero.types.AionTxReceipt;
import org.aion.zero.types.AionBlock;

/** Aion chain interface. */
public interface IAionChain extends IChainInstancePOW, QueryInterface {

    IPowChain<org.aion.zero.impl.types.AionBlock, A0BlockHeader> getBlockchain();

    void close();

    TransactionExtend createTransaction(BigInteger nonce, Address to, BigInteger value, byte[] data);

    void broadcastTransaction(TransactionExtend transaction);

    AionTxReceipt callConstant(TransactionExtend tx, AionBlock block);

    Repository<?, ?> getRepository();

    Repository<?, ?> getPendingState();

    Repository<?, ?> getSnapshotTo(byte[] root);

    List<TransactionExtend> getWireTransactions();

    List<TransactionExtend> getPendingStateTransactions();

    AionHub getAionHub();

    void exitOn(long number);

    long estimateTxNrg(TransactionExtend tx, AionBlock block);
}
