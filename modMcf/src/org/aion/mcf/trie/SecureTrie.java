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
 *     The aion network project leverages useful source code from other
 *     open source projects. We greatly appreciate the effort that was
 *     invested in these projects and we thank the individual contributors
 *     for their work. For provenance information and contributors
 *     please see <https://github.com/aionnetwork/aion/wiki/Contributors>.
 *
 * Contributors to the aion source files in decreasing order of code volume:
 *     Aion foundation.
 *     <ether.camp> team through the ethereumJ library.
 *     Ether.Camp Inc. (US) team through Ethereum Harmony.
 *     John Tromp through the Equihash solver.
 *     Samuel Neves through the BLAKE2 implementation.
 *     Zcash project team.
 *     Bitcoinj team.
 */

package org.aion.mcf.trie;

import static org.aion.crypto.HashUtil.h256;

import java.util.Arrays;
import org.aion.base.db.IByteArrayKeyValueStore;

public class SecureTrie extends TrieImpl implements Trie {

    /**
     * A private constructor that initializes the db and root to null.
     *
     * Used by this class's copy() method.
     */
    private SecureTrie() {
        super(null, null);
    }

    public SecureTrie(IByteArrayKeyValueStore db) {
        this(db, "");
    }

    public SecureTrie(IByteArrayKeyValueStore db, Object root) {
        super(db, root);
    }

    @Override
    public byte[] get(byte[] key) {
        return super.get(h256(key));
    }

    @Override
    public void update(byte[] key, byte[] value) {
        super.update(h256(key), value);
    }

    @Override
    public void delete(byte[] key) {
        super.delete(h256(key));
    }

    /**
     * Returns a copy of this trie.
     *
     * The {@link Cache} object returned will be a copy of this object's cache, but the two will
     * share the same references to their data sources ({@link IByteArrayKeyValueStore} objects) and
     * each {@link Node} will retain the same references as the original node's
     * {@link org.aion.rlp.Value} objects.
     *
     * The previous root and current root of this trie will return deep copies of these objects only
     * if they are of type {@code byte[]}. Otherwise, the original references will be passed on. It
     * is expected that they will indeed be byte arrays.
     *
     * @return A copy of this trie.
     */
    public SecureTrie copy() {
        synchronized (super.getCache()) {
            SecureTrie secureTrieCopy = new SecureTrie();

            Object originalPreviousRoot = super.getPrevRoot();
            Object originalRoot = super.getRoot();

            Object previousRootCopy = null;
            Object rootCopy = null;

            // We should only be dealing in terms of byte arrays for the previous & current roots.
            // But if we do not get a byte[] here, then we cannot make a deep copy of the object.
            if (originalPreviousRoot instanceof byte[]) {
                byte[] originalPreviousRootAsBytes = (byte[]) originalPreviousRoot;
                previousRootCopy =
                        Arrays.copyOf(
                                originalPreviousRootAsBytes, originalPreviousRootAsBytes.length);
            } else if (originalPreviousRoot != null) {
                previousRootCopy = originalPreviousRoot;
            }

            if (originalRoot instanceof byte[]) {
                byte[] originalRootAsBytes = (byte[]) originalRoot;
                rootCopy = Arrays.copyOf(originalRootAsBytes, originalRootAsBytes.length);
            } else if (originalRoot != null) {
                rootCopy = originalRoot;
            }

            secureTrieCopy.setPrevRoot(previousRootCopy);
            secureTrieCopy.setRoot(rootCopy);
            secureTrieCopy.setCache(super.getCache().copy());
            secureTrieCopy.setPruningEnabled(super.isPruningEnabled());
            return secureTrieCopy;
        }
    }
}
