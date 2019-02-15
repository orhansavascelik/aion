package org.aion.type.api.interfaces.block;

/** @author jay */
public interface BlockIdentifier {

    byte[] getHash();

    long getNumber();
}
