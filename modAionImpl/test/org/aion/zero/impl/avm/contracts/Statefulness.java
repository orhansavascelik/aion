package org.aion.zero.impl.avm.contracts;

import java.math.BigInteger;
import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.Address;
import org.aion.avm.api.BlockchainRuntime;

public class Statefulness {
    private static int counter = 0;

    public static byte[] main() {
        return ABIDecoder.decodeAndRunWithClass(Statefulness.class, BlockchainRuntime.getData());
    }

    public static void transferValue(Address beneficiary, long amount) {
        if (BlockchainRuntime.call(beneficiary, BigInteger.valueOf(amount), new byte[0], BlockchainRuntime.getRemainingEnergy()).isSuccess()) {
            BlockchainRuntime.println("Transfer was a success. "
                + "Beneficiary balance = " + BlockchainRuntime.getBalance(beneficiary)
                + ", Contract balance = " + BlockchainRuntime.getBalance(BlockchainRuntime.getAddress()));
        } else {
            BlockchainRuntime.println("Transfer was unsuccessful.");
        }
        counter++;
    }

    public static void incrementCounter() {
        counter++;
    }

    public static int getCount() {
        BlockchainRuntime.println("Count = " + counter);
        return counter;
    }

    public static long getContractBalance() {
        BigInteger balance =  BlockchainRuntime.getBalance(BlockchainRuntime.getAddress());
        BlockchainRuntime.println("Contract balance = " + balance);
        counter++;
        return balance.longValue();
    }

    public static long getBalanceOf(Address address) {
        BigInteger balance =  BlockchainRuntime.getBalance(address);
        BlockchainRuntime.println("Balance of " + address + " = " + balance);
        counter++;
        return balance.longValue();
    }
}
