package org.aion.api.server.nrgprice;

import org.aion.type.api.interfaces.block.Block;
import org.aion.type.api.interfaces.tx.TransactionExtend;

public abstract class NrgPriceAdvisor<BLK extends Block, TXN extends TransactionExtend>
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
