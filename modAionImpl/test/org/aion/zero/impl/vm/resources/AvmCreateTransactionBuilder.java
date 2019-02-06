package org.aion.zero.impl.vm.resources;

import java.math.BigInteger;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.base.type.AionAddress;
import org.aion.base.vm.VirtualMachineSpecs;
import org.aion.crypto.ECKey;
import org.aion.mcf.vm.Constants;
import org.aion.zero.types.AionTransaction;

/**
 * A builder class that makes constructing a signed avm transaction that will deploy a new dApp
 * simple.
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
public final class AvmCreateTransactionBuilder {
    private static final String ERROR_MESSAGE = "Cannot build avm create transaction with ";
    private ECKey senderKey = null;
    private BigInteger nonce = null;
    private Class<?> mainClass = null;
    private BigInteger value = BigInteger.ZERO;
    private Class<?>[] dependencies = new Class<?>[0];
    private byte[] clinitArgs = new byte[0];
    private long energyLimit = Constants.NRG_CREATE_CONTRACT_MAX;
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
    public AvmCreateTransactionBuilder senderKey(ECKey senderKey) {
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
    public AvmCreateTransactionBuilder senderNonce(BigInteger nonce) {
        this.nonce = nonce;
        return this;
    }

    /**
     * Sets the class that has the main entry point method in the dapp to the specified class.
     *
     * <b>This field must be set.</b>
     *
     * @param main The main class in the dapp.
     * @return this builder.
     */
    public AvmCreateTransactionBuilder dappMainClass(Class<?> main) {
        this.mainClass = main;
        return this;
    }

    /**
     * Sets the amount of value to transfer to the dapp to the specified value.
     *
     * @param value The value to transfer.
     * @return this builder.
     */
    public AvmCreateTransactionBuilder valueToTransferToContract(BigInteger value) {
        this.value = value;
        return this;
    }

    /**
     * Sets any classes that the main class depends on to the specified classes.
     *
     * @param dependencies The class dependencies.
     * @return this builder.
     */
    public AvmCreateTransactionBuilder dappDependencyClasses(Class<?>... dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    /**
     * Sets the dapp's clinit arguments to the specified arguments.
     *
     * The clinit arguments are the special arguments that get passed into the dapp during its
     * original construction phase, this is where all static fields and blocks get initialized.
     *
     * These arguments can be captured using the blockchain runtime's getData() method.
     *
     * The avm strips the clinit after the first launch of the dapp, so these arguments can only
     * be provided once.
     *
     * @param arguments The clinit arguments.
     * @return this builder.
     */
    public AvmCreateTransactionBuilder dappClinitArguments(byte[] arguments) {
        this.clinitArgs = arguments;
        return this;
    }

    /**
     * Sets the energy limit of the transaction to the specified limit.
     *
     * @param limit The transaction energy limit.
     * @return this builder.
     */
    public AvmCreateTransactionBuilder energyLimit(long limit) {
        this.energyLimit = limit;
        return this;
    }

    /**
     * Sets the energy price of the transaction to the specified price.
     *
     * @param price The transaction energy price.
     * @return this builder.
     */
    public AvmCreateTransactionBuilder energyPrice(long price) {
        this.energyPrice = price;
        return this;
    }

    /**
     * Constructs a new transaction for creating a new avm dApp.
     *
     * The following fields must have been set before calling this method or it will fail:
     *   - sender key
     *   - sender nonce
     *   - dApp main class
     *
     * The remaining optional fields have the following default values if they have not been set:
     *   - value is {@link BigInteger#ZERO}
     *   - dependencies is an empty array
     *   - clinit arguments is an empty array
     *   - energy limit is {@link Constants#NRG_CREATE_CONTRACT_MAX}
     *   - energy price is {@code 1}
     *
     * @return a new transaction for creating a new avm dApp.
     */
    public AionTransaction buildAvmCreateTransaction() {
        if (this.senderKey == null) {
            throw new IllegalStateException(ERROR_MESSAGE + "no sender.");
        }
        if (this.nonce == null) {
            throw new IllegalStateException(ERROR_MESSAGE + "no sender nonce.");
        }
        if (this.mainClass == null) {
            throw new IllegalStateException(ERROR_MESSAGE + "no main dapp class.");
        }

        AionTransaction transaction = new AionTransaction(
            this.nonce.toByteArray(),
            AionAddress.wrap(this.senderKey.getAddress()),
            null,
            this.value.toByteArray(),
            getJarFileBytes(),
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
        this.mainClass = null;
        this.value = BigInteger.ZERO;
        this.dependencies = new Class<?>[0];
        this.clinitArgs = new byte[0];
        this.energyLimit = Constants.NRG_CREATE_CONTRACT_MAX;
        this.energyPrice = 1;
    }

    private byte[] getJarFileBytes() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(this.mainClass, this.dependencies);
        return new CodeAndArguments(jar, this.clinitArgs).encodeToBytes();
    }

}
