package org.aion.mcf.types;

import org.aion.base.type.IBlockHeader;

import java.math.BigInteger;

/** @author jay */
public interface IPowBlockHeader extends IBlockHeader {

    byte[] getDifficulty();

    BigInteger getDifficultyBI();

    void setDifficulty(byte[] _diff);

    byte[] getPowBoundary();

    byte[] getNonce();

    void setNonce(byte[] _nc);
}
