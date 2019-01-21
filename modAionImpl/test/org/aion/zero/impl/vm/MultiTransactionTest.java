package org.aion.zero.impl.vm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.aion.base.type.AionAddress;
import org.aion.crypto.ECKey;
import org.aion.mcf.core.ImportResult;
import org.aion.util.conversions.Hex;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.impl.StandaloneBlockchain;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.impl.types.AionBlockSummary;
import org.aion.zero.impl.vm.contracts.ContractUtils;
import org.aion.zero.types.AionTransaction;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests sending off multiple transactions in a single block.
 */
public class MultiTransactionTest {
    private StandaloneBlockchain blockchain;
    private List<ECKey> keys;

    @Before
    public void setup() {
        StandaloneBlockchain.Bundle bundle =
            new StandaloneBlockchain.Builder()
                .withDefaultAccounts()
                .withValidatorConfiguration("simple")
                .build();
        this.blockchain = bundle.bc;
        this.keys = bundle.privateKeys;
    }

    @After
    public void tearDown() {
        this.blockchain = null;
    }

    @Test
    public void testTwoValueTransfersInSameBlock() {
        Address sender = recoverAddress(this.keys.get(0));
        Address destination = getRandomAddress();
        BigInteger nonce = this.blockchain.getRepository().getNonce(sender);
        BigInteger value = BigInteger.valueOf(12345);

        BigInteger balance = this.blockchain.getRepository().getBalance(sender);

        AionTransaction transaction1 = new AionTransaction(nonce.toByteArray(), sender, destination, value.toByteArray(), new byte[0], 2_000_000, 1, (byte) 0x1);
        AionTransaction transaction2 = new AionTransaction(nonce.add(BigInteger.ONE).toByteArray(), sender, destination, value.toByteArray(), new byte[0], 2_000_000, 1, (byte) 0x1);

        transaction1.sign(this.keys.get(0));
        transaction2.sign(this.keys.get(0));

        List<AionTransaction> transactions = new ArrayList<>();
        transactions.add(transaction1);
        transactions.add(transaction2);

        AionBlockSummary summary = sendTransactions(transactions);

        BigInteger energyUsed1 = BigInteger.valueOf(summary.getSummaries().get(0).getReceipt().getEnergyUsed());
        BigInteger energyUsed2 = BigInteger.valueOf(summary.getSummaries().get(1).getReceipt().getEnergyUsed());

        assertEquals(nonce.add(BigInteger.TWO), this.blockchain.getRepository().getNonce(sender));
        assertEquals(value.add(value), this.blockchain.getRepository().getBalance(destination));
        assertEquals(balance.subtract(value).subtract(value).subtract(energyUsed1).subtract(energyUsed2), this.blockchain.getRepository().getBalance(sender));
    }

    @Test
    public void testTwoFastVmTransactionsInSameBlock() throws IOException {
        Address sender = recoverAddress(this.keys.get(0));
        BigInteger nonce = this.blockchain.getRepository().getNonce(sender);
        BigInteger balance = this.blockchain.getRepository().getBalance(sender);

        AionBlockSummary summary = deployFvmContract();

        Address contract = summary.getSummaries().get(0).getTransaction().getContractAddress();
        BigInteger energyUsed = BigInteger.valueOf(summary.getReceipts().get(0).getEnergyUsed());

        assertEquals(nonce.add(BigInteger.ONE), this.blockchain.getRepository().getNonce(sender));
        assertEquals(balance.subtract(energyUsed), this.blockchain.getRepository().getBalance(sender));

        AionTransaction transaction1 = getTransactionToCallFunctionF(sender, nonce.add(BigInteger.ONE), contract);
        AionTransaction transaction2 = getTransactionToCallFunctionG(sender, nonce.add(BigInteger.TWO), contract);

        List<AionTransaction> transactions = new ArrayList<>();
        transactions.add(transaction1);
        transactions.add(transaction2);

        summary = sendTransactions(transactions);

        BigInteger energyUsed1 = BigInteger.valueOf(summary.getSummaries().get(0).getReceipt().getEnergyUsed());
        BigInteger energyUsed2 = BigInteger.valueOf(summary.getSummaries().get(1).getReceipt().getEnergyUsed());

        assertEquals(nonce.add(BigInteger.valueOf(3)), this.blockchain.getRepository().getNonce(sender));
        assertEquals(BigInteger.ZERO, this.blockchain.getRepository().getBalance(contract));
        assertEquals(balance.subtract(energyUsed).subtract(energyUsed1).subtract(energyUsed2), this.blockchain.getRepository().getBalance(sender));
    }

    private AionBlockSummary deployFvmContract() throws IOException {
        byte[] deployCode = ContractUtils.getContractDeployer("ByteArrayMap.sol", "ByteArrayMap");

        Address sender = recoverAddress(this.keys.get(0));
        BigInteger nonce = this.blockchain.getRepository().getNonce(sender);

        AionTransaction transaction = new AionTransaction(nonce.toByteArray(), sender, null, BigInteger.ZERO.toByteArray(), deployCode, 5_000_000, 1, (byte) 0x1);
        transaction.sign(this.keys.get(0));

        return sendTransactions(Collections.singletonList(transaction));
    }

    private AionBlockSummary sendTransactions(List<AionTransaction> transactions) {
        AionBlock parentBlock = this.blockchain.getBestBlock();
        AionBlock block = this.blockchain.createNewBlock(parentBlock, transactions, false);

        Pair<ImportResult, AionBlockSummary> connectResult = this.blockchain.tryToConnectAndFetchSummary(block);
        assertEquals(ImportResult.IMPORTED_BEST, connectResult.getLeft());
        return connectResult.getRight();
    }

    private Address getRandomAddress() {
        byte[] bytes = RandomUtils.nextBytes(Address.SIZE);
        bytes[0] = (byte) 0xa0;
        return AionAddress.wrap(bytes);
    }

    private Address recoverAddress(ECKey key) {
        return AionAddress.wrap(key.getAddress());
    }

    private AionTransaction getTransactionToCallFunctionF(Address sender, BigInteger nonce, Address contract) {
        byte[] data = Hex.decode("26121ff0");

        AionTransaction transaction = new AionTransaction(nonce.toByteArray(), sender, contract, BigInteger.ZERO.toByteArray(), data, 2_000_000, 1, (byte) 0x1);
        transaction.sign(this.keys.get(0));

        return transaction;
    }

    private AionTransaction getTransactionToCallFunctionG(Address sender, BigInteger nonce, Address contract) {
        byte[] data = Hex.decode("e2179b8e");

        AionTransaction transaction = new AionTransaction(nonce.toByteArray(), sender, contract, BigInteger.ZERO.toByteArray(), data, 2_000_000, 1, (byte) 0x1);
        transaction.sign(this.keys.get(0));

        return transaction;
    }

}
