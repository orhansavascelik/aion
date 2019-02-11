package org.aion.type.api.type;

/** @author jay */
public interface IBlockIdentifier {

    byte[] getHash();

    long getNumber();
}
