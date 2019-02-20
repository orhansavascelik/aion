package org.aion.mcf.blockchain;

import java.math.BigInteger;
import java.util.List;
import org.aion.types.Address;
import org.aion.interfaces.db.RepositoryCache;
import org.aion.interfaces.tx.Transaction;

public interface IPendingState<TX extends Transaction> {

    List<TxResponse> addPendingTransactions(List<TX> transactions);

    TxResponse addPendingTransaction(TX tx);

    boolean isValid(TX tx);

    RepositoryCache<?, ?> getRepository();

    List<TX> getPendingTransactions();

    BigInteger bestPendingStateNonce(Address addr);

    String getVersion();
}
