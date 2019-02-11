package org.aion.equihash;

import static org.aion.type.api.util.ByteUtil.bytesToInts;
import static org.aion.type.api.util.ByteUtil.intsToBytes;

/**
 * This package contains utility functions commonly called across multiple equihash classes.
 *
 * @author Ross (ross@nuco.io)
 */
public class EquiUtils {

    /**
     * Expand an array from compressed format into hash-append value format.
     *
     * <p>More precise extendArray method, allows for better specification of in and outLen
     *
     * @throws NullPointerException when given null input
     */
    public static void extendArray(byte[] in, int inLen, byte[] out, int bitLen, int bytePad) {

        if (in == null) throw new NullPointerException("null input array");
        else if (out == null) throw new NullPointerException("null output array");

        int outWidth = (bitLen + 7) / 8 + bytePad;
        int bitLenMask = (1 << bitLen) - 1;
        int accBits = 0;
        int accValue = 0;

        int j = 0;
        for (int i = 0; i < inLen; i++) {
            accValue = (accValue << 8) | (in[i] & 0xff);
            accBits += 8;

            if (accBits >= bitLen) {
                accBits -= bitLen;

                for (int x = bytePad; x < outWidth; x++) {

                    out[j + x] =
                            (byte)
                                    ((
                                            // Big-endian
                                            accValue >>> (accBits + (8 * (outWidth - x - 1))))
                                            & (
                                            // Apply bit_len_mask across byte boundaries
                                            (bitLenMask >>> (8 * (outWidth - x - 1))) & 0xFF));
                }
                j += outWidth;
            }
        }
    }

    /**
     * A simplified extend array method using default outLen and inLen parameters. Expand an array
     * from compressed format into hash-append value format.
     *
     * <p>More precise extendArray method, allows for better specification of in and outLen
     *
     * @throws NullPointerException when given null input
     */
    public static void extendArray(byte[] in, byte[] out, int bitLen, int bytePad) {

        if (in == null) throw new NullPointerException("null input array");
        else if (out == null) throw new NullPointerException("null output array");

        int outWidth = (bitLen + 7) / 8 + bytePad;
        int bitLenMask = (1 << bitLen) - 1;
        int accBits = 0;
        int accValue = 0;

        int j = 0;
        for (byte anIn : in) {

            accValue = (accValue << 8) | (anIn & 0xff);
            accBits += 8;

            if (accBits >= bitLen) {
                accBits -= bitLen;

                for (int x = bytePad; x < outWidth; x++) {

                    out[j + x] =
                            (byte)
                                    ((
                                            // Big-endian
                                            accValue >>> (accBits + (8 * (outWidth - x - 1))))
                                            & (
                                            // Apply bit_len_mask across byte boundaries
                                            (bitLenMask >>> (8 * (outWidth - x - 1))) & 0xFF));
                }
                j += outWidth;
            }
        }
    }

    /**
     * Compare len bytes up to lenIndicies of a and b,
     *
     * @param a StepRow a
     * @param b StepRow b
     * @param len Starting position in hashes
     * @param lenIndices Length of indices
     * @return True if a > b, else false
     * @throws NullPointerException when given null input
     */
    public static boolean indicesBefore(StepRow a, StepRow b, int len, int lenIndices) {
        if (a == null || b == null)
            throw new NullPointerException("null StepRow passed for comparison");

        byte[] hashA = a.getHash();
        byte[] hashB = b.getHash();

        if (hashA == null) throw new NullPointerException("null hash within StepRow a");
        else if (hashB == null) throw new NullPointerException("null hash within StepRow b");

        int i = 0;
        while ((hashA[i + len] & 0xff) == (hashB[i + len] & 0xff) && i < lenIndices) {
            i++;
        }

        return (hashA[i + len] & 0xff) <= (hashB[i + len] & 0xff);
    }

    /**
     * Compress an array into index format.
     *
     * @param in Input array
     * @param out Output array
     * @param bitLen Number of bits to compress
     * @param bytePad Byte padding to ensure a whole number of bytes examined.
     * @throws NullPointerException when given null input
     */
    private static void compressArray(byte[] in, byte[] out, int bitLen, int bytePad) {

        if (in == null) throw new NullPointerException("null input array");
        else if (out == null) throw new NullPointerException("null output array");

        int inWidth = (bitLen + 7) / 8 + bytePad;
        int bitLenMask = (1 << bitLen) - 1;

        int accBits = 0;
        int accValue = 0;

        int j = 0;
        for (int i = 0; i < out.length; i++) {
            if (accBits < 8) {
                accValue = accValue << bitLen;
                for (int x = bytePad; x < inWidth; x++) {
                    accValue =
                            accValue
                                    | ((in[j + x]
                                                    & ((bitLenMask >> (8 * (inWidth - x - 1)))
                                                            & 0xFF))
                                            << (8 * (inWidth - x - 1)));
                }
                j += inWidth;
                accBits += bitLen;
            }

            accBits -= 8;
            out[i] = (byte) ((accValue >> accBits) & 0xFF);
        }
    }

    /**
     * Converts a solution to its compress I2BSP format to be added to block headers.
     *
     * @param indices Indices array
     * @param cBitLen Collision bit length
     * @return Minimized version of passed indices
     * @throws NullPointerException when given null input
     */
    public static byte[] getMinimalFromIndices(int[] indices, int cBitLen) {

        if (indices == null) throw new NullPointerException("null indices array");

        int lenIndices = indices.length * Integer.BYTES;
        int minLen = (cBitLen + 1) * lenIndices / (8 * Integer.BYTES);
        int bytePad = Integer.BYTES - ((cBitLen + 1) + 7) / 8;

        byte[] arr = intsToBytes(indices, true);

        byte[] ret = new byte[minLen];
        compressArray(arr, ret, cBitLen + 1, bytePad);

        return ret;
    }

    /**
     * Get indices of solutions from minimized array format.
     *
     * @param minimal Byte array in minimal format
     * @param cBitLen Number of bits in a collision
     * @return An array containing solution indices.
     * @throws NullPointerException when given null input
     */
    public static int[] getIndicesFromMinimal(byte[] minimal, int cBitLen) {
        if (minimal == null) {
            throw new NullPointerException("null minimal bytes");
        }

        int lenIndices = 8 * Integer.BYTES * minimal.length / (cBitLen + 1);
        int bytePad = Integer.BYTES - ((cBitLen + 1) + 7) / 8;

        byte[] arr = new byte[lenIndices];
        EquiUtils.extendArray(minimal, arr, cBitLen + 1, bytePad);

        return bytesToInts(arr, true);
    }
}
