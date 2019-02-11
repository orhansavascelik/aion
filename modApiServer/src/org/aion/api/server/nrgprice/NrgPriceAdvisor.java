package org.aion.api.server.nrgprice;

import org.aion.type.api.type.IBlock;
import org.aion.type.api.type.ITransaction;

public abstract class NrgPriceAdvisor<BLK extends IBlock, TXN extends ITransaction>
        implements INrgPriceAdvisor<BLK, TXN> {

    protected long defaultPrice;
    protected long maxPrice;

    /* Impose a min & max thresholds on the recommendation output
     */
    public NrgPriceAdvisor(long defaultPrice, long maxPrice) {
        this.defaultPrice = defaultPrice;
        this.maxPrice = maxPrice;
    }
}
