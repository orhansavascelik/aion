/*
 * Copyright 2013 Bruno Oliveira, and individual contributors
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.libsodium.jni.keys;

import static org.libsodium.jni.NaCl.sodium;
import static org.libsodium.jni.SodiumConstants.PUBLICKEY_BYTES;
import static org.libsodium.jni.SodiumConstants.SECRETKEY_BYTES;
import static org.libsodium.jni.crypto.Util.zeros;

import org.libsodium.jni.Sodium;
import org.libsodium.jni.crypto.Point;
import org.libsodium.jni.crypto.Util;
import org.libsodium.jni.encoders.Encoder;

public class KeyPair {

    private byte[] publicKey;
    private final byte[] seed;
    private final byte[] secretKey;

    public KeyPair() {
        this.secretKey = zeros(SECRETKEY_BYTES);
        this.publicKey = zeros(PUBLICKEY_BYTES);
        this.seed = null;
        sodium();
        Sodium.crypto_box_curve25519xsalsa20poly1305_keypair(publicKey, secretKey);
    }

    public KeyPair(byte[] seed) {
        Util.checkLength(seed, SECRETKEY_BYTES);
        this.seed = seed;
        this.secretKey = zeros(SECRETKEY_BYTES);
        this.publicKey = zeros(PUBLICKEY_BYTES);
        sodium();
        Util.isValid(
                Sodium.crypto_box_curve25519xsalsa20poly1305_seed_keypair(
                        publicKey, secretKey, seed),
                "Failed to generate a key pair");
    }

    //    public KeyPair(byte[] secretKey) {
    //        this.secretKey = secretKey;
    //        checkLength(this.secretKey, SECRETKEY_BYTES);
    //    }
    public KeyPair(String secretKey, Encoder encoder) {
        this(encoder.decode(secretKey));
    }

    public PublicKey getPublicKey() {
        Point point = new Point();
        byte[] key = publicKey != null ? publicKey : point.mult(secretKey).toBytes();
        return new PublicKey(key);
    }

    public static PublicKey getPublicKey(byte[] sk) {
        Point point = new Point();
        byte[] key = point.mult(sk).toBytes();
        return new PublicKey(key);
    }

    public PrivateKey getPrivateKey() {
        return new PrivateKey(secretKey);
    }

    public byte[] getSeed() {
        return seed;
    }
}
