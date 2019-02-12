package org.aion.zero.impl.sync;

import static org.aion.p2p.V1Constants.CONTRACT_MISSING_KEYS_LIMIT;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.mcf.core.AccountState;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.impl.AionBlockchainImpl;
import org.aion.zero.impl.types.AionBlock;

/**
 * Directs behavior for fast sync functionality.
 *
 * @author Alexandra Roatis
 */
public final class FastSyncManager {

    private boolean enabled;

    private final Map<ByteArrayWrapper, byte[]> importedTrieNodes = new ConcurrentHashMap<>();

    private final BlockingQueue<ByteArrayWrapper> requiredStateNodes = new LinkedBlockingQueue<>();
    private final BlockingQueue<ByteArrayWrapper> requiredStorage = new LinkedBlockingQueue<>();

    private AionBlock pivot = null;
    private long pivotNumber = -1;

    private final AionBlockchainImpl chain;

    public FastSyncManager() {
        this.enabled = false;
        this.chain = null;
    }

    public FastSyncManager(AionBlockchainImpl chain) {
        this.enabled = true;
        this.chain = chain;
    }

    public void addImportedNode(ByteArrayWrapper key, byte[] value) {
        if (enabled) {
            importedTrieNodes.put(key, value);
        }
    }

    public boolean containsExact(ByteArrayWrapper key, byte[] value) {
        return enabled
                && importedTrieNodes.containsKey(key)
                && Arrays.equals(importedTrieNodes.get(key), value);
    }

    @VisibleForTesting
    void setPivot(AionBlock pivot) {
        Objects.requireNonNull(pivot);

        this.pivot = pivot;
        this.pivotNumber = pivot.getNumber();
    }

    /** Changes the pivot in case of import failure. */
    public void handleFailedImport(
            ByteArrayWrapper key, byte[] value, DatabaseType dbType, int peerId, String peer) {
        if (enabled) {
            // TODO: received incorrect or inconsistent state: change pivot??
            // TODO: consider case where someone purposely sends incorrect values
            // TODO: decide on how far back to move the pivot
        }
    }

    /**
     * Indicates the status of the fast sync process.
     *
     * @return {@code true} when fast sync is complete and secure, {@code false} while trie nodes
     *     are still required or completeness has not been confirmed yet
     */
    public boolean isComplete() {
        // TODO: implement check for completeness
        return false;
    }

    @VisibleForTesting
    boolean isCompleteReceiptTransfer() {
        // TODO: implement
        return false;
    }

    @VisibleForTesting
    boolean isCompleteWorldState() {
        if (pivot == null) {
            return false;
        } else {
            // get root of pivot
            byte[] root = pivot.getStateRoot();

            // traverse trie from root to find missing nodes
            Set<ByteArrayWrapper> missing = chain.traverseTrieFromNode(root, DatabaseType.STATE);

            // clearing the queue to ensure we're not still requesting already received nodes
            requiredStateNodes.clear();

            if (missing.isEmpty()) {
                return true;
            } else {
                requiredStateNodes.addAll(missing);
                return false;
            }
        }
    }

    /**
     * Check if the receipts have been processed and the world state download is complete.
     *
     * @implNote This condition must pass before checking that the download of <i>details</i> and
     *     <i>storage</i> data for each contract is complete.
     * @return {@code true} when all the receipts have been processed and the world state download
     *     is complete, {@code false} otherwise.
     */
    private boolean satisfiesContractRequirements() {
        // to be sure we have all the contract information we need:
        // 1. to check the receipts for all the deployed contracts
        // 2. to check the state for the root of the details for each contract
        return isCompleteReceiptTransfer() && isCompleteWorldState();
    }

    @VisibleForTesting
    boolean isCompleteContractStorage() {
        if (!requiredStorage.isEmpty() || !satisfiesContractRequirements()) {
            // checking all contracts is expensive; to efficiently manage memory we do this check
            // only if all the already known missing values have been requested
            // and the state and receipts parts are complete
            return false;
        } else {
            Iterator<Address> iterator = chain.getContracts();

            // check that each contract has all the required data
            while (iterator.hasNext()) {
                Address contract = iterator.next();
                AccountState contractState = chain.getRepository().getAccountState(contract);

                if (contractState == null) {
                    // determine if the contract was created after the pivot block
                    // this can happen if the pivot was updated due to other issues
                    if (chain.getInceptionBlockNumber(contract) > pivotNumber) {
                        continue;
                    } else {
                        // somehow the world state was incorrectly labeled as complete
                        // this should not happen so switching off sync
                        // TODO: disable fast sync in method that catches this exception
                        throw new IllegalStateException(
                                "Fast sync encountered a error for which there is no defined recovery path. Disabling fast sync.");
                    }
                } else {
                    byte[] root = contractState.getStateRoot();

                    // traverse trie from root to find missing nodes
                    Set<ByteArrayWrapper> missing =
                            chain.traverseTrieFromNode(root, DatabaseType.STORAGE);

                    requiredStorage.addAll(missing);

                    if (requiredStorage.size() >= CONTRACT_MISSING_KEYS_LIMIT) {
                        // to efficiently manage memory: stop checking when reaching the limit
                        return false;
                    }
                }
            }

            return requiredStorage.isEmpty();
        }
    }

    public void updateRequests(ByteArrayWrapper topmostKey, Set<ByteArrayWrapper> referencedKeys) {
        if (enabled) {
            // TODO: check what's still missing and send out requests
            // TODO: send state request to multiple peers
        }
    }
}
