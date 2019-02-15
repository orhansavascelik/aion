package org.aion.mcf.account;

import java.nio.charset.StandardCharsets;
import org.aion.rlp.RLP;
import org.aion.rlp.RLPList;

public class CipherParams {

    private String iv;

    // rlp

    public byte[] toRlp() {
        byte[] bytesIv = RLP.encodeString(this.iv);
        return RLP.encodeList(bytesIv);
    }

    public static CipherParams parse(byte[] bytes) {
        RLPList list = (RLPList) RLP.decode2(bytes).get(0);
        CipherParams cp = new CipherParams();
        cp.setIv(new String(list.get(0).getRLPData(), StandardCharsets.US_ASCII));
        return cp;
    }

    // setters

    public String getIv() {
        return iv;
    }

    // getters

    public void setIv(String iv) {
        this.iv = iv;
    }
}
