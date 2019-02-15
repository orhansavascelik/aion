package org.aion.mcf.blockchain;

import org.aion.type.api.interfaces.db.RepositoryCache;
import org.aion.type.api.interfaces.block.Block;
import org.aion.log.AionLoggerFactory;
import org.aion.log.LogEnum;
import org.aion.mcf.db.IBlockStoreBase;
import org.aion.mcf.type.AbstractTransaction;
import org.aion.mcf.type.AbstractTxReceipt;
import org.slf4j.Logger;

/** TransactionExtend executor base class. */
public abstract class TxExecutorBase<
        BLK extends Block<?, ?>,
        TX extends AbstractTransaction,
        BS extends IBlockStoreBase<?, ?>,
        TR extends AbstractTxReceipt<?>> {

    protected static final Logger LOG = AionLoggerFactory.getLogger(LogEnum.VM.toString());

    protected TX tx;

    protected RepositoryCache<?, ?> track;

    protected RepositoryCache<?, ?> cacheTrack;

    protected BS blockStore;

    protected TR receipt;

    protected BLK currentBlock;
}
