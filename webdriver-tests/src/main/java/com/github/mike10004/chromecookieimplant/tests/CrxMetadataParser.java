package com.github.mike10004.chromecookieimplant.tests;

import com.github.mike10004.chromecookieimplant.tests.CrxMetadata.CrxHeader;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedInteger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;

public class CrxMetadataParser {

    public CrxMetadataParser() {
    }

    public CrxMetadata parse(File crxFile) throws IOException {
        byte[] crxBytes = Files.toByteArray(crxFile);
        CrxHeader crxHeader = readHeader(crxBytes);
        CrxMetadata metadata = readMetadata(crxBytes, crxHeader);
        return metadata;
    }

    protected CrxMetadata readMetadata(byte[] crxBytes, CrxHeader header) throws IOException {
        byte[] pubkeyBytes = slice(crxBytes, 16, header.pubkeyLength);
        String pubkey = new String(pubkeyBytes, StandardCharsets.US_ASCII);
        HashCode pubkeyHash = Hashing.sha256().hashBytes(pubkeyBytes);
        String digest = pubkeyHash.toString().toLowerCase(Locale.ROOT);
        StringBuilder idBuilder = new StringBuilder(ID_LEN);
        translate(DIGEST_CHARS, CRX_ID_CHARS, digest, 0, ID_LEN, idBuilder);
        String id = idBuilder.toString();
        return new CrxMetadata(header, pubkey, id);
    }

    private static final int ID_LEN = 32;

    private static final char[] DIGEST_CHARS = "0123456789abcdef".toCharArray();
    private static final char[] CRX_ID_CHARS = "abcdefghijklmnop".toCharArray();

    @SuppressWarnings("SameParameterValue")
    protected void translate(char[] from, char[] to, String source, int sourceStart, int sourceLen, StringBuilder sink) throws IOException {
        checkArgument(from.length == to.length, "arrays must be congruent");
        for (int i = sourceStart; i < (sourceStart + sourceLen); i++) {
            char untranslated = source.charAt(i);
            int fromIndex = Arrays.binarySearch(from, untranslated);
            char translated = untranslated;
            if (fromIndex >= 0) {
                translated = to[fromIndex];
            }
            sink.append(translated);
        }
    }

    protected CrxHeader readHeader(byte[] crxBytes) throws IOException {
        byte[] headerBytes = slice(crxBytes, 0, 16);
        try (LittleEndianDataInputStream in = new LittleEndianDataInputStream(new ByteArrayInputStream(headerBytes))) {
            byte[] value0Bytes = new byte[4];
            in.readFully(value0Bytes);
            String value0 = new String(value0Bytes, StandardCharsets.US_ASCII);
            int value1 = Ints.checkedCast(UnsignedInteger.fromIntBits(in.readInt()).longValue());
            int pubkeyLength = Ints.checkedCast(UnsignedInteger.fromIntBits(in.readInt()).longValue());
            int value3 = Ints.checkedCast(UnsignedInteger.fromIntBits(in.readInt()).longValue());
            return new CrxHeader(value0, value1, pubkeyLength, value3);
        }
    }

    static byte[] slice(byte[] array, int start, int len) {
        byte[] copied = new byte[len];
        System.arraycopy(array, start, copied, 0, len);
        return copied;
    }
}
