package org.aion.type.api.interfaces.common;

/** A {@value SIZE}-byte sized public-facing Hash. */
public interface Hash {
    /** The number of bytes in an {@code Hash}. */
    int SIZE = 32;

    /**
     * The bytes that make up the {@code Hash}.
     *
     * @return The bytes of the Hash.
     */
    byte[] toBytes();

}
