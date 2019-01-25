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

package org.aion.zero.db;

import static org.aion.base.util.ByteArrayWrapper.wrap;
import static org.aion.base.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.aion.crypto.HashUtil.EMPTY_TRIE_HASH;
import static org.aion.crypto.HashUtil.h256;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.aion.base.db.IByteArrayKeyValueStore;
import org.aion.base.db.IContractDetails;
import org.aion.base.type.AionAddress;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.base.util.ByteUtil;
import org.aion.base.vm.DebugInfo;
import org.aion.mcf.db.AbstractContractDetails;
import org.aion.mcf.ds.XorDataSource;
import org.aion.mcf.trie.SecureTrie;
import org.aion.mcf.vm.types.DataWord;
import org.aion.mcf.vm.types.DoubleDataWord;
import org.aion.rlp.RLP;
import org.aion.rlp.RLPElement;
import org.aion.rlp.RLPItem;
import org.aion.rlp.RLPList;
import org.aion.util.conversions.Hex;
import org.aion.vm.api.interfaces.Address;

public class AionContractDetailsImpl extends AbstractContractDetails {

    private IByteArrayKeyValueStore dataSource;

    private byte[] rlpEncoded;

    private Address address = AionAddress.EMPTY_ADDRESS();

    private SecureTrie storageTrie = new SecureTrie(null);

    public boolean externalStorage;
    private IByteArrayKeyValueStore externalStorageDataSource;

    private static int globalCount = 0;
    public int id = -1;

    public AionContractDetailsImpl() {
        id = globalCount;
        globalCount++;
    }

    public AionContractDetailsImpl(int prune, int memStorageLimit) {
        super(prune, memStorageLimit);
        id = globalCount;
        globalCount++;
    }

    private AionContractDetailsImpl(
            Address address, SecureTrie storageTrie, Map<ByteArrayWrapper, byte[]> codes) {
        this.address = address;
        this.storageTrie = storageTrie;
        setCodes(codes);
        id = globalCount;
        globalCount++;
    }

    public AionContractDetailsImpl(byte[] code) throws Exception {
        if (code == null) {
            throw new Exception("Empty input code");
        }

        decode(code);
        id = globalCount;
        globalCount++;
    }

    /**
     * Adds the key-value pair to the database unless value is an ByteArrayWrapper whose underlying
     * byte array consists only of zeros. In this case, if key already exists in the database it
     * will be deleted.
     *
     * @param key The key.
     * @param value The value.
     */
    @Override
    public void put(ByteArrayWrapper key, ByteArrayWrapper value) {
        // We strip leading zeros of a DataWord but not a DoubleDataWord so that when we call get
        // we can differentiate between the two.

        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        if (value.isZero()) {
            // TODO: remove when integrating the AVM
            // used to ensure FVM correctness
            throw new IllegalArgumentException(
                    "Put with zero values is not allowed for the FVM. Explicit call to delete is necessary.");
        }

        byte[] data = RLP.encodeElement(value.getData());
        storageTrie.update(key.getData(), data);

        this.setDirty(true);
        this.rlpEncoded = null;
    }

    @Override
    public void delete(ByteArrayWrapper key) {
        storageTrie.delete(key.getData());

        this.setDirty(true);
        this.rlpEncoded = null;
    }

    /**
     * Returns the value associated with key if it exists, otherwise returns a DataWord consisting
     * entirely of zero bytes.
     *
     * @param key The key to query.
     * @return the corresponding value or a zero-byte DataWord if no such value.
     */
    @Override
    public ByteArrayWrapper get(ByteArrayWrapper key) {
        byte[] data = storageTrie.get(key.getData());

        if (RLP.decode2(data).size() == 0) {
            return null;
        } else {
            return new ByteArrayWrapper(RLP.decode2(data).get(0).getRLPData());
        }
    }

    /**
     * Returns the storage hash.
     *
     * @return the storage hash.
     */
    @Override
    public byte[] getStorageHash() {
        return storageTrie.getRootHash();
    }

    /**
     * Decodes an AionContractDetailsImpl object from the RLP encoding rlpCode.
     *
     * @param rlpCode The encoding to decode.
     */
    @Override
    public void decode(byte[] rlpCode) {
        RLPList data = RLP.decode2(rlpCode);
        RLPList rlpList = (RLPList) data.get(0);

        RLPItem address = (RLPItem) rlpList.get(0);
        RLPItem isExternalStorage = (RLPItem) rlpList.get(1);
        RLPItem storageRoot = (RLPItem) rlpList.get(2);
        RLPItem storage = (RLPItem) rlpList.get(3);
        RLPElement code = rlpList.get(4);

        if (address.getRLPData() == null) {
            this.address = AionAddress.EMPTY_ADDRESS();
        } else {
            this.address = AionAddress.wrap(address.getRLPData());
        }

        if (code instanceof RLPList) {
            for (RLPElement e : ((RLPList) code)) {
                setCode(e.getRLPData());
            }
        } else {
            setCode(code.getRLPData());
        }

        // load/deserialize storage trie
        this.externalStorage = !Arrays.equals(isExternalStorage.getRLPData(), EMPTY_BYTE_ARRAY);
        if (externalStorage) {
            storageTrie = new SecureTrie(getExternalStorageDataSource(), storageRoot.getRLPData());
        } else {
            storageTrie.deserialize(storage.getRLPData());
        }
        storageTrie.withPruningEnabled(prune > 0);

        // switch from in-memory to external storage
        if (!externalStorage && storage.getRLPData().length > detailsInMemoryStorageLimit) {
            externalStorage = true;
            storageTrie.getCache().setDB(getExternalStorageDataSource());
        }

        this.rlpEncoded = rlpCode;
    }

    /**
     * Returns an rlp encoding of this AionContractDetailsImpl object.
     *
     * @return an rlp encoding of this.
     */
    @Override
    public byte[] getEncoded() {
        if (DebugInfo.currentBlockNumber == 257159) {
            String stage = (DebugInfo.isGeneratePreBlock) ? "gen" : "apply";
            System.out.println(
                    "$$$_ACDI_" + stage + "_$$$ Encoding: "
                            + address
                            + " "
                            + externalStorage
                            + " "
                            + Hex.toHexString(storageTrie.getRootHash())
                            + " "
                            + getCodes().size()
                            + " ");
            System.out.print("[codes = ");
            for (byte[] codes : getCodes().values()) {
                System.out.print(Hex.toHexString(codes) + " ");
            }
        }

        if (rlpEncoded == null) {

            byte[] rlpAddress = RLP.encodeElement(address.toBytes());
            byte[] rlpIsExternalStorage = RLP.encodeByte((byte) (externalStorage ? 1 : 0));
            byte[] rlpStorageRoot =
                    RLP.encodeElement(
                            externalStorage ? storageTrie.getRootHash() : EMPTY_BYTE_ARRAY);
            byte[] rlpStorage =
                    RLP.encodeElement(externalStorage ? EMPTY_BYTE_ARRAY : storageTrie.serialize());
            byte[][] codes = new byte[getCodes().size()][];
            int i = 0;
            for (byte[] bytes : this.getCodes().values()) {
                codes[i++] = RLP.encodeElement(bytes);
            }
            byte[] rlpCode = RLP.encodeList(codes);

            this.rlpEncoded =
                    RLP.encodeList(
                            rlpAddress, rlpIsExternalStorage, rlpStorageRoot, rlpStorage, rlpCode);
        }

        return rlpEncoded;
    }

    /**
     * Get the address associated with this AionContractDetailsImpl.
     *
     * @return the associated address.
     */
    @Override
    public Address getAddress() {
        return address;
    }

    /**
     * Sets the associated address to address.
     *
     * @param address The address to set.
     */
    @Override
    public void setAddress(Address address) {
        this.address = address;
        this.rlpEncoded = null;
    }

    /** Syncs the storage trie. */
    @Override
    public void syncStorage() {
        if (externalStorage) {
            storageTrie.sync();
        }
    }

    /**
     * Sets the data source to dataSource.
     *
     * @param dataSource The new dataSource.
     */
    public void setDataSource(IByteArrayKeyValueStore dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns the external storage data source.
     *
     * @return the external storage data source.
     */
    private IByteArrayKeyValueStore getExternalStorageDataSource() {
        if (externalStorageDataSource == null) {
            externalStorageDataSource =
                    new XorDataSource(
                            dataSource, h256(("details-storage/" + address.toString()).getBytes()));
        }
        return externalStorageDataSource;
    }

    /**
     * Sets the external storage data source to dataSource.
     *
     * @param dataSource The new data source.
     */
    public void setExternalStorageDataSource(IByteArrayKeyValueStore dataSource) {
        this.externalStorageDataSource = dataSource;
        this.externalStorage = true;
        this.storageTrie = new SecureTrie(getExternalStorageDataSource());
    }

    /**
     * Returns an AionContractDetailsImpl object pertaining to a specific point in time given by the
     * root hash hash.
     *
     * @param hash The root hash to search for.
     * @return the specified AionContractDetailsImpl.
     */
    @Override
    public IContractDetails getSnapshotTo(byte[] hash) {

        IByteArrayKeyValueStore keyValueDataSource = this.storageTrie.getCache().getDb();

        SecureTrie snapStorage =
                wrap(hash).equals(wrap(EMPTY_TRIE_HASH))
                        ? new SecureTrie(keyValueDataSource, "".getBytes())
                        : new SecureTrie(keyValueDataSource, hash);
        snapStorage.withPruningEnabled(storageTrie.isPruningEnabled());

        snapStorage.setCache(this.storageTrie.getCache());

        AionContractDetailsImpl details =
                new AionContractDetailsImpl(this.address, snapStorage, getCodes());
        details.externalStorage = this.externalStorage;
        details.externalStorageDataSource = this.externalStorageDataSource;
        details.dataSource = dataSource;

        return details;
    }

    /** {@inheritDoc} */
    @Override
    public AionContractDetailsImpl copy() {
        AionContractDetailsImpl detailsCopy = new AionContractDetailsImpl();
        detailsCopy.dataSource = (this.dataSource == null) ? null : this.dataSource.copy();
        detailsCopy.rlpEncoded = (this.rlpEncoded == null) ? null : Arrays.copyOf(this.rlpEncoded, this.rlpEncoded.length);
        detailsCopy.address = (this.address == null) ? null : new AionAddress(this.address.toBytes());
        detailsCopy.storageTrie = (this.storageTrie == null) ? null : this.storageTrie.copy();
        detailsCopy.externalStorage = this.externalStorage;
        detailsCopy.externalStorageDataSource = this.externalStorageDataSource.copy();

        detailsCopy.id = globalCount;
        globalCount++;

        detailsCopy.setCodes(deepCopyOfCodes());
        detailsCopy.setDirty(this.isDirty());
        detailsCopy.setDeleted(this.isDeleted());
        detailsCopy.prune = this.prune;
        detailsCopy.detailsInMemoryStorageLimit = this.detailsInMemoryStorageLimit;
        return detailsCopy;
    }

    private Map<ByteArrayWrapper, byte[]> deepCopyOfCodes() {
        Map<ByteArrayWrapper, byte[]> actualCodes = this.getCodes();

        if (actualCodes == null) {
            return null;
        }

        Map<ByteArrayWrapper, byte[]> copyCodes = new HashMap<>();
        for (Entry<ByteArrayWrapper, byte[]> codeEntry : actualCodes.entrySet()) {
            byte[] bytesOfKey = codeEntry.getKey().getData();
            byte[] bytesOfValue = codeEntry.getValue();
            copyCodes.put(new ByteArrayWrapper(Arrays.copyOf(bytesOfKey, bytesOfKey.length)), Arrays.copyOf(bytesOfValue, bytesOfValue.length));
        }
        return copyCodes;
    }
}
