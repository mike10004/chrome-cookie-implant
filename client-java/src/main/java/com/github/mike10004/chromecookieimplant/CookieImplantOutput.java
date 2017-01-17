package com.github.mike10004.chromecookieimplant;

import java.util.List;

public class CookieImplantOutput {

    public CookieProcessingStatus status;
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
