/*
 * Copyright (c) 2017-2018 Aion foundation.
 *
 *     This file is part of the aion network project.
 *
 *     The aion network project is free software: you can redistribute it
 *     and/or modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation, either version 3 of
 *     the License, or any later version.
 *
 *     The aion network project is distributed in the hope that it will
 *     be useful, but WITHOUT ANY WARRANTY; without even the implied
 *     warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *     See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with the aion network project source files.
 *     If not, see <https://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Aion foundation.
 */
package org.aion.zero.impl;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static java.math.BigInteger.ZERO;
import static java.util.Collections.emptyList;
import static org.aion.base.util.BIUtil.isMoreThan;
import static org.aion.base.util.Hex.toHexString;
import static org.aion.mcf.core.ImportResult.EXIST;
import static org.aion.mcf.core.ImportResult.IMPORTED_BEST;
import static org.aion.mcf.core.ImportResult.IMPORTED_NOT_BEST;
import static org.aion.mcf.core.ImportResult.INVALID_BLOCK;
import static org.aion.mcf.core.ImportResult.NO_PARENT;

import com.google.common.annotations.VisibleForTesting;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.aion.base.db.IRepository;
import org.aion.base.db.IRepositoryCache;
import org.aion.base.type.AionAddress;
import org.aion.base.type.Hash256;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.base.util.ByteUtil;
import org.aion.base.util.FastByteComparisons;
import org.aion.base.util.Hex;
import org.aion.base.vm.DebugInfo;
import org.aion.crypto.HashUtil;
import org.aion.equihash.EquihashMiner;
import org.aion.evtmgr.IEvent;
import org.aion.evtmgr.IEventMgr;
import org.aion.evtmgr.impl.evt.EventBlock;
import org.aion.log.AionLoggerFactory;
import org.aion.log.LogEnum;
import org.aion.mcf.core.AccountState;
import org.aion.mcf.core.ImportResult;
import org.aion.mcf.db.IBlockStoreBase;
import org.aion.mcf.db.IBlockStorePow;
import org.aion.mcf.db.TransactionStore;
import org.aion.mcf.manager.ChainStatistics;
import org.aion.mcf.trie.Trie;
import org.aion.mcf.trie.TrieImpl;
import org.aion.mcf.types.BlockIdentifier;
import org.aion.mcf.valid.BlockHeaderValidator;
import org.aion.mcf.valid.GrandParentBlockHeaderValidator;
import org.aion.mcf.valid.ParentBlockHeaderValidator;
import org.aion.mcf.vm.types.Bloom;
import org.aion.rlp.RLP;
import org.aion.vm.BulkExecutor;
import org.aion.vm.ExecutionBatch;
import org.aion.vm.PostExecutionWork;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.exceptions.HeaderStructureException;
import org.aion.zero.impl.blockchain.ChainConfiguration;
import org.aion.zero.impl.config.CfgAion;
import org.aion.zero.impl.core.IAionBlockchain;
import org.aion.zero.impl.core.energy.AbstractEnergyStrategyLimit;
import org.aion.zero.impl.core.energy.EnergyStrategies;
import org.aion.zero.impl.db.AionBlockStore;
import org.aion.zero.impl.db.AionRepositoryImpl;
import org.aion.zero.impl.sync.SyncMgr;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.impl.types.AionBlockSummary;
import org.aion.zero.impl.types.AionTxInfo;
import org.aion.zero.impl.types.RetValidPreBlock;
import org.aion.zero.impl.valid.TXValidator;
import org.aion.zero.types.A0BlockHeader;
import org.aion.zero.types.AionTransaction;
import org.aion.zero.types.AionTxExecSummary;
import org.aion.zero.types.AionTxReceipt;
import org.aion.zero.types.IAionBlock;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: clean and clarify best block
// bestKnownBlock - block with the highest block number
// pubBestBlock - block with the highest total difficulty
// bestBlock - current best block inside the blockchain implementation

/**
 * Core blockchain consensus algorithms, the rule within this class decide whether the correct chain
 * from branches and dictates the placement of items into {@link AionRepositoryImpl} as well as
 * managing the state trie. This module also collects stats about block propagation, from its point
 * of view.
 *
 * <p>The module is also responsible for generate new blocks, mostly called by {@link EquihashMiner}
 * to generate new blocks to mine. As for receiving blocks, this class interacts with {@link
 * SyncMgr} to manage the importing of blocks from network.
 */
public class AionBlockchainImpl implements IAionBlockchain {

    private static final Logger LOG = LoggerFactory.getLogger(LogEnum.CONS.name());
    private static final int THOUSAND_MS = 1000;
    private static final int DIFFICULTY_BYTES = 16;

    private A0BCConfig config;
    private long exitOn = Long.MAX_VALUE;

    private AionRepositoryImpl repository;
    private IRepositoryCache track;
    private TransactionStore<AionTransaction, AionTxReceipt, org.aion.zero.impl.types.AionTxInfo>
            transactionStore;
    private AionBlock bestBlock;

    private static final Logger LOGGER_VM = AionLoggerFactory.getLogger(LogEnum.VM.toString());

    /**
     * This version of the bestBlock is only used for external reference (ex. through {@link
     * #getBestBlock()}), this is done because {@link #bestBlock} can slip into temporarily
     * inconsistent states while forking, and we don't want to expose that information to external
     * actors.
     *
     * <p>However we would still like to publish a bestBlock without locking, therefore we introduce
     * a volatile block that is only published when all forking/appending behaviour is completed.
     */
    private volatile AionBlock pubBestBlock;

    private volatile BigInteger totalDifficulty = ZERO;
    private ChainStatistics chainStats;

    private final GrandParentBlockHeaderValidator<A0BlockHeader> grandParentBlockHeaderValidator;
    private final ParentBlockHeaderValidator<A0BlockHeader> parentHeaderValidator;
    private final BlockHeaderValidator<A0BlockHeader> blockHeaderValidator;
    private AtomicReference<BlockIdentifier> bestKnownBlock =
            new AtomicReference<BlockIdentifier>();

    private boolean fork = false;

    private Address minerCoinbase;
    private byte[] minerExtraData;

    private Stack<State> stateStack = new Stack<>();
    private IEventMgr evtMgr = null;

    private AbstractEnergyStrategyLimit energyLimitStrategy;

    /**
     * Chain configuration class, because chain configuration may change dependant on the block
     * being executed. This is simple for now but in the future we may have to create a "chain
     * configuration provider" to provide us different configurations.
     */
    ChainConfiguration chainConfiguration;

    /**
     * Helper method for generating the adapter between this class and {@link CfgAion}
     *
     * @param cfgAion
     * @return {@code configuration} instance that directly references the singleton instance of
     *     cfgAion
     */
    private static A0BCConfig generateBCConfig(CfgAion cfgAion) {
        ChainConfiguration config = new ChainConfiguration();
        return new A0BCConfig() {
            @Override
            public Address getCoinbase() {
                return cfgAion.getGenesis().getCoinbase();
            }

            @Override
            public byte[] getExtraData() {
                return cfgAion.getConsensus().getExtraData().getBytes();
            }

            @Override
            public boolean getExitOnBlockConflict() {
                return true;
                // return cfgAion.getSync().getExitOnBlockConflict();
            }

            @Override
            public Address getMinerCoinbase() {
                return AionAddress.wrap(cfgAion.getConsensus().getMinerAddress());
            }

            // TODO: hook up to configuration file
            @Override
            public int getFlushInterval() {
                return 1;
            }

            @Override
            public AbstractEnergyStrategyLimit getEnergyLimitStrategy() {
                return EnergyStrategies.getEnergyStrategy(
                        cfgAion.getConsensus().getEnergyStrategy().getStrategy(),
                        cfgAion.getConsensus().getEnergyStrategy(),
                        config);
            }
        };
    }

    private AionBlockchainImpl() {
        this(generateBCConfig(CfgAion.inst()), AionRepositoryImpl.inst(), new ChainConfiguration());
    }

    protected AionBlockchainImpl(
            final A0BCConfig config,
            final AionRepositoryImpl repository,
            final ChainConfiguration chainConfig) {
        this.config = config;
        this.repository = repository;
        this.chainStats = new ChainStatistics();

        /**
         * Because we dont have any hardforks, later on chain configuration must be determined by
         * blockHash and number.
         */
        this.chainConfiguration = chainConfig;

        this.grandParentBlockHeaderValidator =
                this.chainConfiguration.createGrandParentHeaderValidator();
        this.parentHeaderValidator = this.chainConfiguration.createParentHeaderValidator();
        this.blockHeaderValidator = this.chainConfiguration.createBlockHeaderValidator();

        this.transactionStore = this.repository.getTransactionStore();

        this.minerCoinbase = this.config.getMinerCoinbase();

        if (minerCoinbase.isEmptyAddress()) {
            LOG.warn("No miner Coinbase!");
        }

        /** Save a copy of the miner extra data */
        byte[] extraBytes = this.config.getExtraData();
        this.minerExtraData =
                new byte[this.chainConfiguration.getConstants().getMaximumExtraDataSize()];
        if (extraBytes.length < this.chainConfiguration.getConstants().getMaximumExtraDataSize()) {
            System.arraycopy(extraBytes, 0, this.minerExtraData, 0, extraBytes.length);
        } else {
            System.arraycopy(
                    extraBytes,
                    0,
                    this.minerExtraData,
                    0,
                    this.chainConfiguration.getConstants().getMaximumExtraDataSize());
        }
        this.energyLimitStrategy = config.getEnergyLimitStrategy();
    }

    /**
     * Initialize as per the <a href=
     * "https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom">Initialization-on-demand</a>
     * holder pattern
     */
    private static class Holder {
        static final AionBlockchainImpl INSTANCE = new AionBlockchainImpl();
    }

    public static AionBlockchainImpl inst() {
        return Holder.INSTANCE;
    }

    /**
     * Should be set after initialization, note that the blockchain will still operate if not set,
     * just will not emit events.
     *
     * @param eventManager
     */
    void setEventManager(IEventMgr eventManager) {
        this.evtMgr = eventManager;
    }

    public AionBlockStore getBlockStore() {
        return repository.getBlockStore();
    }

    /**
     * Referenced only by external
     *
     * <p>Note: If you are making changes to this method and want to use it to track internal state,
     * use {@link #bestBlock} instead
     *
     * @return {@code bestAionBlock}
     * @see #pubBestBlock
     */
    @Override
    public byte[] getBestBlockHash() {
        return getBestBlock().getHash();
    }

    /**
     * Referenced only by external
     *
     * <p>Note: If you are making changes to this method and want to use it to track internal state,
     * opt for {@link #getSizeInternal()} instead.
     *
     * @return {@code positive long} representing the current size
     * @see #pubBestBlock
     */
    @Override
    public long getSize() {
        return getBestBlock().getNumber() + 1;
    }

    /** @see #getSize() */
    private long getSizeInternal() {
        return this.bestBlock.getNumber() + 1;
    }

    @Override
    public AionBlock getBlockByNumber(long blockNr) {
        return getBlockStore().getChainBlockByNumber(blockNr);
    }

    @Override
    /* NOTE: only returns receipts from the main chain
     */
    @SuppressWarnings("Duplicates")
    public AionTxInfo getTransactionInfo(byte[] hash) {

        List<AionTxInfo> infos = transactionStore.get(hash);

        if (infos == null || infos.isEmpty()) {
            return null;
        }

        AionTxInfo txInfo = null;
        if (infos.size() == 1) {
            txInfo = infos.get(0);
        } else {
            // pick up the receipt from the block on the main chain
            for (AionTxInfo info : infos) {
                AionBlock block = getBlockStore().getBlockByHash(info.getBlockHash());
                if (block == null) continue;

                AionBlock mainBlock = getBlockStore().getChainBlockByNumber(block.getNumber());
                if (mainBlock == null) continue;

                if (FastByteComparisons.equal(info.getBlockHash(), mainBlock.getHash())) {
                    txInfo = info;
                    break;
                }
            }
        }
        if (txInfo == null) {
            LOG.warn("Can't find block from main chain for transaction " + toHexString(hash));
            return null;
        }

        AionTransaction tx =
                this.getBlockByHash(txInfo.getBlockHash())
                        .getTransactionsList()
                        .get(txInfo.getIndex());
        txInfo.setTransaction(tx);
        return txInfo;
    }

    @SuppressWarnings("Duplicates")
    // returns transaction info (tx receipt) without the transaction embedded in it.
    // saves on db reads for api when processing large transactions
    public AionTxInfo getTransactionInfoLite(byte[] txHash, byte[] blockHash) {
        return transactionStore.get(txHash, blockHash);
    }

    @Override
    public AionBlock getBlockByHash(byte[] hash) {
        return getBlockStore().getBlockByHash(hash);
    }

    @Override
    public List<byte[]> getListOfHashesEndWith(byte[] hash, int qty) {
        return getBlockStore().getListHashesEndWith(hash, qty < 1 ? 1 : qty);
    }

    @Override
    public List<byte[]> getListOfHashesStartFromBlock(long blockNumber, int qty) {
        // avoiding errors due to negative qty
        qty = qty < 1 ? 1 : qty;

        long bestNumber = bestBlock.getNumber();

        if (blockNumber > bestNumber) {
            return emptyList();
        }

        if (blockNumber + qty - 1 > bestNumber) {
            qty = (int) (bestNumber - blockNumber + 1);
        }

        long endNumber = blockNumber + qty - 1;

        IAionBlock block = getBlockByNumber(endNumber);

        List<byte[]> hashes = getBlockStore().getListHashesEndWith(block.getHash(), qty);

        // asc order of hashes is required in the response
        Collections.reverse(hashes);

        return hashes;
    }

    private static byte[] calcTxTrie(List<AionTransaction> transactions) {

        Trie txsState = new TrieImpl(null);

        if (transactions == null || transactions.isEmpty()) {
            return HashUtil.EMPTY_TRIE_HASH;
        }

        for (int i = 0; i < transactions.size(); i++) {
            byte[] txEncoding = transactions.get(i).getEncoded();
            if (txEncoding != null) {
                txsState.update(RLP.encodeInt(i), txEncoding);
            } else {
                return HashUtil.EMPTY_TRIE_HASH;
            }
        }
        return txsState.getRootHash();
    }

    public AionRepositoryImpl getRepository() {
        return repository;
    }

    private State pushState(byte[] bestBlockHash) {
        State push = stateStack.push(new State());
        this.bestBlock = getBlockStore().getBlockByHash(bestBlockHash);
        this.totalDifficulty = getBlockStore().getTotalDifficultyForHash(bestBlockHash);
        this.repository =
                (AionRepositoryImpl) this.repository.getSnapshotTo(this.bestBlock.getStateRoot());
        return push;
    }

    private void popState() {
        State state = stateStack.pop();
        this.repository = state.savedRepo;
        this.bestBlock = state.savedBest;
        this.totalDifficulty = state.savedTD;
    }

    private void dropState() {
        stateStack.pop();
    }

    /**
     * Not thread safe, currently only run in {@link #tryToConnect(AionBlock)}, assumes that the
     * environment is already locked
     *
     * @param block
     * @return
     */
    private AionBlockSummary tryConnectAndFork(final AionBlock block) {
        State savedState = pushState(block.getParentHash());
        this.fork = true;

        AionBlockSummary summary = null;
        try {
            summary = add(block);
        } catch (Exception e) {
            LOG.error("Unexpected error: ", e);
        } finally {
            this.fork = false;
        }

        if (summary != null && isMoreThan(this.totalDifficulty, savedState.savedTD)) {

            if (LOG.isInfoEnabled()) {
                LOG.info(
                        "branching: from = {}/{}, to = {}/{}",
                        savedState.savedBest.getNumber(),
                        toHexString(savedState.savedBest.getHash()),
                        block.getNumber(),
                        toHexString(block.getHash()));
            }

            // main branch become this branch
            // cause we proved that total difficulty
            // is greater
            getBlockStore().reBranch(block);

            // The main repository rebranch
            this.repository = savedState.savedRepo;
            this.repository.syncToRoot(block.getStateRoot());

            // flushing
            flush();

            dropState();
        } else {
            // Stay on previous branch
            popState();
        }

        return summary;
    }

    private AtomicLong bestBlockNumber = new AtomicLong(0L);

    /**
     * Heuristic for skipping the call to tryToConnect with very large or very small block number.
     */
    public boolean skipTryToConnect(long blockNumber) {
        long current = bestBlockNumber.get();
        return blockNumber > current + 32 || blockNumber < current - 32;
    }

    /**
     * If using TOP pruning we need to check the pruning restriction for the block. Otherwise, there
     * is not prune restriction.
     */
    public boolean hasPruneRestriction() {
        // no restriction when not in TOP pruning mode
        return repository.usesTopPruning();
    }

    /**
     * Heuristic for skipping the call to tryToConnect with block number that was already pruned.
     */
    public boolean isPruneRestricted(long blockNumber) {
        // no restriction when not in TOP pruning mode
        if (!hasPruneRestriction()) {
            return false;
        }
        return blockNumber < bestBlockNumber.get() - repository.getPruneBlockCount() + 1;
    }

    public synchronized ImportResult tryToConnect(final AionBlock block) {
        return tryToConnectInternal(block, System.currentTimeMillis() / THOUSAND_MS);
    }

    public synchronized void compactState() {
        repository.compactState();
    }

    @VisibleForTesting
    Pair<ImportResult, AionBlockSummary> tryToConnectAndFetchSummary(
            AionBlock block, long currTimeSeconds) {
        // Check block exists before processing more rules
        if (getBlockStore().getMaxNumber() >= block.getNumber()
                && getBlockStore().isBlockExist(block.getHash())) {

            if (LOG.isDebugEnabled()) {
                LOG.debug(
                        "Block already exists hash: {}, number: {}",
                        block.getShortHash(),
                        block.getNumber());
            }

            if (!repository.isValidRoot(block.getStateRoot())) {
                // correct the world state for this block
                recoverWorldState(repository, block);
            }

            if (!repository.isIndexed(block.getHash(), block.getNumber())) {
                // correct the index for this block
                recoverIndexEntry(repository, block);
            }

            // retry of well known block
            return Pair.of(EXIST, null);
        }

        if (block.getTimestamp()
                > (currTimeSeconds
                        + this.chainConfiguration.getConstants().getClockDriftBufferTime())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                        "Block {} invalid due to timestamp {}.",
                        Hex.toHexString(block.getHash()),
                        block.getTimestamp());
            }
            return Pair.of(INVALID_BLOCK, null);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Try connect block hash: {}, number: {}",
                    block.getShortHash(),
                    block.getNumber());
        }

        final ImportResult ret;

        // The simple case got the block
        // to connect to the main chain
        final AionBlockSummary summary;
        if (bestBlock.isParentOf(block)) {
            repository.syncToRoot(bestBlock.getStateRoot());
            summary = add(block);
            ret = summary == null ? INVALID_BLOCK : IMPORTED_BEST;
        } else {
            if (getBlockStore().isBlockExist(block.getParentHash())) {
                BigInteger oldTotalDiff = getInternalTD();
                summary = tryConnectAndFork(block);
                ret =
                        summary == null
                                ? INVALID_BLOCK
                                : (isMoreThan(getInternalTD(), oldTotalDiff)
                                        ? IMPORTED_BEST
                                        : IMPORTED_NOT_BEST);
            } else {
                summary = null;
                ret = NO_PARENT;
            }
        }

        // update best block reference
        if (ret == IMPORTED_BEST) {
            pubBestBlock = bestBlock;
        }

        // fire block events
        if (ret.isSuccessful()) {
            if (this.evtMgr != null) {

                IEvent evtOnBlock = new EventBlock(EventBlock.CALLBACK.ONBLOCK0);
                evtOnBlock.setFuncArgs(Collections.singletonList(summary));
                this.evtMgr.newEvent(evtOnBlock);

                IEvent evtTrace = new EventBlock(EventBlock.CALLBACK.ONTRACE0);
                String str = String.format("Block chain size: [ %d ]", this.getSizeInternal());
                evtTrace.setFuncArgs(Collections.singletonList(str));
                this.evtMgr.newEvent(evtTrace);

                if (ret == IMPORTED_BEST) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("IMPORTED_BEST");
                    }
                    IEvent evtOnBest = new EventBlock(EventBlock.CALLBACK.ONBEST0);
                    evtOnBest.setFuncArgs(Arrays.asList(block, summary.getReceipts()));
                    this.evtMgr.newEvent(evtOnBest);
                }
            }
        }

        return Pair.of(ret, summary);
    }

    /**
     * Processes a new block and potentially appends it to the blockchain, thereby changing the
     * state of the world. Decoupled from wrapper function {@link #tryToConnect(AionBlock)} so we
     * can feed timestamps manually
     */
    ImportResult tryToConnectInternal(final AionBlock block, long currTimeSeconds) {
        return tryToConnectAndFetchSummary(block, currTimeSeconds).getLeft();
    }

    /**
     * Creates a new block, if you require more context refer to the blockContext creation method,
     * which allows us to add metadata not usually associated with the block itself.
     *
     * @param parent block
     * @param txs to be added into the block
     * @param waitUntilBlockTime if we should wait until the specified blockTime before create a new
     *     block
     * @see #createNewBlock(AionBlock, List, boolean)
     * @return new block
     */
    public synchronized AionBlock createNewBlock(
            AionBlock parent, List<AionTransaction> txs, boolean waitUntilBlockTime) {
        return createNewBlockInternal(
                        parent, txs, waitUntilBlockTime, System.currentTimeMillis() / THOUSAND_MS)
                .block;
    }

    /**
     * Creates a new block, adding in context/metadata about the block
     *
     * @param parent block
     * @param txs to be added into the block
     * @param waitUntilBlockTime if we should wait until the specified blockTime before create a new
     *     block
     * @see #createNewBlock(AionBlock, List, boolean)
     * @return new block
     */
    public synchronized BlockContext createNewBlockContext(
            AionBlock parent, List<AionTransaction> txs, boolean waitUntilBlockTime) {
        return createNewBlockInternal(
                parent, txs, waitUntilBlockTime, System.currentTimeMillis() / THOUSAND_MS);
    }

    BlockContext createNewBlockInternal(
            AionBlock parent,
            List<AionTransaction> txs,
            boolean waitUntilBlockTime,
            long currTimeSeconds) {
        long time = currTimeSeconds;

        if (parent.getTimestamp() >= time) {
            time = parent.getTimestamp() + 1;
            while (waitUntilBlockTime && System.currentTimeMillis() / THOUSAND_MS <= time) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        long energyLimit = this.energyLimitStrategy.getEnergyLimit(parent.getHeader());

        AionBlock block;
        try {
            A0BlockHeader.Builder headerBuilder =
                    new A0BlockHeader.Builder()
                            .withVersion((byte) 1)
                            .withParentHash(parent.getHash())
                            .withCoinbase(minerCoinbase)
                            .withNumber(parent.getNumber() + 1)
                            .withTimestamp(time)
                            .withExtraData(minerExtraData)
                            .withTxTrieRoot(calcTxTrie(txs))
                            .withEnergyLimit(energyLimit);
            block = new AionBlock(headerBuilder.build(), txs);
        } catch (HeaderStructureException e) {
            throw new RuntimeException(e);
        }

        IAionBlock grandParent = this.getParent(parent.getHeader());
        block.getHeader()
                .setDifficulty(
                        ByteUtil.bigIntegerToBytes(
                                this.chainConfiguration
                                        .getDifficultyCalculator()
                                        .calculateDifficulty(
                                                parent.getHeader(),
                                                grandParent == null
                                                        ? null
                                                        : grandParent.getHeader()),
                                DIFFICULTY_BYTES));
        /*
         * Begin execution phase
         */
        pushState(parent.getHash());

        track = repository.startTracking();

        RetValidPreBlock preBlock = generatePreBlock(block);

        /*
         * Calculate the gas used for the included transactions
         */
        long totalEnergyUsed = 0;
        BigInteger totalTransactionFee = BigInteger.ZERO;
        for (AionTxExecSummary summary : preBlock.summaries) {
            totalEnergyUsed = totalEnergyUsed + summary.getNrgUsed().longValueExact();
            totalTransactionFee = totalTransactionFee.add(summary.getFee());
        }

        byte[] stateRoot = getRepository().getRoot();
        popState();

        /*
         * End execution phase
         */
        Bloom logBloom = new Bloom();
        for (AionTxReceipt receipt : preBlock.receipts) {
            logBloom.or(receipt.getBloomFilter());
        }

        block.seal(
                preBlock.txs,
                calcTxTrie(preBlock.txs),
                stateRoot,
                logBloom.getBloomFilterBytes(),
                calcReceiptsTrie(preBlock.receipts),
                totalEnergyUsed);

        // derive base block reward
        BigInteger baseBlockReward =
                this.chainConfiguration.getRewardsCalculator().calculateReward(block.getHeader());
        return new BlockContext(block, baseBlockReward, totalTransactionFee);
    }

    @Override
    public synchronized AionBlockSummary add(AionBlock block) {
        // typical use without rebuild
        AionBlockSummary summary = add(block, false);

        if (summary != null) {
            List<AionTxReceipt> receipts = summary.getReceipts();

            updateTotalDifficulty(block);
            summary.setTotalDifficulty(block.getCumulativeDifficulty());

            storeBlock(block, receipts);

            flush();
        }

        return summary;
    }

    public synchronized AionBlockSummary add(AionBlock block, boolean rebuild) {

        if (!isValid(block)) {
            LOG.error("Attempting to add {} block.", (block == null ? "NULL" : "INVALID"));
            return null;
        }

        track = repository.startTracking();
        byte[] origRoot = repository.getRoot();

        // (if not reconstructing old blocks) keep chain continuity
        if (!rebuild && !Arrays.equals(bestBlock.getHash(), block.getParentHash())) {
            LOG.error("Attempting to add NON-SEQUENTIAL block.");
            return null;
        }

        AionBlockSummary summary = processBlock(block);
        List<AionTxReceipt> receipts = summary.getReceipts();

        // Sanity checks
        byte[] receiptHash = block.getReceiptsRoot();
        byte[] receiptListHash = calcReceiptsTrie(receipts);

        if (!Arrays.equals(receiptHash, receiptListHash)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(
                        "Block's given Receipt Hash doesn't match: {} != {}",
                        receiptHash,
                        receiptListHash);
                LOG.warn("Calculated receipts: " + receipts);
            }
            track.rollback();
            return null;
        }

        byte[] logBloomHash = block.getLogBloom();
        byte[] logBloomListHash = calcLogBloom(receipts);

        if (!Arrays.equals(logBloomHash, logBloomListHash)) {
            if (LOG.isWarnEnabled())
                LOG.warn(
                        "Block's given logBloom Hash doesn't match: {} != {}",
                        ByteUtil.toHexString(logBloomHash),
                        ByteUtil.toHexString(logBloomListHash));
            track.rollback();
            return null;
        }

        if (!rebuild) {
            byte[] blockStateRootHash = block.getStateRoot();
            byte[] worldStateRootHash = repository.getRoot();

            if (!Arrays.equals(blockStateRootHash, worldStateRootHash)) {

                LOG.warn(
                        "BLOCK: State conflict or received invalid block. block: {} worldstate {} mismatch",
                        block.getNumber(),
                        worldStateRootHash);
                LOG.warn("Conflict block dump: {}", toHexString(block.getEncoded()));

                track.rollback();
                // block is bad so 'rollback' the state root to the original
                // state
                repository.setRoot(origRoot);
            }
        }

        // update corresponding account with the new balance
        track.flush();

        if (rebuild) {
            for (int i = 0; i < receipts.size(); i++) {
                transactionStore.putToBatch(new AionTxInfo(receipts.get(i), block.getHash(), i));
            }
            transactionStore.flushBatch();

            repository.commitBlock(block.getHeader());

            if (LOG.isDebugEnabled())
                LOG.debug(
                        "Block rebuilt: number: {}, hash: {}, TD: {}",
                        block.getNumber(),
                        block.getShortHash(),
                        getBlockStore().getTotalDifficulty());
        }

        return summary;
    }

    @Override
    public void flush() {
        repository.flush();
        try {
            getBlockStore().flush();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        transactionStore.flush();
    }

    @SuppressWarnings("unused")
    private boolean needFlushByMemory(double maxMemoryPercents) {
        return getRuntime().freeMemory() < (getRuntime().totalMemory() * (1 - maxMemoryPercents));
    }

    private static byte[] calcReceiptsTrie(List<AionTxReceipt> receipts) {
        Trie receiptsTrie = new TrieImpl(null);

        if (receipts == null || receipts.isEmpty()) {
            return HashUtil.EMPTY_TRIE_HASH;
        }

        for (int i = 0; i < receipts.size(); i++) {
            receiptsTrie.update(RLP.encodeInt(i), receipts.get(i).getReceiptTrieEncoded());
        }
        return receiptsTrie.getRootHash();
    }

    private byte[] calcLogBloom(List<AionTxReceipt> receipts) {

        Bloom retBloomFilter = new Bloom();

        if (receipts == null || receipts.isEmpty()) {
            return retBloomFilter.getBloomFilterBytes();
        }

        for (AionTxReceipt receipt : receipts) {
            retBloomFilter.or(receipt.getBloomFilter());
        }

        return retBloomFilter.getBloomFilterBytes();
    }

    private IAionBlock getParent(A0BlockHeader header) {
        return getBlockStore().getBlockByHash(header.getParentHash());
    }

    public boolean isValid(A0BlockHeader header) {

        /*
         * Header should already be validated at this point, no need to check again
         * 1. Block came in from network; validated by P2P before processing further
         * 2. Block was submitted locally - adding invalid data to your own chain
         */
        //        if (!this.blockHeaderValidator.validate(header, LOG)) {
        //            return false;
        //        }

        IAionBlock parent = this.getParent(header);

        if (!this.parentHeaderValidator.validate(header, parent.getHeader(), LOG)) {
            return false;
        }

        IAionBlock grandParent = this.getParent(parent.getHeader());

        if (!this.grandParentBlockHeaderValidator.validate(
                grandParent == null ? null : grandParent.getHeader(),
                parent.getHeader(),
                header,
                LOG)) {
            return false;
        }

        return true;
    }

    /**
     * This mechanism enforces a homeostasis in terms of the time between blocks; a smaller period
     * between the last two blocks results in an increase in the difficulty level and thus
     * additional computation required, lengthening the likely next period. Conversely, if the
     * period is too large, the difficulty, and expected time to the next block, is reduced.
     */
    private boolean isValid(AionBlock block) {

        if (block == null) {
            return false;
        }

        if (!block.isGenesis()) {
            if (!isValid(block.getHeader())) {
                return false;
            }

            // Sanity checks
            String trieHash = toHexString(block.getTxTrieRoot());
            String trieListHash = toHexString(calcTxTrie(block.getTransactionsList()));

            if (!trieHash.equals(trieListHash)) {
                LOG.warn("Block's given Trie Hash doesn't match: {} != {}", trieHash, trieListHash);
                return false;
            }

            List<AionTransaction> txs = block.getTransactionsList();
            if (txs != null && !txs.isEmpty()) {
                IRepository parentRepo = repository;
                if (!Arrays.equals(
                        getBlockStore().getBestBlock().getHash(), block.getParentHash())) {
                    parentRepo =
                            repository.getSnapshotTo(
                                    getBlockByHash(block.getParentHash()).getStateRoot());
                }

                Map<Address, BigInteger> nonceCache = new HashMap<>();

                if (txs.parallelStream().anyMatch(tx -> !TXValidator.isValid(tx))) {
                    LOG.error("Some transactions in the block are invalid");
                    return false;
                }

                for (AionTransaction tx : txs) {
                    Address txSender = tx.getSenderAddress();

                    BigInteger expectedNonce = nonceCache.get(txSender);

                    if (expectedNonce == null) {
                        expectedNonce = parentRepo.getNonce(txSender);
                    }

                    BigInteger txNonce = new BigInteger(1, tx.getNonce());

                    if (!expectedNonce.equals(txNonce)) {
                        LOG.warn(
                                "Invalid transaction: Tx nonce {} != expected nonce {} (parent nonce: {}): {}",
                                txNonce.toString(),
                                expectedNonce.toString(),
                                parentRepo.getNonce(txSender),
                                tx);
                        return false;
                    }

                    // update cache
                    nonceCache.put(txSender, expectedNonce.add(BigInteger.ONE));
                }
            }
        }

        return true;
    }

    public static Set<ByteArrayWrapper> getAncestors(
            IBlockStorePow<IAionBlock, A0BlockHeader> blockStore,
            IAionBlock testedBlock,
            int limitNum,
            boolean isParentBlock) {
        Set<ByteArrayWrapper> ret = new HashSet<>();
        limitNum = (int) max(0, testedBlock.getNumber() - limitNum);
        IAionBlock it = testedBlock;
        if (!isParentBlock) {
            it = blockStore.getBlockByHash(it.getParentHash());
        }
        while (it != null && it.getNumber() >= limitNum) {
            ret.add(new ByteArrayWrapper(it.getHash()));
            it = blockStore.getBlockByHash(it.getParentHash());
        }
        return ret;
    }

    private AionBlockSummary processBlock(AionBlock block) {

        if (!block.isGenesis()) {
            return applyBlock(block);
        } else {
            return new AionBlockSummary(
                    block,
                    new HashMap<Address, BigInteger>(),
                    new ArrayList<AionTxReceipt>(),
                    new ArrayList<AionTxExecSummary>());
        }
    }

    /**
     * For generating the necessary transactions for a block
     *
     * @param block
     * @return
     */
    private RetValidPreBlock generatePreBlock(IAionBlock block) {
        DebugInfo.currentBlockNumber = block.getNumber();
        DebugInfo.isGeneratePreBlock = true;
        DebugInfo.isApplyBlock = false;

        long saveTime = System.nanoTime();

        List<AionTxReceipt> receipts = new ArrayList<>();
        List<AionTxExecSummary> summaries = new ArrayList<>();
        List<AionTransaction> transactions = new ArrayList<>();

        ExecutionBatch batch = new ExecutionBatch(block, block.getTransactionsList());
        BulkExecutor executor =
                new BulkExecutor(
                        batch,
                        repository,
                        track,
                        false,
                        true,
                        block.getNrgLimit(),
                        LOGGER_VM,
                        getPostExecutionWorkForGeneratePreBlock());
        List<AionTxExecSummary> executionSummaries = executor.execute();

        for (AionTxExecSummary summary : executionSummaries) {
            if (!summary.isRejected()) {
                transactions.add(summary.getTransaction());
                receipts.add(summary.getReceipt());
                summaries.add(summary);
            }
        }

        Map<Address, BigInteger> rewards = addReward(block, summaries);

        track.flush();

        long totalTime = System.nanoTime() - saveTime;
        chainStats.addBlockExecTime(totalTime);
        return new RetValidPreBlock(transactions, rewards, receipts, summaries);
    }

    /**
     * Returns a {@link PostExecutionWork} object whose {@code doPostExecutionWork()} method will
     * run the provided logic defined in this method. This work is to be applied after each
     * transaction has been run.
     *
     * <p>This "work" is specific to the {@link AionBlockchainImpl#generatePreBlock(IAionBlock)}
     * method.
     */
    private static PostExecutionWork getPostExecutionWorkForGeneratePreBlock() {
        return new PostExecutionWork() {
            @Override
            public long doPostExecutionWork(IRepository repository,
                IRepositoryCache<AccountState, IBlockStoreBase<?, ?>> repositoryChild,
                AionTxExecSummary summary, AionTransaction transaction, long blockEnergyRemaining) {

                if (!summary.isRejected()) {
                    if (DebugInfo.currentBlockNumber == 257159) {
                        DebugInfo.currentPipelineStage = "generatePreBlockWork childRepository.flush()";
                    }

                    repositoryChild.flush();

                    AionTxReceipt receipt = summary.getReceipt();
                    byte[] root = repository.getRoot();
                    receipt.setPostTxState(root);
                    receipt.setTransaction(transaction);

                    if (DebugInfo.currentBlockNumber == 257159) {
                        System.out.println("$$$_WORK_gen_$$$ topRepo root hash = " + Hex.toHexString(root));
                    }

                    return receipt.getEnergyUsed();
                } else {
                    return 0;
                }
            }
        };



//        return (topRepository,
//                childRepository,
//                transactionSummary,
//                transaction,
//                blockEnergyLeft) -> {
//            if (!transactionSummary.isRejected()) {
//
//                if (DebugInfo.currentBlockNumber == 257159) {
//                    DebugInfo.currentPipelineStage = "generatePreBlockWork childRepository.flush()";
//                }
//
//                childRepository.flush();
//
//                AionTxReceipt receipt = transactionSummary.getReceipt();
//                byte[] root = topRepository.getRoot();
//                receipt.setPostTxState(root);
//                receipt.setTransaction(transaction);
//
//                if (DebugInfo.currentBlockNumber == 257159) {
//                    System.out.println("$$$_WORK_gen_$$$ topRepo root hash = " + Hex.toHexString(root));
//                }
//
//                return receipt.getEnergyUsed();
//            } else {
//                return 0;
//            }
//        };
    }

    private AionBlockSummary applyBlock(IAionBlock block) {
        DebugInfo.currentBlockNumber = block.getNumber();
        DebugInfo.isGeneratePreBlock = false;
        DebugInfo.isApplyBlock = true;

        long saveTime = System.nanoTime();

        List<AionTxReceipt> receipts = new ArrayList<>();
        List<AionTxExecSummary> summaries = new ArrayList<>();

        ExecutionBatch batch = new ExecutionBatch(block, block.getTransactionsList());
        BulkExecutor executor =
                new BulkExecutor(
                        batch,
                        repository,
                        track,
                        false,
                        true,
                        block.getNrgLimit(),
                        LOGGER_VM,
                        getPostExecutionWorkForApplyBlock());
        List<AionTxExecSummary> executionSummaries = executor.execute();

        for (AionTxExecSummary summary : executionSummaries) {
            receipts.add(summary.getReceipt());
            summaries.add(summary);
        }

        Map<Address, BigInteger> rewards = addReward(block, summaries);

        long totalTime = System.nanoTime() - saveTime;
        chainStats.addBlockExecTime(totalTime);

        return new AionBlockSummary(block, rewards, receipts, summaries);
    }

    /**
     * Returns a {@link PostExecutionWork} object whose {@code doPostExecutionWork()} method will
     * run the provided logic defined in this method. This work is to be applied after each
     * transaction has been run.
     *
     * <p>This "work" is specific to the {@link AionBlockchainImpl#applyBlock(IAionBlock)} method.
     */
    private static PostExecutionWork getPostExecutionWorkForApplyBlock() {
        return new PostExecutionWork() {
            @Override
            public long doPostExecutionWork(IRepository repository,
                IRepositoryCache<AccountState, IBlockStoreBase<?, ?>> repositoryChild,
                AionTxExecSummary summary, AionTransaction transaction, long blockEnergyRemaining) {

                if (DebugInfo.currentBlockNumber == 257159) {
                    DebugInfo.currentPipelineStage = "applyBlockWork childRepository.flush()";
                }

                repositoryChild.flush();
                AionTxReceipt receipt = summary.getReceipt();
                byte[] root = repository.getRoot();
                receipt.setPostTxState(root);

                if (DebugInfo.currentBlockNumber == 257159) {
                    System.out.println("$$$_WORK_apply_$$$ topRepo root hash = " + Hex.toHexString(root));
                }


                return 0;
            }
        };






//        return (topRepository,
//                childRepository,
//                transactionSummary,
//                transaction,
//                blockEnergyLeft) -> {
//
//            if (DebugInfo.currentBlockNumber == 257159) {
//                DebugInfo.currentPipelineStage = "applyBlockWork childRepository.flush()";
//            }
//
//            childRepository.flush();
//            AionTxReceipt receipt = transactionSummary.getReceipt();
//            byte[] root = topRepository.getRoot();
//            receipt.setPostTxState(root);
//
//            if (DebugInfo.currentBlockNumber == 257159) {
//                System.out.println("$$$_WORK_apply_$$$ topRepo root hash = " + Hex.toHexString(root));
//            }
//
//
//            return 0;
//        };
    }

    /**
     * Add reward to block- and every uncle coinbase assuming the entire block is valid.
     *
     * @param block object containing the header and uncles
     */
    private Map<Address, BigInteger> addReward(
            IAionBlock block, List<AionTxExecSummary> summaries) {

        Map<Address, BigInteger> rewards = new HashMap<>();
        BigInteger minerReward =
                this.chainConfiguration.getRewardsCalculator().calculateReward(block.getHeader());
        rewards.put(block.getCoinbase(), minerReward);

        if (LOG.isTraceEnabled()) {
            LOG.trace(
                    "rewarding: {}np to {} for mining block {}",
                    minerReward,
                    block.getCoinbase(),
                    block.getNumber());
        }

        /*
         * Remaining fees (the ones paid to miners for running transactions) are
         * already paid for at a earlier point in execution.
         */
        track.addBalance(block.getCoinbase(), minerReward);
        track.flush();
        return rewards;
    }

    public ChainConfiguration getChainConfiguration() {
        return chainConfiguration;
    }

    @Override
    public synchronized void storeBlock(AionBlock block, List<AionTxReceipt> receipts) {

        if (fork) {
            getBlockStore().saveBlock(block, totalDifficulty, false);
        } else {
            getBlockStore().saveBlock(block, totalDifficulty, true);
        }

        for (int i = 0; i < receipts.size(); i++) {
            transactionStore.putToBatch(new AionTxInfo(receipts.get(i), block.getHash(), i));
        }
        transactionStore.flushBatch();

        repository.commitBlock(block.getHeader());

        if (LOG.isDebugEnabled())
            LOG.debug(
                    "Block saved: number: {}, hash: {}, TD: {}",
                    block.getNumber(),
                    block.getShortHash(),
                    totalDifficulty);

        if (LOG.isDebugEnabled()) {
            LOG.debug("block added to the blockChain: index: [{}]", block.getNumber());
        }

        setBestBlock(block);
    }

    @Override
    public boolean storePendingStatusBlock(AionBlock block) {
        try {
            return repository.getPendingBlockStore().addStatusBlock(block);
        } catch (Exception e) {
            LOG.error("Unable to store status block in " + repository.toString() + " due to: ", e);
            return false;
        }
    }

    @Override
    public int storePendingBlockRange(List<AionBlock> blocks) {
        try {
            return repository.getPendingBlockStore().addBlockRange(blocks);
        } catch (Exception e) {
            LOG.error(
                    "Unable to store range of blocks in " + repository.toString() + " due to: ", e);
            return 0;
        }
    }

    @Override
    public Map<ByteArrayWrapper, List<AionBlock>> loadPendingBlocksAtLevel(long level) {
        try {
            return repository.getPendingBlockStore().loadBlockRange(level);
        } catch (Exception e) {
            LOG.error(
                    "Unable to retrieve stored blocks from " + repository.toString() + " due to: ",
                    e);
            return Collections.emptyMap();
        }
    }

    @Override
    public long nextBase(long current, long knownStatus) {
        try {
            return repository.getPendingBlockStore().nextBase(current, knownStatus);
        } catch (Exception e) {
            LOG.error("Unable to generate next LIGHTNING request base due to: ", e);
            return current;
        }
    }

    @Override
    public void dropImported(
            long level,
            List<ByteArrayWrapper> ranges,
            Map<ByteArrayWrapper, List<AionBlock>> blocks) {
        try {
            repository.getPendingBlockStore().dropPendingQueues(level, ranges, blocks);
        } catch (Exception e) {
            LOG.error(
                    "Unable to delete used blocks from " + repository.toString() + " due to: ", e);
            return;
        }
    }

    public boolean hasParentOnTheChain(AionBlock block) {
        return getParent(block.getHeader()) != null;
    }

    public TransactionStore<AionTransaction, AionTxReceipt, AionTxInfo> getTransactionStore() {
        return transactionStore;
    }

    @Override
    public synchronized void setBestBlock(AionBlock block) {
        bestBlock = block;
        updateBestKnownBlock(block);
        bestBlockNumber.set(bestBlock.getNumber());
    }

    @Override
    public AionBlock getBestBlock() {
        return pubBestBlock == null ? bestBlock : pubBestBlock;
    }

    @Override
    public synchronized void close() {
        getBlockStore().close();
    }

    @Override
    public BigInteger getTotalDifficulty() {
        return getBestBlock().getCumulativeDifficulty();
    }

    // this method is for the testing purpose
    protected BigInteger getCacheTD() {
        return totalDifficulty;
    }

    private BigInteger getInternalTD() {
        return totalDifficulty;
    }

    private void updateTotalDifficulty(AionBlock block) {
        totalDifficulty = totalDifficulty.add(block.getDifficultyBI());
        block.setCumulativeDifficulty(totalDifficulty);
        if (LOG.isDebugEnabled()) {
            LOG.debug("TD: updated to {}", totalDifficulty);
        }
    }

    @Override
    public void setTotalDifficulty(BigInteger totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
    }

    public void setRepository(AionRepositoryImpl repository) {
        this.repository = repository;
    }

    public void setExitOn(long exitOn) {
        this.exitOn = exitOn;
    }

    @Override
    public Address getMinerCoinbase() {
        return minerCoinbase;
    }

    public boolean isBlockExist(byte[] hash) {
        return getBlockStore().isBlockExist(hash);
    }

    /**
     * Returns up to limit headers found with following search parameters
     *
     * @param blockNumber Identifier of start block, by number
     * @param limit Maximum number of headers in return
     * @return {@link A0BlockHeader}'s list or empty list if none found
     */
    @Override
    public List<A0BlockHeader> getListOfHeadersStartFrom(long blockNumber, int limit) {

        // identifying block we'll move from
        IAionBlock startBlock = getBlockByNumber(blockNumber);

        // if nothing found on main chain, return empty array
        if (startBlock == null) {
            return emptyList();
        }

        List<A0BlockHeader> headers;
        long bestNumber = bestBlock.getNumber();
        headers = getContinuousHeaders(bestNumber, blockNumber, limit);

        return headers;
    }

    /**
     * Finds up to limit blocks starting from blockNumber on main chain
     *
     * @param bestNumber Number of best block
     * @param blockNumber Number of block to start search (included in return)
     * @param limit Maximum number of headers in response
     * @return headers found by query or empty list if none
     */
    private List<A0BlockHeader> getContinuousHeaders(long bestNumber, long blockNumber, int limit) {
        int qty = getQty(blockNumber, bestNumber, limit);

        byte[] startHash = getStartHash(blockNumber, qty);

        if (startHash == null) {
            return emptyList();
        }

        List<A0BlockHeader> headers = getBlockStore().getListHeadersEndWith(startHash, qty);

        // blocks come with decreasing numbers
        Collections.reverse(headers);

        return headers;
    }

    private int getQty(long blockNumber, long bestNumber, int limit) {
        if (blockNumber + limit - 1 > bestNumber) {
            return (int) (bestNumber - blockNumber + 1);
        } else {
            return limit;
        }
    }

    private byte[] getStartHash(long blockNumber, int qty) {

        long startNumber;

        startNumber = blockNumber + qty - 1;

        IAionBlock block = getBlockByNumber(startNumber);

        if (block == null) {
            return null;
        }

        return block.getHash();
    }

    // NOTE: Functionality removed because not used and untested
    //    /**
    //     * Returns up to limit headers found with following search parameters
    //     *
    //     * @param identifier
    //     *            Identifier of start block, by number of by hash
    //     * @param skip
    //     *            Number of blocks to skip between consecutive headers
    //     * @param limit
    //     *            Maximum number of headers in return
    //     * @param reverse
    //     *            Is search reverse or not
    //     * @return {@link A0BlockHeader}'s list or empty list if none found
    //     */
    //    @Override
    //    public List<A0BlockHeader> getListOfHeadersStartFrom(BlockIdentifier identifier, int skip,
    // int limit,
    //            boolean reverse) {
    //
    //        // null identifier check
    //        if (identifier == null){
    //            return emptyList();
    //        }
    //
    //        // Identifying block we'll move from
    //        IAionBlock startBlock;
    //        if (identifier.getHash() != null) {
    //            startBlock = getBlockByHash(identifier.getHash());
    //        } else {
    //            startBlock = getBlockByNumber(identifier.getNumber());
    //        }
    //
    //        // If nothing found or provided hash is not on main chain, return empty
    //        // array
    //        if (startBlock == null) {
    //            return emptyList();
    //        }
    //        if (identifier.getHash() != null) {
    //            IAionBlock mainChainBlock = getBlockByNumber(startBlock.getNumber());
    //            if (!startBlock.equals(mainChainBlock)) {
    //                return emptyList();
    //            }
    //        }
    //
    //        List<A0BlockHeader> headers;
    //        if (skip == 0) {
    //            long bestNumber = bestBlock.getNumber();
    //            headers = getContinuousHeaders(bestNumber, startBlock.getNumber(), limit,
    // reverse);
    //        } else {
    //            headers = getGapedHeaders(startBlock, skip, limit, reverse);
    //        }
    //
    //        return headers;
    //    }
    //
    //    /**
    //     * Finds up to limit blocks starting from blockNumber on main chain
    //     *
    //     * @param bestNumber
    //     *            Number of best block
    //     * @param blockNumber
    //     *            Number of block to start search (included in return)
    //     * @param limit
    //     *            Maximum number of headers in response
    //     * @param reverse
    //     *            Order of search
    //     * @return headers found by query or empty list if none
    //     */
    //    private List<A0BlockHeader> getContinuousHeaders(long bestNumber, long blockNumber, int
    // limit, boolean reverse) {
    //        int qty = getQty(blockNumber, bestNumber, limit, reverse);
    //
    //        byte[] startHash = getStartHash(blockNumber, qty, reverse);
    //
    //        if (startHash == null) {
    //            return emptyList();
    //        }
    //
    //        List<A0BlockHeader> headers = getBlockStore().getListHeadersEndWith(startHash, qty);
    //
    //        // blocks come with falling numbers
    //        if (!reverse) {
    //            Collections.reverse(headers);
    //        }
    //
    //        return headers;
    //    }
    //
    //    /**
    //     * Gets blocks from main chain with gaps between
    //     *
    //     * @param startBlock
    //     *            Block to start from (included in return)
    //     * @param skip
    //     *            Number of blocks skipped between every header in return
    //     * @param limit
    //     *            Maximum number of headers in return
    //     * @param reverse
    //     *            Order of search
    //     * @return headers found by query or empty list if none
    //     */
    //    private List<A0BlockHeader> getGapedHeaders(IAionBlock startBlock, int skip, int limit,
    // boolean reverse) {
    //        List<A0BlockHeader> headers = new ArrayList<>();
    //        headers.add(startBlock.getHeader());
    //        int offset = skip + 1;
    //        if (reverse) {
    //            offset = -offset;
    //        }
    //        long currentNumber = startBlock.getNumber();
    //        boolean finished = false;
    //
    //        while (!finished && headers.size() < limit) {
    //            currentNumber += offset;
    //            IAionBlock nextBlock = getBlockStore().getChainBlockByNumber(currentNumber);
    //            if (nextBlock == null) {
    //                finished = true;
    //            } else {
    //                headers.add(nextBlock.getHeader());
    //            }
    //        }
    //
    //        return headers;
    //    }
    //
    //
    //    private int getQty(long blockNumber, long bestNumber, int limit, boolean reverse) {
    //        if (reverse) {
    //            return blockNumber - limit + 1 < 0 ? (int) (blockNumber + 1) : limit;
    //        } else {
    //            if (blockNumber + limit - 1 > bestNumber) {
    //                return (int) (bestNumber - blockNumber + 1);
    //            } else {
    //                return limit;
    //            }
    //        }
    //    }
    //
    //    private byte[] getStartHash(long blockNumber, int qty, boolean reverse) {
    //
    //        long startNumber;
    //
    //        if (reverse) {
    //            startNumber = blockNumber;
    //        } else {
    //            startNumber = blockNumber + qty - 1;
    //        }
    //
    //        IAionBlock block = getBlockByNumber(startNumber);
    //
    //        if (block == null) {
    //            return null;
    //        }
    //
    //        return block.getHash();
    //    }

    @Override
    public List<byte[]> getListOfBodiesByHashes(List<byte[]> hashes) {
        List<byte[]> bodies = new ArrayList<>(hashes.size());

        for (byte[] hash : hashes) {
            AionBlock block = getBlockStore().getBlockByHash(hash);
            if (block == null) {
                break;
            }
            bodies.add(block.getEncodedBody());
        }

        return bodies;
    }

    private class State {

        AionRepositoryImpl savedRepo = repository;
        AionBlock savedBest = bestBlock;
        BigInteger savedTD = totalDifficulty;
    }

    private void updateBestKnownBlock(AionBlock block) {
        updateBestKnownBlock(block.getHeader());
    }

    private void updateBestKnownBlock(A0BlockHeader header) {
        if (bestKnownBlock.get() == null || header.getNumber() > bestKnownBlock.get().getNumber()) {
            bestKnownBlock.set(new BlockIdentifier(header.getHash(), header.getNumber()));
        }
    }

    public IEventMgr getEventMgr() {
        return this.evtMgr;
    }

    @Override
    public synchronized boolean recoverWorldState(IRepository repository, AionBlock block) {
        if (block == null) {
            LOG.error("World state recovery attempted with null block.");
            return false;
        }
        if (repository.isSnapshot()) {
            LOG.error("World state recovery attempted with snapshot repository.");
            return false;
        }

        long blockNumber = block.getNumber();
        LOG.info(
                "Pruned or corrupt world state at block hash: {}, number: {}."
                        + " Looking for ancestor block with valid world state ...",
                block.getShortHash(),
                blockNumber);

        AionRepositoryImpl repo = (AionRepositoryImpl) repository;

        // keeping track of the original root
        byte[] originalRoot = repo.getRoot();

        Deque<AionBlock> dirtyBlocks = new ArrayDeque<>();
        // already known to be missing the state
        dirtyBlocks.push(block);

        AionBlock other = block;

        // find all the blocks missing a world state
        do {
            other = repo.getBlockStore().getBlockByHash(other.getParentHash());

            // cannot recover if no valid states exist (must build from genesis)
            if (other == null) {
                return false;
            } else {
                dirtyBlocks.push(other);
            }
        } while (!repo.isValidRoot(other.getStateRoot()) && other.getNumber() > 0);

        if (other.getNumber() == 0 && !repo.isValidRoot(other.getStateRoot())) {
            LOG.info("Rebuild state FAILED because a valid state could not be found.");
            return false;
        }

        // sync to the last correct state
        repo.syncToRoot(other.getStateRoot());

        // remove the last added block because it has a correct world state
        dirtyBlocks.pop();

        LOG.info(
                "Valid state found at block hash: {}, number: {}.",
                other.getShortHash(),
                other.getNumber());

        // rebuild world state for dirty blocks
        while (!dirtyBlocks.isEmpty()) {
            other = dirtyBlocks.pop();
            LOG.info(
                    "Rebuilding block hash: {}, number: {}, txs: {}.",
                    other.getShortHash(),
                    other.getNumber(),
                    other.getTransactionsList().size());
            this.add(other, true);
        }

        // update the repository
        repo.flush();

        // setting the root back to its correct value
        repo.syncToRoot(originalRoot);

        // return a flag indicating if the recovery worked
        return repo.isValidRoot(block.getStateRoot());
    }

    @Override
    public synchronized boolean recoverIndexEntry(IRepository repository, AionBlock block) {
        if (block == null) {
            LOG.error("Index recovery attempted with null block.");
            return false;
        }
        if (repository.isSnapshot()) {
            LOG.error("Index recovery attempted with snapshot repository.");
            return false;
        }

        LOG.info(
                "Missing index at block hash: {}, number: {}. Looking for ancestor block with valid index ...",
                block.getShortHash(),
                block.getNumber());

        AionRepositoryImpl repo = (AionRepositoryImpl) repository;

        Deque<AionBlock> dirtyBlocks = new ArrayDeque<>();
        // already known to be missing the state
        dirtyBlocks.push(block);

        AionBlock other = block;

        // find all the blocks missing a world state
        do {
            other = repo.getBlockStore().getBlockByHash(other.getParentHash());

            // cannot recover if no valid states exist (must build from genesis)
            if (other == null) {
                return false;
            } else {
                dirtyBlocks.push(other);
            }
        } while (!repo.isIndexed(other.getHash(), other.getNumber()) && other.getNumber() > 0);

        if (other.getNumber() == 0 && !repo.isIndexed(other.getHash(), other.getNumber())) {
            LOG.info("Rebuild index FAILED because a valid index could not be found.");
            return false;
        }

        // if the size key is missing we set it to the MAX(best block, this block, current value)
        long maxNumber = getBlockStore().getMaxNumber();
        if (bestBlock != null && bestBlock.getNumber() > maxNumber) {
            maxNumber = bestBlock.getNumber();
        }
        if (block.getNumber() > maxNumber) {
            maxNumber = bestBlock.getNumber();
        }
        getBlockStore().correctSize(maxNumber, LOG);

        // remove the last added block because it has a correct world state
        BigInteger totalDiff =
                getBlockStore().getTotalDifficultyForHash(dirtyBlocks.pop().getHash());

        LOG.info(
                "Valid index found at block hash: {}, number: {}.",
                other.getShortHash(),
                other.getNumber());

        // rebuild world state for dirty blocks
        while (!dirtyBlocks.isEmpty()) {
            other = dirtyBlocks.pop();
            LOG.info(
                    "Rebuilding index for block hash: {}, number: {}, txs: {}.",
                    other.getShortHash(),
                    other.getNumber(),
                    other.getTransactionsList().size());
            totalDiff = repo.getBlockStore().correctIndexEntry(other, totalDiff);
        }

        // update the repository
        repo.flush();

        // return a flag indicating if the recovery worked
        if (repo.isIndexed(block.getHash(), block.getNumber())) {
            AionBlock mainChain = getBlockStore().getBestBlock();
            BigInteger mainChainTotalDiff =
                    getBlockStore().getTotalDifficultyForHash(mainChain.getHash());

            // check if the main chain needs to be updated
            if (mainChainTotalDiff.compareTo(totalDiff) < 0) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(
                            "branching: from = {}/{}, to = {}/{}",
                            mainChain.getNumber(),
                            toHexString(mainChain.getHash()),
                            block.getNumber(),
                            toHexString(block.getHash()));
                }
                getBlockStore().reBranch(block);
                repo.syncToRoot(block.getStateRoot());
                repo.flush();
            } else {
                if (mainChain.getNumber() > block.getNumber()) {
                    // checking if the current recovered blocks are a subsection of the main chain
                    AionBlock ancestor = getBlockByNumber(block.getNumber() + 1);
                    if (ancestor != null
                            && FastByteComparisons.equal(
                                    ancestor.getParentHash(), block.getHash())) {
                        getBlockStore().correctMainChain(block, LOG);
                        repo.flush();
                    }
                }
            }
            return true;
        } else {
            LOG.info("Rebuild index FAILED.");
            return false;
        }
    }

    @Override
    public BigInteger getTotalDifficultyByHash(Hash256 hash) {
        if (hash == null) {
            throw new NullPointerException();
        }
        return this.getBlockStore().getTotalDifficultyForHash(hash.toBytes());
    }
}
