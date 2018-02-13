package com.github.mike10004.chromecookieimplant;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Value class that represents a cookie with structure defined by the Chrome Extensions API.
 * All fields are nullable.
 * See https://developer.chrome.com/extensions/cookies#method-set.
 */
public class ChromeCookie {

    @SuppressWarnings("unused") // used in deserialization
    private ChromeCookie() {
        url = null;
        name = null;
        value = null;
        domain = null;
        path = null;
        secure = null;
        httpOnly = null;
        sameSite = null;
        expirationDate = null;
        storeId = null;
        session = null;
    }

    private ChromeCookie(Builder builder) {
        url = builder.url;
        name = builder.name;
        value = builder.value;
        domain = builder.domain;
        path = builder.path;
        secure = builder.secure;
        httpOnly = builder.httpOnly;
        sameSite = builder.sameSite;
        session = builder.session;
        expirationDate = builder.expirationDate;
        storeId = builder.storeId;
    }

    @SuppressWarnings("unused")
    public enum SameSiteStatus { no_restriction, lax, strict }

    public final String url;
    public final String name;
    public final String value;
    public final String domain;
    public final String path;
    public final Boolean secure;
    public final Boolean httpOnly;
    public final SameSiteStatus sameSite;
    public final Boolean session;

    /**
     * The expiration date in seconds.
     */
    public final BigDecimal expirationDate;
    public final String storeId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChromeCookie)) return false;

        ChromeCookie that = (ChromeCookie) o;

        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (domain != null ? !domain.equals(that.domain) : that.domain != null) return false;
        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        if (secure != null ? !secure.equals(that.secure) : that.secure != null) return false;
        if (httpOnly != null ? !httpOnly.equals(that.httpOnly) : that.httpOnly != null) return false;
        if (sameSite != that.sameSite) return false;
        if (session != null ? !session.equals(that.session) : that.session != null) return false;
        if (expirationDate != null ? !expirationDate.equals(that.expirationDate) : that.expirationDate != null)
            return false;
        return storeId != null ? storeId.equals(that.storeId) : that.storeId == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (secure != null ? secure.hashCode() : 0);
        result = 31 * result + (httpOnly != null ? httpOnly.hashCode() : 0);
        result = 31 * result + (sameSite != null ? sameSite.hashCode() : 0);
        result = 31 * result + (session != null ? session.hashCode() : 0);
        result = 31 * result + (expirationDate != null ? expirationDate.hashCode() : 0);
        result = 31 * result + (storeId != null ? storeId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChromeCookie{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", domain='" + domain + '\'' +
                ", path='" + path + '\'' +
                ", secure=" + secure +
                ", httpOnly=" + httpOnly +
                ", sameSite=" + sameSite +
                ", session=" + session +
                ", expirationDate=" + expirationDate +
                ", storeId='" + storeId + '\'' +
                '}';
    }

    public static Builder builder(String url) {
        return new Builder().url(url);
    }

    @SuppressWarnings("unused")
    public static final class Builder {
        private String url;
        private String name;
        private String value;
        private String domain;
        private String path;
        private Boolean secure;
        private Boolean httpOnly;
        private SameSiteStatus sameSite;
        private Boolean session;
        private BigDecimal expirationDate;
        private String storeId;

        private Builder() {
        }

        public Builder url(String val) {
            url = val;
            return this;
        }

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder value(String val) {
            value = val;
            return this;
        }

        public Builder domain(String val) {
            domain = val;
            return this;
        }

        public Builder path(String val) {
            path = val;
            return this;
        }

        public Builder secure(Boolean val) {
            secure = val;
            return this;
        }

        public Builder httpOnly(Boolean val) {
            httpOnly = val;
            return this;
        }

        public Builder sameSite(SameSiteStatus val) {
            sameSite = val;
            return this;
        }

        public Builder session(Boolean val) {
            session = val;
            return this;
        }

        /**
         * Expiration date, in seconds since the epoch.
         * @param val number of seconds since the epoch
         * @return this builder instance
         */
        public Builder expirationDate(BigDecimal val) {
            expirationDate = val;
            return this;
        }

        private static BigDecimal convertDateToDecimal(@Nullable Date date) {
            return date == null ? null : BigDecimal.valueOf(date.getTime()).scaleByPowerOfTen(-3);
        }

        public Builder expirationDate(Date date) {
            return expirationDate(convertDateToDecimal(date));
        }

        /**
         * Expiration date, in seconds since the epoch.
         * @param val number of seconds since the epoch
         * @return this builder instance
         */
        public Builder expirationDate(Double val) {
            expirationDate = val == null ? null : BigDecimal.valueOf(val);
            return this;
        }

        public Builder storeId(String val) {
            storeId = val;
            return this;
        }

        public ChromeCookie build() {
            return new ChromeCookie(this);
        }
    }

    public String summarize() {
        return "Cookie{domain=" + domain + ",path=" + path + ",name=" + name + "}";
    }

}
