package org.aion.zero.impl.vm.resources;

import java.math.BigInteger;
import org.aion.base.type.AionAddress;
import org.aion.crypto.ECKey;
import org.aion.mcf.vm.Constants;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.types.AionTransaction;

/**
 * A builder class that makes constructing a signed value transfer transaction simple. A value
 * transfer transaction means a transaction that will not create any new dApps or invoke any
 * specified methods in an existing dApp, except perhaps default value methods that are invoked
 * regardless.
 *
 * If the same method is invoked multiple times with different arguments each time before the
 * transaction is built, then the final invocation takes precedence and defines that field.
 *
 * The builder class can be reused after the transaction has been built. It will retain all of its
 * previously set fields.
 *
 * Alternatively, the {@code resetBuilder()} method can be used to reset all fields as if the
 * builder had been newly created.
 */
public class ValueTransferTransactionBuilder {
    private static final String ERROR_MESSAGE = "Cannot build avm create transaction with ";
    private ECKey senderKey = null;
    private Address beneficiary = null;
    private BigInteger nonce = null;
    private BigInteger value = null;
    private long energyLimit = Constants.NRG_TRANSACTION_MAX;
    private long energyPrice = 1;

    /**
     * Sets the sender to the account derivable from the specified key and signs the transaction
     * using this key.
     *
     * <b>This field must be set.</b>
     *
     * @param senderKey The key of the sender.
     * @return this builder.
     */
    public ValueTransferTransactionBuilder senderKey(ECKey senderKey) {
        this.senderKey = senderKey;
        return this;
    }

    /**
     * Sets the nonce of the sender to the specified nonce.
     *
     * <b>This field must be set.</b>
     *
     * @param nonce The nonce of the sender.
     * @return this builder.
     */
    public ValueTransferTransactionBuilder senderNonce(BigInteger nonce) {
        this.nonce = nonce;
        return this;
    }

    /**
     * Sets the address of the account that will receive the funds being transferred.
     *
     * <b>This field must be set.</b>
     *
     * @param beneficiary The beneficiary of the transfer.
     * @return this builder.
     */
    public ValueTransferTransactionBuilder beneficiary(Address beneficiary) {
        this.beneficiary = beneficiary;
        return this;
    }

    /**
     * Sets the amount of value to transfer from the sender to the beneficiary.
     *
     * <b>This field must be set.</b>
     *
     * @param value The amount of funds to transfer.
     * @return this builder.
     */
    public ValueTransferTransactionBuilder valueToTransfer(BigInteger value) {
        this.value = value;
        return this;
    }

    /**
     * Sets the energy limit of the transaction to the specified limit.
     *
     * @param limit The transaction energy limit.
     * @return this builder.
     */
    public ValueTransferTransactionBuilder energyLimit(long limit) {
        this.energyLimit = limit;
        return this;
    }

    /**
     * Sets the energy price of the transaction to the specified price.
     *
     * @param price The transaction energy price.
     * @return this builder.
     */
    public ValueTransferTransactionBuilder energyPrice(long price) {
        this.energyPrice = price;
        return this;
    }

    /**
     * Constructs a new transaction for only transferring value.
     *
     * The following fields must have been set before calling this method or it will fail:
     *   - sender key
     *   - sender nonce
     *   - beneficiary
     *   - value
     *
     * The remaining optional fields have the following default values if they have not been set:
     *   - energy limit is {@link Constants#NRG_TRANSACTION_MAX}
     *   - energy price is {@code 1}
     *
     * @return a new transaction for only transferring value.
     */
    public AionTransaction buildValueTransferTransaction() {
        if (this.senderKey == null) {
            throw new IllegalStateException(ERROR_MESSAGE + "no sender.");
        }
        if (this.nonce == null) {
            throw new IllegalStateException(ERROR_MESSAGE + "no sender nonce.");
        }
        if (this.beneficiary == null) {
            throw new IllegalStateException(ERROR_MESSAGE + "no beneficiary.");
        }
        if (this.value == null) {
            throw new IllegalStateException(ERROR_MESSAGE + "no transfer value specified.");
        }

        AionTransaction transaction = new AionTransaction(
            this.nonce.toByteArray(),
            AionAddress.wrap(this.senderKey.getAddress()),
            this.beneficiary,
            this.value.toByteArray(),
            new byte[0],
            this.energyLimit,
            this.energyPrice);
        transaction.sign(this.senderKey);
        return transaction;
    }

    /**
     * Resets this builder so that all of the fields have the same initialization state as when
     * this class is newly constructed. There should be no difference between a newly constructed
     * instance of this class and an instance of this class after this method has been called.
     */
    public void resetBuilder() {
        this.senderKey = null;
        this.nonce = null;
        this.beneficiary = null;
        this.value = null;
        this.energyLimit = Constants.NRG_TRANSACTION_MAX;
        this.energyPrice = 1;
    }

}
