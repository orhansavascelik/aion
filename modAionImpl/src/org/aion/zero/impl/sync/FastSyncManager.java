package org.aion.zero.impl.sync;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.aion.base.util.ByteArrayWrapper;
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

    public void updateRequests(ByteArrayWrapper topmostKey, Set<ByteArrayWrapper> referencedKeys) {
        if (enabled) {
            // TODO: check what's still missing and send out requests
            // TODO: send state request to multiple peers
        }
    }
}
