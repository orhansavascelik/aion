package org.aion.type.api.interfaces.tx;

import org.aion.type.api.interfaces.common.Address;

public interface Transaction {
    byte[] getTransactionHash();

    Address getSenderAddress();

    Address getDestinationAddress();

    byte[] getNonce();

    byte[] getValue();

    byte[] getData();

    byte getTargetVM();

    long getEnergyLimit();

    long getEnergyPrice();

    long getTransactionCost();

    byte[] getTimestamp();

    boolean isContractCreationTransaction();
}
