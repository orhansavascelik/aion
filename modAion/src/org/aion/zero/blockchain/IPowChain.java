package org.aion.zero.blockchain;
import java.math.BigInteger;
import org.aion.base.type.Address;
import org.aion.base.type.Hash256;
import org.aion.base.type.IBlock;
import org.aion.base.type.ITransaction;
import org.aion.mcf.core.AbstractTxInfo;
import org.aion.mcf.core.IBlockchain;
import org.aion.mcf.types.AbstractBlockHeader;
import org.aion.mcf.types.AbstractTxReceipt;

/**
 * proof of work chain interface.
 *
 * @param <BLK>
 * @param <BH>
 */
@SuppressWarnings("rawtypes")
public interface IPowChain<
        BLK extends IBlock,
        BH extends AbstractBlockHeader,
        TX extends ITransaction,
        TR extends AbstractTxReceipt,
        INFO extends AbstractTxInfo>
        extends IBlockchain<BLK, BH, TX, TR, INFO> {

    BigInteger getTotalDifficulty();

    void setTotalDifficulty(BigInteger totalDifficulty);

    BigInteger getTotalDifficultyByHash(Hash256 hash);

    Address getMinerCoinbase();
}
