package org.aion.zero.impl.vm.resources;

import java.math.BigInteger;
import org.aion.base.type.AionAddress;
import org.aion.crypto.ECKey;
import org.aion.mcf.vm.Constants;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.types.AionTransaction;

public class TransferValueTransactionBuilder {
    private static String errorMessage = "Cannot build a value transfer transaction without ";

    private ECKey senderKey = null;
    private Address beneficiary = null;
    private BigInteger nonce = null;
    private BigInteger value = null;
    private long energyLimit = -1;
    private long energyPrice = -1;

    public TransferValueTransactionBuilder senderKey(ECKey senderKey) {
        this.senderKey = senderKey;
        return this;
    }

    public TransferValueTransactionBuilder beneficiary(Address beneficiary) {
        this.beneficiary = beneficiary;
        return this;
    }

    public TransferValueTransactionBuilder nonce(BigInteger nonce) {
        this.nonce = nonce;
        return this;
    }

    public TransferValueTransactionBuilder value(BigInteger value) {
        this.value = value;
        return this;
    }

    public TransferValueTransactionBuilder energyLimit(long limit) {
        this.energyLimit = limit;
        return this;
    }

    public TransferValueTransactionBuilder energyPrice(long price) {
        this.energyPrice = price;
        return this;
    }

    /**
     * Builds a transaction that will transfer value to a beneficiary. No code will be called if the
     * beneficiary is a contract.
     *
     * The following fields must have been set by the builder:
     *   - {@code senderKey}
     *   - {@code beneficiary}
     *   - {@code nonce}
     *   - {@code value}
     *
     * The following non-required fields have these default values if not specified:
     *   - {@code energyLimit} default is {@value Constants#NRG_TRANSACTION_MAX}.
     *   - {@code energyPrice} default is 1.
     *
     * @return A new value transfer transaction.
     */
    public AionTransaction buildValueTransfer() {
        if (this.senderKey == null) {
            throw new IllegalStateException(errorMessage + "a sender.");
        }
        if (this.beneficiary == null) {
            throw new IllegalStateException(errorMessage + "a beneficiary.");
        }
        if (this.nonce == null) {
            throw new IllegalStateException(errorMessage + "the sender's nonce.");
        }
        if (this.value == null) {
            throw new IllegalStateException(errorMessage + "an amount of value to transfer.");
        }

        this.energyLimit = (this.energyLimit == -1) ? Constants.NRG_TRANSACTION_MAX : this.energyLimit;
        this.energyPrice = (this.energyPrice == -1) ? 1 : this.energyPrice;

        AionTransaction transaction = new AionTransaction(
            this.nonce.toByteArray(),
            AionAddress.wrap(this.senderKey.getAddress()),
            this.beneficiary,
            this.value.toByteArray(),
            new byte[0],
            this.energyLimit,
            this.energyPrice,
            (byte) 0x1);
        transaction.sign(this.senderKey);
        return transaction;
    }

}
