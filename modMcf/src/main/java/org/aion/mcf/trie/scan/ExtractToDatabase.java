package org.aion.mcf.trie.scan;

import org.aion.type.api.interfaces.db.ByteArrayKeyValueDatabase;
import org.aion.rlp.Value;

/** @author Alexandra Roatis */
public class ExtractToDatabase implements ScanAction {

    // only the keys are relevant so the value will be this constant
    private byte[] dummy_value = new byte[] {0};
    private ByteArrayKeyValueDatabase db;
    public long count = 0;

    public ExtractToDatabase(ByteArrayKeyValueDatabase _db) {
        this.db = _db;
    }

    @Override
    public void doOnNode(byte[] hash, Value node) {
        db.put(hash, dummy_value);
        count++;
    }
}
