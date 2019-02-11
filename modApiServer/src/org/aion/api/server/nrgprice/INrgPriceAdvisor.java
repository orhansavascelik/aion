package org.aion.api.server.nrgprice;

import org.aion.type.api.type.IBlock;
import org.aion.type.api.type.ITransaction;

public interface INrgPriceAdvisor<BLK extends IBlock, TXN extends ITransaction> {
    /* Is the recommendation engine hungry for more data?
     * Recommendations prescribed by engine while hungry are left up to the engine itself
     */
    boolean isHungry();

    /* Build the recommendation, one block at a time
     */
    void processBlock(BLK blk);

    /* Retrieve the recommendation stored in internal representation
     */
    long computeRecommendation();

    /* flush all history for recommendation engine
     */
    void flush();
}
