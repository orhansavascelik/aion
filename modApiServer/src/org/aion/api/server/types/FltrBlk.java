package org.aion.api.server.types;

import org.aion.type.api.interfaces.block.BlockSummary;

public class FltrBlk extends Fltr {

    public FltrBlk() {
        super(Type.BLOCK);
    }

    @Override
    public boolean onBlock(BlockSummary b) {
        add(new EvtBlk(b.getBlock()));
        return true;
    }
}
