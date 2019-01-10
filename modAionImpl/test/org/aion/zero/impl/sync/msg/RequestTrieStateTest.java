package org.aion.zero.impl.sync.msg;

import static com.google.common.truth.Truth.assertThat;
import static org.aion.zero.impl.sync.TrieDatabase.STATE;

import org.aion.rlp.RLP;
import org.junit.Test;

/**
 * Unit tests for {@link RequestTrieState} messages.
 *
 * @author Alexandra Roatis
 */
public class RequestTrieStateTest {
    private static final byte[] nodeKey =
            new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
                24, 25, 26, 27, 28, 29, 30, 31, 32
            };
    private static final byte[] emptyKey = new byte[] {};

    @Test(expected = NullPointerException.class)
    public void testConstructor_nullKey() {
        new RequestTrieState(null, STATE, 10);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_nullType() {
        new RequestTrieState(nodeKey, null, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_negativeLimit() {
        new RequestTrieState(nodeKey, STATE, -10);
    }

    @Test
    public void testDecode_nullMessage() {
        assertThat(RequestTrieState.decode(null)).isNull();
    }

    @Test
    public void testDecode_emptyMessage() {
        assertThat(RequestTrieState.decode(emptyKey)).isNull();
    }

    @Test
    public void testDecode_missingType() {
        byte[] encoding = RLP.encodeList(RLP.encodeElement(nodeKey), RLP.encodeInt(0));
        assertThat(RequestTrieState.decode(encoding)).isNull();
    }

    @Test
    public void testDecode_missingKey() {
        byte[] encoding = RLP.encodeList(RLP.encodeString(STATE.toString()), RLP.encodeInt(0));
        assertThat(RequestTrieState.decode(encoding)).isNull();
    }

    @Test
    public void testDecode_missingLimit() {
        byte[] encoding =
                RLP.encodeList(RLP.encodeElement(nodeKey), RLP.encodeString(STATE.toString()));
        assertThat(RequestTrieState.decode(encoding)).isNull();
    }

    @Test
    public void testDecode_additionalValue() {
        byte[] encoding =
                RLP.encodeList(
                        RLP.encodeString(STATE.toString()),
                        RLP.encodeElement(nodeKey),
                        RLP.encodeInt(0),
                        RLP.encodeInt(10));
        assertThat(RequestTrieState.decode(encoding)).isNull();
    }

    @Test
    public void testDecode_outOfOrder() {
        byte[] encoding =
                RLP.encodeList(
                        RLP.encodeElement(nodeKey),
                        RLP.encodeString(STATE.toString()),
                        RLP.encodeInt(0));
        assertThat(RequestTrieState.decode(encoding)).isNull();
    }

    @Test
    public void testDecode_incorrectType() {
        byte[] encoding =
                RLP.encodeList(
                        RLP.encodeString("random"),
                        RLP.encodeElement(new byte[] {1, 2, 3}),
                        RLP.encodeInt(10));
        assertThat(RequestTrieState.decode(encoding)).isNull();
    }

    @Test
    public void testDecode_incorrectKeySize() {
        byte[] encoding =
                RLP.encodeList(
                        RLP.encodeString(STATE.toString()),
                        RLP.encodeElement(new byte[] {1, 2, 3}),
                        RLP.encodeInt(10));
        assertThat(RequestTrieState.decode(encoding)).isNull();
    }

    @Test
    public void testDecode_correct() {
        byte[] encoding =
                RLP.encodeList(
                        RLP.encodeString(STATE.toString()),
                        RLP.encodeElement(nodeKey),
                        RLP.encodeInt(Integer.MAX_VALUE));

        RequestTrieState message = RequestTrieState.decode(encoding);

        assertThat(message).isNotNull();
        assertThat(message.getDbType()).isEqualTo(STATE);
        assertThat(message.getNodeKey()).isEqualTo(nodeKey);
        assertThat(message.getLimit()).isEqualTo(Integer.MAX_VALUE);
    }
}
