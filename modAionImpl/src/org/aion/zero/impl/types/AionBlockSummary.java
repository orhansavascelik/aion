package org.aion.zero.impl.types;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.aion.type.api.interfaces.block.BlockSummary;
import org.aion.mcf.type.AbstractBlockSummary;
import org.aion.type.api.interfaces.common.Address;
import org.aion.zero.types.AionTransaction;
import org.aion.zero.types.AionTxExecSummary;
import org.aion.zero.types.AionTxReceipt;
import org.aion.zero.types.AionBlock;

/**
 * Modified to add transactions
 *
 * @author yao
 */
public class AionBlockSummary
        extends AbstractBlockSummary<AionBlock, AionTransaction, AionTxReceipt, AionTxExecSummary>
        implements BlockSummary {

    public AionBlockSummary(
            AionBlock block,
            Map<Address, BigInteger> rewards,
            List<AionTxReceipt> receipts,
            List<AionTxExecSummary> summaries) {
        this.block = block;
        this.rewards = rewards;
        this.receipts = receipts;
        this.summaries = summaries;
    }
}
