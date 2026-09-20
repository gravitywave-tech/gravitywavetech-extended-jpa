package org.gravitywavetech.framework.util;

import com.github.f4b6a3.uuid.util.*;
import org.apache.commons.codec.binary.Base64;

import java.nio.ByteBuffer;
import java.util.UUID;

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
        return Base58.encode(uuidBytes());
    }

    public static String encodeBase58Uuid(String uuidString) {
        return Base58.encode(toBytes(UUID.fromString(uuidString)));
    }

    public static String decodeBase58Uuid(String base58uuid) {
        return fromBytes(Base58.decode(base58uuid)).toString();
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

    public static void main(String[] args){
        // 1. 生成标准 UUID (等同你的 uuid())
        UUID uuid = UUID.randomUUID();


        // 2. Base64 编解码 (等同你的 base64Uuid(), encodeBase64Uuid(), decodeBase64Uuid())
//        String base64Str = Base64Codec.(uuid);
//        UUID decodedUuid1 = Base64Codec.decode(base64Str);
//
//        // 3. Base58 编解码 (等同你的 base58Uuid(), encodeBase58Uuid(), decodeBase58Uuid())
//        String base58Str = Base58Codec.encode(uuid);
//        UUID decodedUuid2 = Base58Codec.decode(base58Str);
    }
}
