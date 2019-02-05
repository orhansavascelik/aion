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
 * A convenience class for constructing transactions that will deploy a new Avm dApp.
 */
public class AvmCreateTransactionBuilder {
    private static String errorMessage = "Cannot build an avm create transaction without ";

    private ECKey senderKey = null;
    private BigInteger nonce = null;
    private BigInteger value = null;
    private Class<?> mainClass = null;
    private Class<?>[] otherClasses = null;
    private byte[] clinitArgs = null;
    private long energyLimit = -1;
    private long energyPrice = -1;

    public AvmCreateTransactionBuilder senderKey(ECKey senderKey) {
        this.senderKey = senderKey;
        return this;
    }

    public AvmCreateTransactionBuilder nonce(BigInteger nonce) {
        this.nonce = nonce;
        return this;
    }

    public AvmCreateTransactionBuilder value(BigInteger value) {
        this.value = value;
        return this;
    }

    public AvmCreateTransactionBuilder mainClass(Class<?> main) {
        this.mainClass = main;
        return this;
    }

    public AvmCreateTransactionBuilder otherClasses(Class<?>... otherClasses) {
        this.otherClasses = otherClasses;
        return this;
    }

    public AvmCreateTransactionBuilder clinitArguments(byte[] arguments) {
        this.clinitArgs = arguments;
        return this;
    }

    public AvmCreateTransactionBuilder energyLimit(long limit) {
        this.energyLimit = limit;
        return this;
    }

    public AvmCreateTransactionBuilder energyPrice(long price) {
        this.energyPrice = price;
        return this;
    }

    /**
     * Builds a transaction that will create a new Avm dApp.
     *
     * The following fields must have been set by the builder:
     *   - {@code senderKey}
     *   - {@code nonce}
     *   - {@code mainClass}
     *
     * The following non-required fields have these default values if not specified:
     *   - {@code value} default is {@link BigInteger#ZERO}.
     *   - {@code otherClasses} default is an empty array.
     *   - {@code clinitArguments} default is an empty array.
     *   - {@code energyLimit} default is {@value Constants#NRG_CREATE_CONTRACT_MAX}.
     *   - {@code energyPrice} default is 1.
     *
     * @return A new avm create transaction.
     */
    public AionTransaction buildAvmCreate() {
        if (this.senderKey == null) {
            throw new IllegalStateException(errorMessage + "a sender.");
        }
        if (this.nonce == null) {
            throw new IllegalStateException(errorMessage + "the sender's nonce.");
        }
        if (this.mainClass == null) {
            throw new IllegalStateException(errorMessage + "a main class for the dApp.");
        }

        this.value = (this.value == null) ? BigInteger.ZERO : this.value;
        this.otherClasses = (this.otherClasses == null) ? new Class<?>[0] : this.otherClasses;
        this.clinitArgs = (this.clinitArgs == null) ? new byte[0] : this.clinitArgs;
        this.energyLimit = (this.energyLimit == -1) ? Constants.NRG_CREATE_CONTRACT_MAX : this.energyLimit;
        this.energyPrice = (this.energyPrice == -1) ? 1 : this.energyPrice;

        AionTransaction transaction = new AionTransaction(
            this.nonce.toByteArray(),
            AionAddress.wrap(this.senderKey.getAddress()),
            null,
            this.value.toByteArray(),
            getDeploymentJarBytes(),
            this.energyLimit,
            this.energyPrice,
            VirtualMachineSpecs.AVM_CREATE_CODE);
        transaction.sign(this.senderKey);
        return transaction;
    }

    private byte[] getDeploymentJarBytes() {
        return new CodeAndArguments(JarBuilder.buildJarForMainAndClasses(this.mainClass, this.otherClasses), this.clinitArgs).encodeToBytes();
    }

}
