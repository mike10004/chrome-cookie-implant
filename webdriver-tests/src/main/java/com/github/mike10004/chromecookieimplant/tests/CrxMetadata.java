package com.github.mike10004.chromecookieimplant.tests;

import static com.google.common.base.Preconditions.checkNotNull;

public class CrxMetadata {
    public final CrxHeader header;
    public final String pubkey;
    public final String id;

    public CrxMetadata(CrxHeader header, String pubkey, String id) {
        this.header = checkNotNull(header);
        this.pubkey = checkNotNull(pubkey);
        this.id = checkNotNull(id);
    }

    public static class CrxHeader {
        public final String value0;
        public final int value1;
        public final int pubkeyLength;
        public final int value3;

        public CrxHeader(String value0, int value1, int pubkeyLength, int value3) {
            this.value0 = checkNotNull(value0);
            this.value1 = value1;
            this.pubkeyLength = pubkeyLength;
            this.value3 = value3;
        }

        @Override
        public String toString() {
            return "CrxHeader{" +
                    "value0='" + value0 + '\'' +
                    ", value1=" + value1 +
                    ", pubkeyLength=" + pubkeyLength +
                    ", value3=" + value3 +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "CrxMetadata{" +
                "id='" + id + '\'' +
                '}';
    }
}
