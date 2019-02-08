package org.aion.crypto.ed25519;

import java.util.Arrays;
import org.aion.crypto.AddressSpecs;
import org.aion.crypto.ISignature;
import org.aion.util.bytes.ByteUtil;

/**
 * ED25519 signature implementation. Each {@link Ed25519Signature} contains two components, public
 * key and raw signature.
 *
 * @author yulong
 */
public class Ed25519Signature implements ISignature {

    private static final int LEN = ECKeyEd25519.PUBKEY_BYTES + ECKeyEd25519.SIG_BYTES;

    private byte[] pk;

    private byte[] sig;

    public Ed25519Signature(byte[] pk, byte[] sig) {
        this.pk = pk;
        this.sig = sig;
    }

    public static Ed25519Signature fromBytes(byte[] args) {
        if (args != null && args.length == LEN) {
            byte[] pk = Arrays.copyOfRange(args, 0, ECKeyEd25519.PUBKEY_BYTES);
            byte[] sig = Arrays.copyOfRange(args, ECKeyEd25519.PUBKEY_BYTES, LEN);
            return new Ed25519Signature(pk, sig);
        } else {
            System.err.println("Ed25519 signature decode failed!");
            return null;
        }
    }

    @Override
    public byte[] toBytes() {
        byte[] buf = new byte[LEN];
        System.arraycopy(pk, 0, buf, 0, ECKeyEd25519.PUBKEY_BYTES);
        System.arraycopy(sig, 0, buf, ECKeyEd25519.PUBKEY_BYTES, ECKeyEd25519.SIG_BYTES);

        return buf;
    }

    @Override
    public byte[] getSignature() {
        return sig;
    }

    @Override
    public byte[] getPubkey(byte[] msg) {
        return pk;
    }

    @Override
    public String toString() {

        byte[] address = this.getAddress();

        return "[pk: "
                + (this.pk == null ? "null" : ByteUtil.toHexString(this.pk))
                + " address: "
                + (address == null ? "null" : ByteUtil.toHexString(address))
                + " signature: "
                + (this.sig == null ? "null" : ByteUtil.toHexString(this.sig))
                + "]";
    }

    @Override
    public byte[] getAddress() {
        if (this.pk == null) return null;
        return AddressSpecs.computeA0Address(this.pk);
    }
}
