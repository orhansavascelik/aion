package org.aion.type.api.interfaces.block;

import java.util.List;

/** @author jay */
public interface BlockSummary {
    List<?> getReceipts();

    Block getBlock();
}
