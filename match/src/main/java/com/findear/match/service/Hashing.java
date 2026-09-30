package com.findear.match.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA-256 해시 도우미 (결정적 선택·점수용). */
public final class Hashing {

    private Hashing() {
    }

    public static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    /** 앞 4바이트를 big-endian 부호 없는 정수로 읽는다. */
    public static long unsignedInt(byte[] hash, int offset) {
        return Integer.toUnsignedLong(ByteBuffer.wrap(hash).getInt(offset));
    }

    /** 앞 8바이트를 big-endian long으로 읽는다. */
    public static long firstLong(byte[] hash) {
        return ByteBuffer.wrap(hash).getLong(0);
    }
}
