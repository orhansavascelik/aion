package org.aion.zero.impl.vm.resources;

import java.math.BigInteger;
import org.aion.avm.api.ABIEncoder;
import org.aion.base.type.AionAddress;
import org.aion.base.vm.VirtualMachineSpecs;
import org.aion.crypto.ECKey;
import org.aion.mcf.vm.Constants;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.types.AionTransaction;

public class AvmCallTransactionBuilder {
    private static String errorMessage = "Cannot build an avm call transaction without ";

    private ECKey senderKey = null;
    private Address contract = null;
    private BigInteger nonce = null;
    private BigInteger value = null;
    private String method = null;
    private Object[] parameters = null;
    private long energyLimit = -1;
    private long energyPrice = -1;

    public AvmCallTransactionBuilder senderKey(ECKey senderKey) {
        this.senderKey = senderKey;
        return this;
    }

    public AvmCallTransactionBuilder contract(Address contract) {
        this.contract = contract;
        return this;
    }

    public AvmCallTransactionBuilder nonce(BigInteger nonce) {
        this.nonce = nonce;
        return this;
    }

    public AvmCallTransactionBuilder value(BigInteger value) {
        this.value = value;
        return this;
    }

    public AvmCallTransactionBuilder method(String methodName) {
        this.method = methodName;
        return this;
    }

    public AvmCallTransactionBuilder methodParameters(Object... parameters) {
        this.parameters = parameters;
        return this;
    }

    public AvmCallTransactionBuilder energyLimit(long limit) {
        this.energyLimit = limit;
        return this;
    }

    public AvmCallTransactionBuilder energyPrice(long price) {
        this.energyPrice = price;
        return this;
    }

    /**
     * Builds a transaction that will call an existing Avm dApp.
     *
     * The following fields must have been set by the builder:
     *   - {@code senderKey}
     *   - {@code contract}
     *   - {@code nonce}
     *   - {@code method}
     *
     * The following non-required fields have these default values if not specified:
     *   - {@code value} default is {@link BigInteger#ZERO}.
     *   - {@code parameters} default is an empty array.
     *   - {@code energyLimit} default is {@value Constants#NRG_TRANSACTION_MAX}.
     *   - {@code energyPrice} default is 1.
     *
     * @return A new avm call transaction.
     */
    public AionTransaction buildAvmCall() {
        if (this.senderKey == null) {
            throw new IllegalStateException(errorMessage + "a sender.");
        }
        if (this.contract == null) {
            throw new IllegalStateException(errorMessage + "a contract to call.");
        }
        if (this.nonce == null) {
            throw new IllegalStateException(errorMessage + "the sender's nonce.");
        }
        if (this.method == null) {
            throw new IllegalStateException(errorMessage + "a method to call into.");
        }

        this.value = (this.value == null) ? BigInteger.ZERO : this.value;
        this.parameters = (this.parameters == null) ? new Object[0] : this.parameters;
        this.energyLimit = (this.energyLimit == -1) ? Constants.NRG_TRANSACTION_MAX : this.energyLimit;
        this.energyPrice = (this.energyPrice == -1) ? 1 : this.energyPrice;

        AionTransaction transaction = new AionTransaction(
            this.nonce.toByteArray(),
            AionAddress.wrap(this.senderKey.getAddress()),
            this.contract,
            this.value.toByteArray(),
            encodeMethodCall(),
            this.energyLimit,
            this.energyPrice,
            VirtualMachineSpecs.AVM_CREATE_CODE);
        transaction.sign(this.senderKey);
        return transaction;
    }

    private byte[] encodeMethodCall() {
        return ABIEncoder.encodeMethodArguments(this.method, this.parameters);
    }

}
