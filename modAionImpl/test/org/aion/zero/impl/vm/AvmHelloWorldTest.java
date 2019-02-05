package org.aion.zero.impl.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Collections;
import org.aion.avm.core.NodeEnvironment;
import org.aion.base.type.AionAddress;
import org.aion.crypto.ECKey;
import org.aion.mcf.core.ImportResult;
import org.aion.vm.VirtualMachineProvider;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.impl.StandaloneBlockchain;
import org.aion.zero.impl.vm.contracts.AvmHelloWorld;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.impl.types.AionBlockSummary;
import org.aion.zero.impl.vm.resources.AvmCallTransactionBuilder;
import org.aion.zero.impl.vm.resources.AvmCreateTransactionBuilder;
import org.aion.zero.types.AionTransaction;
import org.aion.zero.types.AionTxReceipt;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AvmHelloWorldTest {
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
        AionTransaction transaction = new AvmCreateTransactionBuilder()
            .senderKey(deployerKey)
            .nonce(BigInteger.ZERO)
            .mainClass(AvmHelloWorld.class)
            .buildAvmCreate();

        AionBlock block = this.blockchain.createNewBlock(this.blockchain.getBestBlock(), Collections.singletonList(transaction), false);
        Pair<ImportResult, AionBlockSummary> connectResult = this.blockchain.tryToConnectAndFetchSummary(block);
        AionTxReceipt receipt = connectResult.getRight().getReceipts().get(0);

        // Check the block was imported, the contract has the Avm prefix, and deployment succeeded.
        assertEquals(ImportResult.IMPORTED_BEST, connectResult.getLeft());
        assertEquals(NodeEnvironment.CONTRACT_PREFIX, receipt.getTransactionOutput()[0]);
        assertTrue(receipt.isSuccessful());
    }

    @Test
    public void testDeployAndCallContract() {
        // Deploy the contract.
        AionTransaction transaction = new AvmCreateTransactionBuilder()
            .senderKey(this.deployerKey)
            .nonce(BigInteger.ZERO)
            .mainClass(AvmHelloWorld.class)
            .buildAvmCreate();

        AionBlock block = this.blockchain.createNewBlock(this.blockchain.getBestBlock(), Collections.singletonList(transaction), false);
        Pair<ImportResult, AionBlockSummary> connectResult = this.blockchain.tryToConnectAndFetchSummary(block);
        AionTxReceipt receipt = connectResult.getRight().getReceipts().get(0);

        // Check the block was imported, the contract has the Avm prefix, and deployment succeeded.
        assertEquals(ImportResult.IMPORTED_BEST, connectResult.getLeft());
        assertEquals(NodeEnvironment.CONTRACT_PREFIX, receipt.getTransactionOutput()[0]);
        assertTrue(receipt.isSuccessful());

        Address contract = AionAddress.wrap(receipt.getTransactionOutput());
        transaction = new AvmCallTransactionBuilder()
            .senderKey(this.deployerKey)
            .contract(contract)
            .nonce(BigInteger.ONE)
            .method("sayHello")
            .buildAvmCall();

        block = this.blockchain.createNewBlock(this.blockchain.getBestBlock(), Collections.singletonList(transaction), false);
        connectResult = this.blockchain.tryToConnectAndFetchSummary(block);
        receipt = connectResult.getRight().getReceipts().get(0);

        // Check the block was imported and the transaction was successful.
        assertEquals(ImportResult.IMPORTED_BEST, connectResult.getLeft());
        assertTrue(receipt.isSuccessful());
    }

}