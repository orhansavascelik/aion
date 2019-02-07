package org.aion.zero.impl.vm.resources;

import java.math.BigInteger;
import org.aion.avm.api.ABIEncoder;
import org.aion.base.type.AionAddress;
import org.aion.base.vm.VirtualMachineSpecs;
import org.aion.crypto.ECKey;
import org.aion.mcf.vm.Constants;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.types.AionTransaction;

/**
 * A builder class that makes constructing a signed avm transaction that will call into an existing
 * dApp simple.
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
public class AvmCallTransactionBuilder {
    private static final String ERROR_MESSAGE = "Cannot build avm create transaction with ";
    private ECKey senderKey = null;
    private BigInteger nonce = null;
    private String method = null;
    private Address contract = null;
    private Object[] parameters = new Object[0];
    private BigInteger value = BigInteger.ZERO;
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
    public AvmCallTransactionBuilder senderKey(ECKey senderKey) {
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
    public AvmCallTransactionBuilder senderNonce(BigInteger nonce) {
        this.nonce = nonce;
        return this;
    }

    /**
     * Sets the name of the method in the existing dApp to be invoked by this call.
     *
     * <b>This field must be set.</b>
     *
     * @param methodName The method to invoke.
     * @return this builder.
     */
    public AvmCallTransactionBuilder methodToInvoke(String methodName) {
        this.method = methodName;
        return this;
    }

    /**
     * Sets the address of the dApp to call into.
     *
     * @param contract The dApp address.
     * @return this builder.
     */
    public AvmCallTransactionBuilder contractToCall(Address contract) {
        this.contract = contract;
        return this;
    }

    /**
     * Sets the parameters that will be used to invoke the specified dApp method with.
     *
     * @param parameters The invocation parameters.
     * @return this builder.
     */
    public AvmCallTransactionBuilder parametersToInvokeMethodWith(Object... parameters) {
        this.parameters = parameters;
        return this;
    }

    /**
     * Sets the amount of value to transfer to the dapp to the specified value.
     *
     * @param value The value to transfer.
     * @return this builder.
     */
    public AvmCallTransactionBuilder valueToTransferToContract(BigInteger value) {
        this.value = value;
        return this;
    }

    /**
     * Sets the energy limit of the transaction to the specified limit.
     *
     * @param limit The transaction energy limit.
     * @return this builder.
     */
    public AvmCallTransactionBuilder energyLimit(long limit) {
        this.energyLimit = limit;
        return this;
    }

    /**
     * Sets the energy price of the transaction to the specified price.
     *
     * @param price The transaction energy price.
     * @return this builder.
     */
    public AvmCallTransactionBuilder energyPrice(long price) {
        this.energyPrice = price;
        return this;
    }

    /**
     * Constructs a new transaction for calling into an existing avm dApp.
     *
     * The following fields must have been set before calling this method or it will fail:
     *   - sender key
     *   - sender nonce
     *   - dApp method to invoke
     *
     * The remaining optional fields have the following default values if they have not been set:
     *   - value is {@link BigInteger#ZERO}
     *   - dApp method parameters is an empty array
     *   - energy limit is {@link Constants#NRG_TRANSACTION_MAX}
     *   - energy price is {@code 1}
     *
     * @return a new transaction for calling into an existing avm dApp.
     */
    public AionTransaction buildAvmCallTransaction() {
        if (this.senderKey == null) {
            throw new IllegalStateException(ERROR_MESSAGE + "no sender.");
        }
        if (this.nonce == null) {
            throw new IllegalStateException(ERROR_MESSAGE + "no sender nonce.");
        }
        if (this.contract == null) {
            throw new IllegalStateException(ERROR_MESSAGE + "no dapp address to call into.");
        }
        if (this.method == null) {
            throw new IllegalStateException(ERROR_MESSAGE + "no dapp method to call.");
        }

        AionTransaction transaction = new AionTransaction(
            this.nonce.toByteArray(),
            AionAddress.wrap(this.senderKey.getAddress()),
            this.contract,
            this.value.toByteArray(),
            getEncodingOfMethodCall(),
            this.energyLimit,
            this.energyPrice,
            VirtualMachineSpecs.AVM_CREATE_CODE);
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
        this.method = null;
        this.contract = null;
        this.value = BigInteger.ZERO;
        this.parameters = new Object[0];
        this.energyLimit = Constants.NRG_TRANSACTION_MAX;
        this.energyPrice = 1;
    }

    private byte[] getEncodingOfMethodCall() {
        return ABIEncoder.encodeMethodArguments(this.method, this.parameters);
    }

}
