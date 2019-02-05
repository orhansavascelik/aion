package org.aion.zero.impl.vm.resources;

import java.math.BigInteger;
import java.util.Arrays;
import org.aion.base.type.AionAddress;
import org.aion.crypto.ECKey;
import org.aion.mcf.core.ImportResult;
import org.aion.vm.VirtualMachineProvider;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.impl.StandaloneBlockchain;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.impl.types.AionBlockSummary;
import org.aion.zero.types.AionTransaction;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A class that provides methods that should make writing virtual machine integration tests simple
 * and easy.
 *
 * This class abstracts away the notion of a blockchain or a repository or private keys.
 *
 * There is a single pre-mined account that will be the sender of every transaction if no sender
 * parameter exists.
 *
 * There are convenient methods for creating various types of transactions, for executing them, and
 * also for some simple state querying.
 */
public class TransactionExecutionHelper {
    private final StandaloneBlockchain blockchain;
    private final ECKey deployerKey;
    private final Address deployer;
    private boolean virtualMachinesAreLive = false;

    private TransactionExecutionHelper() {
        StandaloneBlockchain.Bundle bundle = new StandaloneBlockchain.Builder()
            .withDefaultAccounts()
            .withValidatorConfiguration("simple")
            .withAvmEnabled()
            .build();
        this.blockchain = bundle.bc;
        this.deployerKey = bundle.privateKeys.get(0);
        this.deployer = AionAddress.wrap(this.deployerKey.getAddress());
    }

    /**
     * Returns a new execution helper and initializes all the supported virtual machines.
     *
     * Once done with this class, the {@code shutdown()} method must be invoked!
     */
    public static TransactionExecutionHelper newExecutionHelper() {
        TransactionExecutionHelper helper = new TransactionExecutionHelper();
        VirtualMachineProvider.initializeAllVirtualMachines();
        helper.virtualMachinesAreLive = true;
        return helper;
    }

    /**
     * Constructs a new transaction that will create an Avm dApp for the provided classes.
     *
     * This transaction will be sent by a pre-mined account using its current nonce.
     *
     * @param mainClass The main class of the dApp.
     * @param otherClasses Any dependencies the main class has.
     * @return The avm create transaction.
     */
    public AionTransaction makeAvmCreateTransaction(Class<?> mainClass, Class<?>... otherClasses) {
        return new AvmCreateTransactionBuilder()
            .senderKey(this.deployerKey)
            .nonce(this.blockchain.getRepository().getNonce(this.deployer))
            .mainClass(mainClass)
            .otherClasses(otherClasses)
            .buildAvmCreate();
    }

    /**
     * Constructs a new transaction that will call an existing Avm dApp's provided method using the
     * given parameters.
     *
     * This transaction will be sent by a pre-mined account using its current nonce.
     *
     * @param contract The contract to call.
     * @param method The method to invoke.
     * @param parameters The parameters to invoke the method with.
     * @return The avm call transaction.
     */
    public AionTransaction makeAvmCallTransaction(Address contract, String method, Object... parameters) {
        return new AvmCallTransactionBuilder()
            .senderKey(this.deployerKey)
            .contract(contract)
            .nonce(this.blockchain.getRepository().getNonce(this.deployer))
            .method(method)
            .methodParameters(parameters)
            .buildAvmCall();
    }

    /**
     * Constructs a new transaction that will only transfer the specified value to the provided
     * beneficiary.
     *
     * This transaction will be sent by a pre-mined account using its current nonce.
     *
     * @param beneficiary The beneficiary of the value transfer.
     * @param value The amount of value to transfer.
     * @return The transfer transaction.
     */
    public AionTransaction makeValueTransferTransaction(Address beneficiary, BigInteger value) {
        return new TransferValueTransactionBuilder()
            .senderKey(this.deployerKey)
            .beneficiary(beneficiary)
            .nonce(this.blockchain.getRepository().getNonce(this.deployer))
            .value(value)
            .buildValueTransfer();
    }

    /**
     * Runs the provided transactions by constructing a block that holds all of the provided
     * transactions and then attempts to attach the new block to the blockchain.
     *
     * @param transactions The transactions to run.
     * @return The import result and block summary pertaining to the provided transactions.
     */
    public Pair<ImportResult, AionBlockSummary> runTransactions(AionTransaction... transactions) {
        if (!this.virtualMachinesAreLive) {
            throw new IllegalStateException("Cannot run: virtual machines have been shut down.");
        }
        AionBlock parent = this.blockchain.getBestBlock();
        AionBlock block = this.blockchain.createBlock(parent, Arrays.asList(transactions), false, parent.getTimestamp());
        return this.blockchain.tryToConnectAndFetchSummary(block);
    }

    /**
     * Returns the balance of the pre-mined account.
     *
     * @return The balance.
     */
    public BigInteger getBalanceOfPreminedAccount() {
        return this.blockchain.getRepository().getBalance(this.deployer);
    }

    /**
     * Returns the balance of the specified account.
     *
     * @param address The account whose balance is to be queried.
     * @return The balance.
     */
    public BigInteger getBalanceOf(Address address) {
        return this.blockchain.getRepository().getBalance(address);
    }

    /**
     * Returns the nonce of the pre-mined account.
     *
     * @return The nonce.
     */
    public BigInteger getNonceOfPreminedAccount() {
        return this.blockchain.getRepository().getNonce(this.deployer);
    }

    /**
     * Returns the nonce of the specified account.
     *
     * @param address The account whose nonce is to be queried.
     * @return The nonce.
     */
    public BigInteger getNonceOf(Address address) {
        return this.blockchain.getRepository().getNonce(address);
    }

    /**
     * Starts up all supported virtual machines.
     */
    public void start() {
        VirtualMachineProvider.initializeAllVirtualMachines();
        this.virtualMachinesAreLive = true;
    }

    /**
     * Shuts down the virtual machines. This method must be invoked when done with this class.
     */
    public void shutdown() {
        VirtualMachineProvider.shutdownAllVirtualMachines();
        this.virtualMachinesAreLive = false;
    }

}
