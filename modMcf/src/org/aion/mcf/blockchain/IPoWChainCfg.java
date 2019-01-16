package org.aion.mcf.blockchain;

import org.aion.base.type.IBlock;
import org.aion.base.type.ITransaction;
import org.aion.mcf.core.IDifficultyCalculator;
import org.aion.mcf.core.IRewardsCalculator;

public interface IPoWChainCfg<Blk extends IBlock<?, ?>, Tx extends ITransaction> extends IChainCfg<Blk, Tx> {
    IDifficultyCalculator getDifficultyCalculator();

    IRewardsCalculator getRewardsCalculator();
}
