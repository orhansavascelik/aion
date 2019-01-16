package org.aion.mcf.blockchain;

import org.aion.base.type.IBlock;
import org.aion.base.type.ITransaction;
import org.aion.mcf.blockchain.IBlockConstants;
import org.aion.mcf.valid.BlockHeaderValidator;
import org.aion.mcf.valid.ParentBlockHeaderValidator;

/** Chain configuration interface. */
public interface IChainCfg<Blk extends IBlock<?, ?>, Tx extends ITransaction> {

    boolean acceptTransactionSignature(Tx tx);

    IBlockConstants getConstants();

    IBlockConstants getCommonConstants();

    BlockHeaderValidator createBlockHeaderValidator();

    ParentBlockHeaderValidator createParentHeaderValidator();
}
