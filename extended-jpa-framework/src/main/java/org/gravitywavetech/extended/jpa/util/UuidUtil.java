package org.gravitywavetech.extended.jpa.util;

import org.apache.commons.codec.binary.Base64;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * UUID 生成与编码工具。
 *
 * <p>除标准 UUID 字符串外，提供两种紧凑编码形式：Base64URL（可读性略高、字符集小）
 * 与 Base58（比特币风格、避开易混淆字符）。用于需要短主键或 URL 安全的场景。</p>
 */
public abstract class UuidUtil {

    private static final int UUID_BYTES = 16;

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static byte[] uuidBytes() {
        return toBytes(UUID.randomUUID());
    }

    public static String base64Uuid() {
        return Base64.encodeBase64URLSafeString(uuidBytes());
    }

    public static String encodeBase64Uuid(String uuidString) {
        return Base64.encodeBase64URLSafeString(toBytes(UUID.fromString(uuidString)));
    }

    public static String decodeBase64Uuid(String compressedUuid) {
        return fromBytes(Base64.decodeBase64(compressedUuid)).toString();
    }

    public static String base58Uuid() {
        return org.bitcoinj.base.Base58.encode(uuidBytes());
    }

    public static String encodeBase58Uuid(String uuidString) {
        return org.bitcoinj.base.Base58.encode(toBytes(UUID.fromString(uuidString)));
    }

    public static String decodeBase58Uuid(String base58uuid) {
        return fromBytes(org.bitcoinj.base.Base58.decode(base58uuid)).toString();
    }

    private static byte[] toBytes(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];
        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> 8 * (7 - i));
            buffer[8 + i] = (byte) (lsb >>> 8 * (7 - i));
        }
        return buffer;
    }


    private static UUID fromBytes(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }

}
