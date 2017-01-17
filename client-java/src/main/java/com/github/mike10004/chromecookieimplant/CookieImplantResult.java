/*
 * (c) 2017 Novetta
 *
 * Created by mike
 */
package com.github.mike10004.chromecookieimplant;

public class CookieImplantResult {

    public final int index;
    public final boolean success;
    public final String message;

    /**
     * The output cookie. This is likely null if success is false.
     */
    public final ChromeCookie savedCookie;

    public CookieImplantResult(int index, boolean success, String message, ChromeCookie savedCookie) {
        this.index = index;
        this.success = success;
        this.message = message;
        this.savedCookie = savedCookie;
    }

    @Override
    public String toString() {
        ChromeCookie c = savedCookie;
        String summary = null;
        if (c != null) {
            summary = c.summarize();
        }
        return "CookieImplantResult{" +
                "index=" + index +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", savedCookie=" + summary +
                '}';
    }
}
