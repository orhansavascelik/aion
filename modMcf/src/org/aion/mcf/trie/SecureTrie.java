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

    // used by copy()
    private SecureTrie() {
        this(null, null);
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

    public SecureTrie copy() {
        SecureTrie trieCopy = new SecureTrie();

        Object actualPrevRoot = super.getPrevRoot();
        Object copyPrevRoot = null;

        // prevRoot and root should only be byte arrays for us.

        if (actualPrevRoot instanceof byte[]) {
            byte[] actualPrevRootBytes = (byte[]) actualPrevRoot;
            copyPrevRoot = Arrays.copyOf(actualPrevRootBytes, actualPrevRootBytes.length);
        } else if (actualPrevRoot != null) {
            // Not much we can do here, thankfully this should never be hit anyway.
            copyPrevRoot = actualPrevRoot;
        }

        Object actualRoot = super.getRoot();
        Object copyRoot = null;

        if (actualRoot instanceof byte[]) {
            byte[] actualRootBytes = (byte[]) actualRoot;
            copyRoot = Arrays.copyOf(actualRootBytes, actualRootBytes.length);
        } else if (actualRoot != null) {
            // Not much we can do here, thankfully this should never be hit anyway.
            copyRoot = actualRoot;
        }

        trieCopy.setPrevRoot(copyPrevRoot);
        trieCopy.setRoot(copyRoot);
        trieCopy.setCache(super.getCache().copy());
        trieCopy.setPruningEnabled(super.isPruningEnabled());
        return trieCopy;
    }
}
