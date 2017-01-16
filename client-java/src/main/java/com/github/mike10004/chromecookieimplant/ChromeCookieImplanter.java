/*
 * (c) 2017 Novetta
 *
 * Created by mike
 */
package com.github.mike10004.chromecookieimplant;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

public class ChromeCookieImplanter {

    private final ByteSource crxBytes;

    public ChromeCookieImplanter() {
        this(Resources.asByteSource(getCrxResourceOrDie()));
    }

    @VisibleForTesting
    ChromeCookieImplanter(ByteSource crxBytes) {
        this.crxBytes = crxBytes;
    }

    private static URL getCrxResourceOrDie() throws IllegalStateException {
        URL url = ChromeCookieImplanter.class.getResource("/chrome-cookie-implant.crx");
        if (url == null) {
            throw new IllegalStateException("resource does not exist: classpath:/chrome-cookie-implant.crx");
        }
        return url;
    }

    public void copyCrxTo(OutputStream outputStream) throws IOException {
        crxBytes.copyTo(outputStream);
    }
}
