package org.aion.mcf.types;

import java.math.BigInteger;
import org.aion.base.type.Address;
import org.aion.base.type.IBlockHeader;
import org.aion.log.AionLoggerFactory;
import org.spongycastle.util.BigIntegers;

/** Abstract BlockHeader. */
public abstract class AbstractBlockHeader implements IBlockHeader {

    public static final int NONCE_LENGTH = 32;
    public static final int SOLUTIONSIZE = 1408;
    private static final int MAX_DIFFICULTY_LENGTH = 16;

    protected byte version;

    /* The SHA3 256-bit hash of the parent block, in its entirety */
    protected byte[] parentHash;

    /*
     * The 256-bit address to which all fees collected from the successful
     * mining of this block be transferred; formally
     */
    protected Address coinbase;
    /*
     * The SHA3 256-bit hash of the root node of the state trie, after all
     * transactions are executed and finalisations applied
     */
    protected byte[] stateRoot;
    /*
     * The SHA3 256-bit hash of the root node of the trie structure populated
     * with each transaction in the transaction list portion, the trie is
     * populate by [key, val] --> [rlp(index), rlp(tx_recipe)] of the block
     */
    protected byte[] txTrieRoot;
    /*
     * The SHA3 256-bit hash of the root node of the trie structure populated
     * with each transaction recipe in the transaction recipes list portion, the
     * trie is populate by [key, val] --> [rlp(index), rlp(tx_recipe)] of the
     * block
     */
    protected byte[] receiptTrieRoot;

    /* todo: comment it when you know what the fuck it is */
    protected byte[] logsBloom;


    /*
     * A scalar value equal to the reasonable output of Unix's time() at this
     * block's inception
     */
    protected long timestamp;

    /*
     * A scalar value equal to the number of ancestor blocks. The genesis block
     * has a number of zero
     */
    protected long number;

    /*
     * An arbitrary byte array containing data relevant to this block. With the
     * exception of the genesis block, this must be 32 bytes or fewer
     */
    protected byte[] extraData;


    /////////////////////////////////////////////////////////////////
    // (1344 in 200-9, 1408 in 210,9)
    protected byte[] solution; // The equihash solution in compressed format

    /*
     * A long value containing energy consumed within this block
     */
    protected long energyConsumed;

    /*
     * A long value containing energy limit of this block
     */
    protected long energyLimit;

    public byte[] getSolution() {
        return solution;
    }

    public void setSolution(byte[] solution) {
        this.solution = solution;
    }

    public AbstractBlockHeader() {}

    public byte[] getParentHash() {
        return parentHash;
    }

    public Address getCoinbase() {
        return coinbase;
    }

    public void setCoinbase(Address coinbase) {
        this.coinbase = coinbase;
    }

    public byte[] getStateRoot() {
        return this.stateRoot;
    }

    public void setStateRoot(byte[] stateRoot) {
        this.stateRoot = stateRoot;
    }

    public byte[] getTxTrieRoot() {
        return txTrieRoot;
    }

    public void setTxTrieRoot(byte[] txTrieRoot) {
        this.txTrieRoot = txTrieRoot;
    }

    public void setReceiptsRoot(byte[] receiptTrieRoot) {
        this.receiptTrieRoot = receiptTrieRoot;
    }

    public byte[] getReceiptsRoot() {
        return receiptTrieRoot;
    }

    public void setTransactionsRoot(byte[] stateRoot) {
        this.txTrieRoot = stateRoot;
    }

    public byte[] getLogsBloom() {
        return logsBloom;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public byte[] getExtraData() {
        return extraData;
    }

    public void setLogsBloom(byte[] logsBloom) {
        this.logsBloom = logsBloom;
    }

    public void setExtraData(byte[] extraData) {
        this.extraData = extraData;
    }

    public boolean isGenesis() {
        return this.number == 0;
    }

    public byte getVersion() {
        return this.version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }
}
