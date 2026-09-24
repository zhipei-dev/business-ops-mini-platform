package dev.zhipei.businessops.domain;

import dev.zhipei.businessops.api.ApiException;

public final class Workflow {
    private Workflow() { }
    public static String purchaseNext(String state, String action) {
        if ("SUBMIT".equals(action) && "DRAFT".equals(state)) return "SUBMITTED";
        if ("APPROVE".equals(action) && "SUBMITTED".equals(state)) return "APPROVED";
        if ("RECEIVE".equals(action) && "APPROVED".equals(state)) return "RECEIVED";
        throw new ApiException(409, "Workflow transition is not allowed");
    }
    public static String salesNext(String state, String action) {
        if ("CONFIRM".equals(action) && "DRAFT".equals(state)) return "CONFIRMED";
        if ("FULFILL".equals(action) && "CONFIRMED".equals(state)) return "FULFILLED";
        throw new ApiException(409, "Workflow transition is not allowed");
    }
}
