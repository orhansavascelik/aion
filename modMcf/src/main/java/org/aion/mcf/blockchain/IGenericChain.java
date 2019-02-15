package org.aion.mcf.blockchain;

import org.aion.type.api.interfaces.block.Block;
import org.aion.mcf.core.AbstractTxInfo;
import org.aion.mcf.db.IBlockStoreBase;
import org.aion.mcf.type.AbstractBlockHeader;

/** Generic Chain interface. */
public interface IGenericChain<BLK extends Block, BH extends AbstractBlockHeader> {

    BLK getBlockByNumber(long number);

    BLK getBlockByHash(byte[] hash);

    IBlockStoreBase<?, ?> getBlockStore();

    BLK getBestBlock();

    AbstractTxInfo getTransactionInfo(byte[] hash);

    void flush();
}
