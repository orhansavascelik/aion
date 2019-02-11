package org.aion.type.api.type;

import java.util.List;

/** @author jay */
public interface IBlockSummary {
    List<?> getReceipts();

    IBlock getBlock();
}
