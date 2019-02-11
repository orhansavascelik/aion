package org.aion.mcf.blockchain;

import java.math.BigInteger;
import java.util.List;
import org.aion.type.api.db.IRepositoryCache;
import org.aion.type.api.type.ITransaction;
import org.aion.vm.api.interfaces.Address;

public interface IPendingState<TX extends ITransaction> {

    List<TxResponse> addPendingTransactions(List<TX> transactions);

    TxResponse addPendingTransaction(TX tx);

    boolean isValid(TX tx);

    IRepositoryCache<?, ?> getRepository();

    List<TX> getPendingTransactions();

    BigInteger bestPendingStateNonce(Address addr);

    String getVersion();
}
