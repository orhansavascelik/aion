package org.aion.equihash;

import org.aion.zero.types.AionBlock;

/**
 * This class encapsulates a valid solution for the given block. This class allows solutions to be
 * passed between classes as needed.
 *
 * @author Ross Kitsis (ross@nuco.io)
 */
public class Solution implements org.aion.type.api.interfaces.block.Solution {

    private final AionBlock block;
    private final byte[] nonce;
    private final byte[] solution;

    public Solution(AionBlock block, byte[] nonce, byte[] solution) {

        this.block = block;
        this.nonce = nonce;
        this.solution = solution;
    }

    public AionBlock getBlock() {
        return block;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public byte[] getSolution() {
        return solution;
    }
}
