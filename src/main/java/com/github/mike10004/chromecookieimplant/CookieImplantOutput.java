package com.github.mike10004.chromecookieimplant;

import java.util.List;

/**
 * Value class that represents the collection of results of an attempt to
 * implant cookies.
 */
public class CookieImplantOutput {

    /**
     * Cookie processing status.
     */
    public CookieProcessingStatus status;

    /**
     * List of implant results.
     */
    public List<CookieImplantResult> implants;

    @Override
    public String toString() {
        List<CookieImplantResult> implants_ = implants;
        return "CookieImplantOutput{" +
                "status=" + status +
                ", implants.size=" + (implants_ == null ? -1 : implants_.size()) +
                '}';
    }
}
