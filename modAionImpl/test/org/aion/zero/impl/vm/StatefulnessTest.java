package org.aion.zero.impl.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import org.aion.avm.core.NodeEnvironment;
import org.aion.base.type.AionAddress;
import org.aion.mcf.core.ImportResult;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.impl.vm.contracts.Statefulness;
import org.aion.zero.impl.types.AionBlockSummary;
import org.aion.zero.impl.vm.resources.TransactionExecutionHelper;
import org.aion.zero.types.AionTransaction;
import org.aion.zero.types.AionTxReceipt;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

// These tests are ignored for now because in order for them to pass we need the clock drift buffer
// time to be set to 2 seconds instead of 1. We still have to figure out how we are going to handle
// this... You can make this change locally to verify these tests pass.

public class StatefulnessTest {
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
        AionTxReceipt receipt = deployContract();

        // Check the contract has the Avm prefix, and deployment succeeded.
        assertEquals(NodeEnvironment.CONTRACT_PREFIX, receipt.getTransactionOutput()[0]);
        assertTrue(receipt.isSuccessful());
    }

    @Test
    public void testStateOfActorsAfterDeployment() {
        BigInteger deployerBalance = helper.getBalanceOfPreminedAccount();
        BigInteger deployerNonce = helper.getNonceOfPreminedAccount();

        AionTxReceipt receipt = deployContract();

        // Check the contract has the Avm prefix, and deployment succeeded, and grab the address.
        assertEquals(NodeEnvironment.CONTRACT_PREFIX, receipt.getTransactionOutput()[0]);
        assertTrue(receipt.isSuccessful());
        Address contract = AionAddress.wrap(receipt.getTransactionOutput());

        BigInteger deployerBalanceAfterDeployment = helper.getBalanceOfPreminedAccount();
        BigInteger deployerNonceAfterDeployment = helper.getNonceOfPreminedAccount();
        BigInteger contractBalance = helper.getBalanceOf(contract);
        BigInteger contractNonce = helper.getNonceOf(contract);

        BigInteger deploymentEnergyCost =
                BigInteger.valueOf(receipt.getEnergyUsed());

        // Check that balances and nonce are in agreement after the deployment.
        assertEquals(
                deployerBalance.subtract(deploymentEnergyCost), deployerBalanceAfterDeployment);
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

        BigInteger deployerInitialNonce = helper.getNonceOfPreminedAccount();
        BigInteger contractInitialNonce = helper.getNonceOf(contract);
        BigInteger deployerInitialBalance = helper.getBalanceOfPreminedAccount();
        BigInteger contractInitialBalance = helper.getBalanceOf(contract);
        BigInteger fundsToSendToContract = BigInteger.valueOf(1000);

        // Transfer some value to the contract so we can do a 'call' from within it.
        receipt = transferValueTo(contract, fundsToSendToContract);
        assertTrue(receipt.isSuccessful());

        // verify that the sender and contract balances are correct after the transfer.
        BigInteger transferEnergyCost =
                BigInteger.valueOf(receipt.getEnergyUsed());
        BigInteger deployerBalanceAfterTransfer =
                deployerInitialBalance.subtract(fundsToSendToContract).subtract(transferEnergyCost);
        BigInteger contractBalanceAfterTransfer = contractInitialBalance.add(fundsToSendToContract);
        BigInteger deployerNonceAfterTransfer = deployerInitialNonce.add(BigInteger.ONE);

        assertEquals(deployerBalanceAfterTransfer, helper.getBalanceOfPreminedAccount());
        assertEquals(contractBalanceAfterTransfer, helper.getBalanceOf(contract));
        assertEquals(deployerNonceAfterTransfer, helper.getNonceOfPreminedAccount());
        assertEquals(contractInitialNonce, helper.getNonceOf(contract));

        // Generate a random beneficiary to transfer funds to via the contract.
        Address beneficiary = randomAionAddress();
        long valueForContractToSend = fundsToSendToContract.longValue() / 2;

        // Call the contract to send value using an internal call.
        receipt =
                callContract(
                        contract, "transferValue", beneficiary.toBytes(), valueForContractToSend);
        assertTrue(receipt.isSuccessful());

        // Verify the accounts have the expected state.
        BigInteger deployerBalanceAfterCall = helper.getBalanceOfPreminedAccount();
        BigInteger contractBalanceAfterCall = helper.getBalanceOf(contract);
        BigInteger beneficiaryBalanceAfterCall = helper.getBalanceOf(beneficiary);

        BigInteger callEnergyCost =
                BigInteger.valueOf(receipt.getEnergyUsed());

        assertEquals(
                deployerBalanceAfterTransfer.subtract(callEnergyCost), deployerBalanceAfterCall);
        assertEquals(
                contractBalanceAfterTransfer.subtract(BigInteger.valueOf(valueForContractToSend)),
                contractBalanceAfterCall);
        assertEquals(BigInteger.valueOf(valueForContractToSend), beneficiaryBalanceAfterCall);
        assertEquals(deployerNonceAfterTransfer.add(BigInteger.ONE), helper.getNonceOfPreminedAccount());

        // The contract nonce increases because it fires off an internal transaction.
        assertEquals(contractInitialNonce.add(BigInteger.ONE), helper.getNonceOf(contract));
    }

    private AionTxReceipt deployContract() {
        AionTransaction transaction = helper.makeAvmCreateTransaction(Statefulness.class);
        return sendTransactions(transaction);
    }

    private AionTxReceipt callContract(Address contract, String method, Object... arguments) {
        AionTransaction transaction = helper.makeAvmCallTransaction(contract, method, arguments);
        return sendTransactions(transaction);
    }

    private AionTxReceipt transferValueTo(Address beneficiary, BigInteger value) {
        AionTransaction transaction = helper.makeValueTransferTransaction(beneficiary, value);
        return sendTransactions(transaction);
    }

    private AionTxReceipt sendTransactions(AionTransaction... transactions) {
        Pair<ImportResult, AionBlockSummary> connectResult = helper.runTransactions(transactions);
        assertEquals(ImportResult.IMPORTED_BEST, connectResult.getLeft());
        return connectResult.getRight().getReceipts().get(0);
    }

    private Address randomAionAddress() {
        byte[] bytes = RandomUtils.nextBytes(32);
        bytes[0] = (byte) 0xa0;
        return AionAddress.wrap(bytes);
    }
}
