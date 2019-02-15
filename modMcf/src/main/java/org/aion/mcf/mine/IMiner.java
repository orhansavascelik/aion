package org.aion.mcf.mine;

import com.google.common.util.concurrent.ListenableFuture;
import org.aion.type.api.interfaces.block.Block;
import org.aion.mcf.type.AbstractBlockHeader;

/**
 * Miner interface.
 *
 * @param <Blk>
 * @param <BH>
 */
public interface IMiner<Blk extends Block<?, ?>, BH extends AbstractBlockHeader> {

    ListenableFuture<Long> mine(Blk block);

    boolean validate(BH blockHeader);
}
