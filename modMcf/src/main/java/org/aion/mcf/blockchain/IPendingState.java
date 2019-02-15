package org.aion.mcf.blockchain;

import java.math.BigInteger;
import java.util.List;
import org.aion.type.api.interfaces.common.Address;
import org.aion.type.api.interfaces.db.RepositoryCache;
import org.aion.type.api.interfaces.tx.Transaction;
import org.aion.type.api.interfaces.tx.TransactionExtend;

public interface IPendingState<TX extends Transaction> {

    List<TxResponse> addPendingTransactions(List<TX> transactions);

    TxResponse addPendingTransaction(TX tx);

    boolean isValid(TX tx);

    RepositoryCache<?, ?> getRepository();

    List<TX> getPendingTransactions();

    BigInteger bestPendingStateNonce(Address addr);

    String getVersion();
}
