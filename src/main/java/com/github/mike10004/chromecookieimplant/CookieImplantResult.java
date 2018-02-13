package com.github.mike10004.chromecookieimplant;

/**
 * Value class that represents the result of an attempt to implant a single cookie.
 */
public class CookieImplantResult {

    /**
     * Index corresponding to the cookie attempt.
     */
    public final int index;

    /**
     * Success flag.
     */
    public final boolean success;

    /**
     * Message relating to the success status.
     */
    public final String message;

    /**
     * The output cookie. This is likely null if success is false.
     */
    public final ChromeCookie savedCookie;

    /**
     * Constructs a result instance.
     * @param index the index
     * @param success the success flag
     * @param message the message
     * @param savedCookie the cookie
     */
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
