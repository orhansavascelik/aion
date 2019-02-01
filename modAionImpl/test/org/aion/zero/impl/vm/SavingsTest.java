package org.aion.zero.impl.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Arrays;
import org.aion.avm.api.ABIEncoder;
import org.aion.avm.core.NodeEnvironment;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.userlib.AionMap;
import org.aion.base.type.AionAddress;
import org.aion.base.vm.VirtualMachineSpecs;
import org.aion.crypto.ECKey;
import org.aion.mcf.core.ImportResult;
import org.aion.util.conversions.Hex;
import org.aion.vm.VirtualMachineProvider;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.impl.StandaloneBlockchain;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.impl.types.AionBlockSummary;
import org.aion.zero.impl.vm.contracts.Savings;
import org.aion.zero.types.AionTransaction;
import org.aion.zero.types.AionTxReceipt;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SavingsTest {
    private StandaloneBlockchain blockchain;
    private ECKey deployerKey;

    @BeforeClass
    public static void setupAvm() {
        VirtualMachineProvider.initializeAllVirtualMachines();
    }

    @AfterClass
    public static void tearDownAvm() {
        VirtualMachineProvider.shutdownAllVirtualMachines();
    }

    @Before
    public void setup() {
        StandaloneBlockchain.Bundle bundle = new StandaloneBlockchain.Builder()
            .withDefaultAccounts()
            .withValidatorConfiguration("simple")
            .withAvmEnabled()
            .build();
        this.blockchain = bundle.bc;
        this.deployerKey = bundle.privateKeys.get(0);
    }

    @After
    public void tearDown() {
        this.blockchain = null;
        this.deployerKey = null;
    }

    @Test
    public void testDeployContract() {
        AionTransaction transaction = getDeployTransaction();

        AionBlockSummary connectResult = sendTransactions(transaction);
        AionTxReceipt receipt = connectResult.getReceipts().get(0);

        // Check the contract has the Avm prefix, and deployment succeeded.
        assertEquals(NodeEnvironment.CONTRACT_PREFIX, receipt.getTransactionOutput()[0]);
        assertTrue(receipt.isSuccessful());
    }

    @Test
    public void testInitContract() {
        Address contract = deployContract();
        byte[] callData = encodeCallToInit(100, 0);
        AionTransaction transaction = getCallTransaction(contract, callData);

        AionBlockSummary connectResult = sendTransactions(transaction);
        AionTxReceipt receipt = connectResult.getReceipts().get(0);
    }

    @Test
    public void test() {
        deployContract();
    }

    private Address deployContract() {
        AionTransaction transaction = getDeployTransaction();
        AionBlockSummary connectResult = sendTransactions(transaction);
        AionTxReceipt receipt = connectResult.getReceipts().get(0);
        return AionAddress.wrap(receipt.getTransactionOutput());
    }

    private AionTransaction getDeployTransaction() {
        byte[] jar = getJarBytes();
        AionTransaction transaction = newTransaction(
            BigInteger.ZERO,
            AionAddress.wrap(deployerKey.getAddress()),
            null,
            jar,
            5_000_000);
        transaction.sign(this.deployerKey);
        return transaction;
    }

    private AionTransaction getCallTransaction(Address contract, byte[] callData) {
        Address deployer = AionAddress.wrap(deployerKey.getAddress());
        AionTransaction transaction = newTransaction(
            this.blockchain.getRepository().getNonce(deployer),
            deployer,
            contract,
            callData,
            2_000_000);
        transaction.sign(this.deployerKey);
        return transaction;
    }

    private AionBlockSummary sendTransactions(AionTransaction... transactions) {
        AionBlock block = this.blockchain.createNewBlock(this.blockchain.getBestBlock(), Arrays.asList(transactions), false);
        Pair<ImportResult, AionBlockSummary> connectResult = this.blockchain.tryToConnectAndFetchSummary(block);
        assertTrue(connectResult.getLeft().isSuccessful());
        return connectResult.getRight();
    }

    private byte[] getJarBytes() {
        return new CodeAndArguments(JarBuilder.buildJarForMainAndClasses(Savings.class, AionMap.class), new byte[0]).encodeToBytes();
    }

    @Test
    public void test2() {
        System.out.println(Hex.toHexString(ABIEncoder.encodeMethodArguments("sayHello")));
    }

    private byte[] encodeCallToInit(int numPeriods, int t0special) {
        return ABIEncoder.encodeMethodArguments("init", numPeriods, t0special);
    }

    private AionTransaction newTransaction(BigInteger nonce, Address sender, Address destination, byte[] data, long energyLimit) {
        return new AionTransaction(nonce.toByteArray(), sender, destination, BigInteger.ZERO.toByteArray(), data, energyLimit, 1, VirtualMachineSpecs.AVM_CREATE_CODE);
    }

}
