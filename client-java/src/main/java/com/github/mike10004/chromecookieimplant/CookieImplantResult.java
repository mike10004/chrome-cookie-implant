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
    public final ChromeCookie savedCookie;

    public CookieImplantResult(int index, boolean success, String message, ChromeCookie savedCookie) {
        this.index = index;
        this.success = success;
        this.message = message;
        this.savedCookie = savedCookie;
    }
}
