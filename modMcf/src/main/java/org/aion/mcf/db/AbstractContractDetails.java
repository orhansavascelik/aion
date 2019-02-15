package org.aion.mcf.db;

import static org.aion.crypto.HashUtil.EMPTY_DATA_HASH;
import static org.aion.crypto.HashUtil.h256;
import static org.aion.util.bytes.ByteUtil.EMPTY_BYTE_ARRAY;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.aion.type.ByteArrayWrapper;
import org.aion.type.api.interfaces.common.Wrapper;
import org.aion.type.api.interfaces.db.ContractDetails;
import org.aion.util.conversions.Hex;

/** Abstract contract details. */
public abstract class AbstractContractDetails implements ContractDetails {

    private boolean dirty = false;
    private boolean deleted = false;

    protected int prune;
    protected int detailsInMemoryStorageLimit;

    private Map<Wrapper, byte[]> codes = new HashMap<>();

    protected AbstractContractDetails() {
        this(0, 64 * 1024);
    }

    protected AbstractContractDetails(int prune, int memStorageLimit) {
        this.prune = prune;
        this.detailsInMemoryStorageLimit = memStorageLimit;
    }

    @Override
    public byte[] getCode() {
        return codes.size() == 0 ? EMPTY_BYTE_ARRAY : codes.values().iterator().next();
    }

    @Override
    public byte[] getCode(byte[] codeHash) {
        if (java.util.Arrays.equals(codeHash, EMPTY_DATA_HASH)) {
            return EMPTY_BYTE_ARRAY;
        }
        byte[] code = codes.get(new ByteArrayWrapper(codeHash));
        return code == null ? EMPTY_BYTE_ARRAY : code;
    }

    @Override
    public void setCode(byte[] code) {
        if (code == null) {
            return;
        }
        try {
            codes.put(ByteArrayWrapper.wrap(h256(code)), code);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        setDirty(true);
    }

    public Map<Wrapper, byte[]> getCodes() {
        return codes;
    }

    protected void setCodes(Map<Wrapper, byte[]> codes) {
        this.codes = new HashMap<>(codes);
    }

    public void appendCodes(Map<Wrapper, byte[]> codes) {
        this.codes.putAll(codes);
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public String toString() {
        String ret;

        if (codes != null) {
            ret =
                    "  Code: "
                            + (codes.size() < 2
                                    ? Hex.toHexString(getCode())
                                    : codes.size() + " versions")
                            + "\n";
        } else {
            ret = "  Code: null\n";
        }

        byte[] storage = getStorageHash();
        if (storage != null) {
            ret += "  Storage: " + Hex.toHexString(storage);
        } else {
            ret += "  Storage: null";
        }

        return ret;
    }

    @VisibleForTesting
    @Override
    public void setStorage(Map<Wrapper, Wrapper> storage) {
        for (Map.Entry<Wrapper, Wrapper> entry : storage.entrySet()) {
            Wrapper key = entry.getKey();
            Wrapper value = entry.getValue();

            if (value != null) {
                put(key, value);
            } else {
                delete(key);
            }
        }
    }

    @Override
    public Map<Wrapper, Wrapper> getStorage(Collection<Wrapper> keys) {
        Map<Wrapper, Wrapper> storage = new HashMap<>();

        if (keys == null) {
            throw new IllegalArgumentException("Input keys cannot be null");
        } else {
            for (Wrapper key : keys) {
                Wrapper value = get(key);

                // we check if the value is not null,
                // cause we keep all historical keys
                if (value != null) {
                    storage.put(key, value);
                }
            }
        }

        return storage;
    }
}
