package org.aion.mcf.ds;

import java.io.Closeable;
import java.util.Optional;
import org.aion.interfaces.db.Flushable;
import org.aion.util.bytes.ByteUtil;
import org.aion.util.conversions.Hex;

/**
 * DataSource Array.
 *
 * @param <V>
 */
public class DataSourceArray<V> implements Flushable, Closeable {

    private final ObjectDataSource<V> src;
    private static final byte[] sizeKey = Hex.decode("FFFFFFFFFFFFFFFF");
    private long size = -1L;

    public DataSourceArray(ObjectDataSource<V> src) {
        this.src = src;
    }

    @Override
    public void flush() {
        src.flush();
    }

    public V set(long index, V value) {
        if (index <= Integer.MAX_VALUE) {
            src.put(ByteUtil.intToBytes((int) index), value);
        } else {
            src.put(ByteUtil.longToBytes(index), value);
        }
        if (index >= size()) {
            setSize(index + 1);
        }
        return value;
    }

    public void remove(long index) {
        // without this check it will remove the sizeKey
        if (index < 0 || index >= size()) {
            return;
        }

        if (index <= Integer.MAX_VALUE) {
            src.delete(ByteUtil.intToBytes((int) index));
        } else {
            src.delete(ByteUtil.longToBytes(index));
        }
        if (index < size()) {
            setSize(index);
        }
    }

    public V get(long index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException(
                    "Incorrect index value <"
                            + index
                            + ">. Allowed values are >= 0 and < "
                            + size
                            + ".");
        }

        V value;

        if (index <= Integer.MAX_VALUE) {
            value = src.get(ByteUtil.intToBytes((int) index));
        } else {
            value = src.get(ByteUtil.longToBytes(index));
        }
        return value;
    }

    public long getStoredSize() {
        long size;

        // Read the value from the database directly and
        // convert to the size, and if it doesn't exist, 0.
        Optional<byte[]> optBytes = src.getSrc().get(sizeKey);
        if (!optBytes.isPresent()) {
            size = 0L;
        } else {
            byte[] bytes = optBytes.get();

            if (bytes.length == 4) {
                size = (long) ByteUtil.byteArrayToInt(bytes);
            } else {
                size = ByteUtil.byteArrayToLong(bytes);
            }
        }

        return size;
    }

    public long size() {

        if (size < 0) {
            size = getStoredSize();
        }

        return size;
    }

    private synchronized void setSize(long newSize) {
        size = newSize;
        if (size <= Integer.MAX_VALUE) {
            src.getSrc().put(sizeKey, ByteUtil.intToBytes((int) newSize));
        } else {
            src.getSrc().put(sizeKey, ByteUtil.longToBytes(newSize));
        }
    }

    @Override
    public void close() {
        src.close();
    }
}
