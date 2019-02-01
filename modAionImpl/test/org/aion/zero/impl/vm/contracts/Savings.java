package org.aion.zero.impl.vm.contracts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Map;
import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.Address;
import org.aion.avm.api.BlockchainRuntime;
import org.aion.avm.userlib.AionMap;

public class Savings {
    private static Address owner = BlockchainRuntime.getCaller();
    private static Address newOwner = null;

    private static boolean inited = false;
    private static boolean locked = false;
    private static boolean nullified = false;

    private static int periods = 0;
    private static int t0special = 0;
    private static int intervalSecs = 60;
    private static int precision = 18;
    private static long startTime = 0;
    private static BigInteger total = BigInteger.ZERO;
    private static BigInteger totalfv = BigInteger.ZERO;
    private static BigInteger remainder = BigInteger.ZERO;
    private static Map<Address, BigInteger> deposited = new AionMap<>();
    private static Map<Address, BigInteger> withdrawn = new AionMap<>();

    static {
        BlockchainRuntime.println("--> " + new Object().hashCode());
    }

    public static byte[] main() {
        return ABIDecoder.decodeAndRunWithClass(Savings.class, BlockchainRuntime.getData());
    }

    public static void changeOwner(Address address) {
        BlockchainRuntime.require(callerIsOwner());
        newOwner = address;
    }

    public static void acceptOwnership() {
        if (BlockchainRuntime.getCaller().equals(newOwner)) {
            BlockchainRuntime.log("CHANGE_OWNER".getBytes(), newOwner.unwrap());
            owner = newOwner;
            newOwner = null;
        }
    }

    public static void init(int numPeriods, int special) {
        BlockchainRuntime.require(callerIsOwner());
        BlockchainRuntime.require(notInitialized());

        BlockchainRuntime.require(numPeriods != 0);
        periods = numPeriods;
        t0special = special;
    }

    public static void finalizeInit() {
        BlockchainRuntime.require(callerIsOwner());
        BlockchainRuntime.require(notInitialized());

        inited = true;
    }

    public static void lock() {
        BlockchainRuntime.require(callerIsOwner());

        locked = true;
    }

    public static void start(long startBlockTimestamp) {
        BlockchainRuntime.require(callerIsOwner());
        BlockchainRuntime.require(initialized());
        BlockchainRuntime.require(preStart());

        startTime = startBlockTimestamp;
        total = BlockchainRuntime.getBalanceOfThisContract();
        totalfv = total;
        remainder = total;
    }

    public static boolean isStarted() {
        return locked && (startTime != 0);
    }

    public static void refund(Address address, byte[] amount) {
        BlockchainRuntime.require(callerIsOwner());
        BlockchainRuntime.require(preLock());

        BlockchainRuntime.call(address, new BigInteger(1, amount), new byte[0], BlockchainRuntime.getRemainingEnergy());
    }

    public static long periodAt(long time) {
        if (startTime > time) {
            return 0;
        }
        long p = ((time - startTime) / intervalSecs) + 1;
        return (p > periods) ? periods : p;
    }

    public static long period() {
        return periodAt(BlockchainRuntime.getBlockTimestamp());
    }

    public static void mint(Address address, byte[] amount) {
        BlockchainRuntime.require(callerIsOwner());
        BlockchainRuntime.require(preLock());
        BlockchainRuntime.require(notNullified());

        BigInteger amountAsBigInt = new BigInteger(1, amount);
        BlockchainRuntime.log("MINT".getBytes(), address.unwrap(), amountAsBigInt.toByteArray());
        deposited.put(address, amountAsBigInt);
    }

    public static void deposit(byte[] amount) {
        depositTo(BlockchainRuntime.getCaller(), amount);
    }

    public static void depositTo(Address beneficiary, byte[] amount) {
        BlockchainRuntime.require(callerIsOwner());
        BlockchainRuntime.require(preLock());
        BlockchainRuntime.require(notNullified());

        BigInteger amountAsBigInt = new BigInteger(1, amount);

        BlockchainRuntime.require(BlockchainRuntime.call(beneficiary, amountAsBigInt, new byte[0], BlockchainRuntime.getRemainingEnergy()).isSuccess());
        BlockchainRuntime.log("DEPOSIT".getBytes(), beneficiary.unwrap(), amountAsBigInt.toByteArray());

        BigInteger deposit = deposited.get(beneficiary);
        if (deposit == null) {
            deposited.put(beneficiary, amountAsBigInt);
        } else {
            deposited.put(beneficiary, deposit.add(amountAsBigInt));
        }

        totalfv = totalfv.add(amountAsBigInt);
    }

    public static boolean withdraw() {
        return withdrawTo(BlockchainRuntime.getCaller());
    }

    public static boolean withdrawTo(Address address) {
        BlockchainRuntime.require(postStart());
        BlockchainRuntime.require(notNullified());

        BigInteger hasDeposited = deposited.get(address);
        BigInteger hasWithdrawn = withdrawn.get(address);
        hasWithdrawn = (hasWithdrawn == null) ? BigInteger.ZERO : hasWithdrawn;
        BigInteger diff = _withdrawTo(hasDeposited, hasWithdrawn, BlockchainRuntime.getBlockTimestamp());

        if (diff.equals(BigInteger.ZERO)) {
            return false;
        }

        BlockchainRuntime.require((diff.add(hasWithdrawn)).compareTo(hasDeposited) <= 0);

        BlockchainRuntime.log("WITHDRAW".getBytes(), address.unwrap(), diff.toByteArray());
        BlockchainRuntime.call(address, diff, new byte[0], BlockchainRuntime.getRemainingEnergy());

        withdrawn.put(address, withdrawn.get(address).add(diff));
        remainder = remainder.subtract(diff);
        return true;
    }

    public static void bulkWithdraw(Address[] addresses) {
        for (Address address : addresses) {
            withdrawTo(address);
        }
    }

    private static BigInteger _withdrawTo(BigInteger hasDeposited, BigInteger hasWithdrawn, long time) {
        if (hasDeposited == null) {
            return BigInteger.ZERO;
        }

        BigDecimal fraction = availableForWithdrawalAt(time);
        BigInteger withdrawable = (new BigDecimal(hasDeposited).multiply(fraction)).toBigInteger();
        return (withdrawable.compareTo(hasWithdrawn) > 0) ? withdrawable.subtract(hasWithdrawn) : BigInteger.ZERO;
    }

    private static BigDecimal availableForWithdrawalAt(long time) {
        BigDecimal numerator = BigDecimal.valueOf(t0special).add(BigDecimal.valueOf(periodAt(time)));
        BigDecimal denominator = BigDecimal.valueOf(t0special).add(BigDecimal.valueOf(periods));

        return numerator.divide(denominator, precision, RoundingMode.HALF_DOWN);
    }

    private static boolean callerIsOwner() {
        return BlockchainRuntime.getCaller().equals(owner);
    }

    private static boolean preLock() {
        return !locked && (startTime == 0);
    }

    private static boolean preStart() {
        return locked && (startTime == 0);
    }

    private static boolean postStart() {
        return locked && (startTime != 0);
    }

    private static boolean notNullified() {
        return !nullified;
    }

    private static boolean initialized() {
        return inited;
    }

    private static boolean notInitialized() {
        return !inited;
    }

}
