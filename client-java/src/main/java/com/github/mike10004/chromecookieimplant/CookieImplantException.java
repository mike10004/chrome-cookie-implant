package com.github.mike10004.chromecookieimplant;

@SuppressWarnings("unused")
public class CookieImplantException extends RuntimeException {
    public CookieImplantException() {
    }

    public CookieImplantException(String message) {
        super(message);
    }

    public CookieImplantException(String message, Throwable cause) {
        super(message, cause);
    }

    public CookieImplantException(Throwable cause) {
        super(cause);
    }
}
