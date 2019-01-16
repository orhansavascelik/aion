package org.aion.zero.impl.blockchain;
import org.aion.mcf.mine.IMineRunner;

/**
 * Chain instance pow interface.
 */
public interface IChainInstancePOW extends IAionChain{
    IMineRunner getBlockMiner();
}
