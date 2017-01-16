package com.github.mike10004.chromecookieimplant;

import com.github.mike10004.chromecookieimplant.CrxMetadata.CrxHeader;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
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
        return parse(Files.asByteSource(crxFile));
    }

    public CrxMetadata parse(ByteSource crxByteSource) throws IOException {
        CrxHeader crxHeader = readHeader(crxByteSource);
        CrxMetadata metadata = readMetadata(crxByteSource, crxHeader);
        return metadata;
    }

    public CrxMetadata parse(byte[] bytes) throws IOException {
        return parse(ByteSource.wrap(bytes));
    }

    protected CrxMetadata readMetadata(ByteSource crxByteSource, CrxHeader header) throws IOException {
        ByteSource pubkeyByteSource = crxByteSource.slice(CRX_HEADER_LENGTH, header.pubkeyLength);
        byte[] pubkeyBytes = pubkeyByteSource.read();
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

    private static final int CRX_HEADER_LENGTH = 16;

    protected CrxHeader readHeader(ByteSource crxByteSource) throws IOException {
        ByteSource headerByteSource = crxByteSource.slice(0, CRX_HEADER_LENGTH);
        byte[] headerBytes = headerByteSource.read();
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
}
