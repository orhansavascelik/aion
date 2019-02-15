package org.aion.mcf.trie.scan;

import java.util.HashSet;
import java.util.Set;
import org.aion.rlp.Value;
import org.aion.type.ByteArrayWrapper;
import org.aion.type.api.interfaces.common.Wrapper;

public class CollectFullSetOfNodes implements ScanAction {
    private Set<Wrapper> nodes = new HashSet<>();

    @Override
    public void doOnNode(byte[] hash, Value node) {
        nodes.add(new ByteArrayWrapper(hash));
    }

    public Set<Wrapper> getCollectedHashes() {
        return nodes;
    }
}
