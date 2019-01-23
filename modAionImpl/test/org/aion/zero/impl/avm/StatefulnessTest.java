package org.aion.zero.impl.avm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Collections;
import org.aion.avm.api.ABIEncoder;
import org.aion.avm.core.NodeEnvironment;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.base.type.AionAddress;
import org.aion.base.vm.VirtualMachineSpecs;
import org.aion.crypto.ECKey;
import org.aion.mcf.core.ImportResult;
import org.aion.vm.VirtualMachineFactory;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.impl.StandaloneBlockchain;
import org.aion.zero.impl.avm.contracts.Statefulness;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.impl.types.AionBlockSummary;
import org.aion.zero.types.AionTransaction;
import org.aion.zero.types.AionTxReceipt;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class StatefulnessTest {
    private StandaloneBlockchain blockchain;
    private ECKey deployerKey;
    private Address deployer;
    private long energyPrice = 1;

    @BeforeClass
    public static void setupAvm() {
        VirtualMachineFactory.getFactorySingleton().initializeAllVirtualMachines();
    }

    @AfterClass
    public static void tearDownAvm() {
        VirtualMachineFactory.getFactorySingleton().shutdownAllVirtualMachines();
    }

    @Before
    public void setup() {
        StandaloneBlockchain.Bundle bundle = new StandaloneBlockchain.Builder()
            .withDefaultAccounts()
            .withValidatorConfiguration("simple")
            .build();
        this.blockchain = bundle.bc;
        this.deployerKey = bundle.privateKeys.get(0);
        this.deployer = AionAddress.wrap(this.deployerKey.getAddress());
    }

    @After
    public void tearDown() {
        this.blockchain = null;
        this.deployerKey = null;
    }

    @Test
    public void testDeployContract() {
        AionTxReceipt receipt = deployContract();

        // Check the contract has the Avm prefix, and deployment succeeded.
        assertEquals(NodeEnvironment.CONTRACT_PREFIX, receipt.getTransactionOutput()[0]);
        assertTrue(receipt.isSuccessful());
    }

    @Test
    public void testStateOfActorsAfterDeployment() {
        BigInteger deployerBalance = getBalance(this.deployer);
        BigInteger deployerNonce = getNonce(this.deployer);

        AionTxReceipt receipt = deployContract();

        // Check the contract has the Avm prefix, and deployment succeeded, and grab the address.
        assertEquals(NodeEnvironment.CONTRACT_PREFIX, receipt.getTransactionOutput()[0]);
        assertTrue(receipt.isSuccessful());
        Address contract = AionAddress.wrap(receipt.getTransactionOutput());

        BigInteger deployerBalanceAfterDeployment = getBalance(this.deployer);
        BigInteger deployerNonceAfterDeployment = getNonce(this.deployer);
        BigInteger contractBalance = getBalance(contract);
        BigInteger contractNonce = this.blockchain.getRepository().getNonce(contract);

        BigInteger deploymentEnergyCost = BigInteger.valueOf(receipt.getEnergyUsed()).multiply(BigInteger.valueOf(this.energyPrice));

        // Check that balances and nonce are in agreement after the deployment.
        assertEquals(deployerBalance.subtract(deploymentEnergyCost), deployerBalanceAfterDeployment);
        assertEquals(deployerNonce.add(BigInteger.ONE), deployerNonceAfterDeployment);
        assertEquals(BigInteger.ZERO, contractBalance);
        assertEquals(BigInteger.ZERO, contractNonce);
    }

    @Test
    public void testUsingCallInContract() {
        AionTxReceipt receipt = deployContract();

        // Check the contract has the Avm prefix, and deployment succeeded, and grab the address.
        assertEquals(NodeEnvironment.CONTRACT_PREFIX, receipt.getTransactionOutput()[0]);
        assertTrue(receipt.isSuccessful());
        Address contract = AionAddress.wrap(receipt.getTransactionOutput());

        BigInteger deployerInitialBalance = getBalance(this.deployer);
        BigInteger contractInitialBalance = getBalance(contract);
        BigInteger fundsToSendToContract = BigInteger.valueOf(1000);

        // Transfer some value to the contract so we can do a 'call' from within it.
        receipt = transferValueTo(contract, fundsToSendToContract);
        assertTrue(receipt.isSuccessful());

        // verify that the sender and contract balances are correct after the transfer.
        BigInteger transferEnergyCost = BigInteger.valueOf(receipt.getEnergyUsed()).multiply(BigInteger.valueOf(this.energyPrice));
        BigInteger deployerBalanceAfterTransfer = deployerInitialBalance.subtract(fundsToSendToContract).subtract(transferEnergyCost);
        BigInteger contractBalanceAfterTransfer = contractInitialBalance.add(fundsToSendToContract);

        assertEquals(deployerBalanceAfterTransfer, getBalance(this.deployer));
        assertEquals(contractBalanceAfterTransfer, getBalance(contract));

        Address beneficiary = randomAionAddress();
        long valueForContractToSend = fundsToSendToContract.longValue() / 2;

        receipt = callContract(contract, "transferValue", beneficiary.toBytes(), valueForContractToSend);
        assertTrue(receipt.isSuccessful());

        BigInteger deployerBalanceAfterCall = getBalance(this.deployer);
        BigInteger contractBalanceAfterCall = getBalance(contract);
        BigInteger beneficiaryBalanceAfterCall = getBalance(beneficiary);

        BigInteger callEnergyCost = BigInteger.valueOf(receipt.getEnergyUsed()).multiply(BigInteger.valueOf(this.energyPrice));

        assertEquals(deployerBalanceAfterTransfer.subtract(callEnergyCost), deployerBalanceAfterCall);
//        assertEquals(contractBalanceAfterTransfer.subtract(BigInteger.valueOf(valueForContractToSend)), contractBalanceAfterCall);
        assertEquals(BigInteger.valueOf(valueForContractToSend), beneficiaryBalanceAfterCall);
    }

    // Returns the contract address and verifies it was imported into the new best block.
    private AionTxReceipt deployContract() {
        byte[] jar = getJarBytes();
        AionTransaction transaction = newTransaction(
            getNonce(this.deployer),
            this.deployer,
            null,
            BigInteger.ZERO,
            jar,
            5_000_000,
            this.energyPrice,
            VirtualMachineSpecs.AVM_VM_CODE);
        transaction.sign(this.deployerKey);

        AionBlock block = this.blockchain.createNewBlock(this.blockchain.getBestBlock(), Collections.singletonList(transaction), false);
        Pair<ImportResult, AionBlockSummary> connectResult = this.blockchain.tryToConnectAndFetchSummary(block);
        assertEquals(ImportResult.IMPORTED_BEST, connectResult.getLeft());
        return connectResult.getRight().getReceipts().get(0);
    }

    private AionTxReceipt callContract(Address contract, String method, Object... arguments) {
        AionTransaction transaction = newTransaction(
            getNonce(this.deployer),
            this.deployer,
            contract,
            BigInteger.ZERO,
            abiEncodeMethodCall(method, arguments),
            2_000_000,
            this.energyPrice,
            VirtualMachineSpecs.AVM_VM_CODE);
        transaction.sign(this.deployerKey);

        AionBlock block = this.blockchain.createNewBlock(this.blockchain.getBestBlock(), Collections.singletonList(transaction), false);
        Pair<ImportResult, AionBlockSummary> connectResult = this.blockchain.tryToConnectAndFetchSummary(block);
        assertEquals(ImportResult.IMPORTED_BEST, connectResult.getLeft());
        return connectResult.getRight().getReceipts().get(0);
    }

    private AionTxReceipt transferValueTo(Address beneficiary, BigInteger value) {
        AionTransaction transaction = newTransaction(
            getNonce(this.deployer),
            this.deployer,
            beneficiary,
            value,
            new byte[0],
            2_000_000,
            this.energyPrice,
            (byte) 0x1);
        transaction.sign(this.deployerKey);

        AionBlock block = this.blockchain.createNewBlock(this.blockchain.getBestBlock(), Collections.singletonList(transaction), false);
        Pair<ImportResult, AionBlockSummary> connectResult = this.blockchain.tryToConnectAndFetchSummary(block);
        assertEquals(ImportResult.IMPORTED_BEST, connectResult.getLeft());
        return connectResult.getRight().getReceipts().get(0);
    }

    private byte[] getJarBytes() {
        return new CodeAndArguments(JarBuilder.buildJarForMainAndClasses(Statefulness.class), new byte[0]).encodeToBytes();
    }

    private byte[] abiEncodeMethodCall(String method, Object... arguments) {
        return ABIEncoder.encodeMethodArguments(method, arguments);
    }

    private AionTransaction newTransaction(BigInteger nonce, Address sender, Address destination, BigInteger value, byte[] data, long energyLimit, long energyPrice, byte vm) {
        return new AionTransaction(nonce.toByteArray(), sender, destination, value.toByteArray(), data, energyLimit, energyPrice, vm);
    }

    private BigInteger getBalance(Address address) {
        return this.blockchain.getRepository().getBalance(address);
    }

    private BigInteger getNonce(Address address) {
        return this.blockchain.getRepository().getNonce(address);
    }

    private Address randomAionAddress() {
        byte[] bytes = RandomUtils.nextBytes(32);
        bytes[0] = (byte) 0xa0;
        return AionAddress.wrap(bytes);
    }

}
