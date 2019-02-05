package org.aion.zero.impl.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.aion.avm.core.NodeEnvironment;
import org.aion.base.type.AionAddress;
import org.aion.mcf.core.ImportResult;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.impl.vm.contracts.AvmHelloWorld;
import org.aion.zero.impl.types.AionBlockSummary;
import org.aion.zero.impl.vm.resources.TransactionExecutionHelper;
import org.aion.zero.types.AionTransaction;
import org.aion.zero.types.AionTxReceipt;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AvmHelloWorldTest {
    private static TransactionExecutionHelper helper;

    @BeforeClass
    public static void setupAvm() {
        helper = TransactionExecutionHelper.start();
    }

    @AfterClass
    public static void tearDownAvm() {
        helper.shutdown();
    }

    @Test
    public void testDeployContract() {
        AionTransaction transaction = helper.makeAvmCreateTransaction(AvmHelloWorld.class);

        Pair<ImportResult, AionBlockSummary> connectResult = helper.runTransactions(transaction);
        AionTxReceipt receipt = connectResult.getRight().getReceipts().get(0);

        // Check the block was imported, the contract has the Avm prefix, and deployment succeeded.
        assertEquals(ImportResult.IMPORTED_BEST, connectResult.getLeft());
        assertEquals(NodeEnvironment.CONTRACT_PREFIX, receipt.getTransactionOutput()[0]);
        assertTrue(receipt.isSuccessful());
    }

    @Test
    public void testDeployAndCallContract() {
        // Deploy the contract.
        AionTransaction transaction = helper.makeAvmCreateTransaction(AvmHelloWorld.class);
        Pair<ImportResult, AionBlockSummary> connectResult = helper.runTransactions(transaction);
        AionTxReceipt receipt = connectResult.getRight().getReceipts().get(0);

        // Check the block was imported, the contract has the Avm prefix, and deployment succeeded.
        assertEquals(ImportResult.IMPORTED_BEST, connectResult.getLeft());
        assertEquals(NodeEnvironment.CONTRACT_PREFIX, receipt.getTransactionOutput()[0]);
        assertTrue(receipt.isSuccessful());

        Address contract = AionAddress.wrap(receipt.getTransactionOutput());

        transaction = helper.makeAvmCallTransaction(contract, "sayHello");
        connectResult = helper.runTransactions(transaction);
        receipt = connectResult.getRight().getReceipts().get(0);

        // Check the block was imported and the transaction was successful.
        assertEquals(ImportResult.IMPORTED_BEST, connectResult.getLeft());
        assertTrue(receipt.isSuccessful());
    }

}
