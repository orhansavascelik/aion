package org.aion.type.api.type;

import java.util.List;

/** @author jay */
public interface ITxReceipt<TX extends ITransaction, LOG> {
    void setTransaction(TX tx);

    void setLogs(List<LOG> logs);

    void setNrgUsed(long nrg);

    void setExecutionResult(byte[] result);

    void setError(String error);
}
