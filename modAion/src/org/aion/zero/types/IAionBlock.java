package org.aion.zero.types;

import java.math.BigInteger;
import java.util.List;
import org.aion.base.type.Address;
import org.aion.base.type.IBlock;

/** aion block interface. */
public interface IAionBlock extends IBlock<AionTransaction, A0BlockHeader> {
    byte[] getDifficulty();

    BigInteger getCumulativeDifficulty();

    void setNonce(byte[] nonce);
}
