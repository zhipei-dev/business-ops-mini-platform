package dev.zhipei.businessops.domain;

import static org.junit.jupiter.api.Assertions.*;
import dev.zhipei.businessops.api.ApiException;
import org.junit.jupiter.api.Test;

class WorkflowTests {
    @Test void purchaseHasOnlyLegalTransitions() { assertEquals("SUBMITTED", Workflow.purchaseNext("DRAFT", "SUBMIT")); assertEquals("APPROVED", Workflow.purchaseNext("SUBMITTED", "APPROVE")); assertEquals("RECEIVED", Workflow.purchaseNext("APPROVED", "RECEIVE")); assertThrows(ApiException.class, () -> Workflow.purchaseNext("DRAFT", "RECEIVE")); }
    @Test void salesHasOnlyLegalTransitions() { assertEquals("CONFIRMED", Workflow.salesNext("DRAFT", "CONFIRM")); assertEquals("FULFILLED", Workflow.salesNext("CONFIRMED", "FULFILL")); assertThrows(ApiException.class, () -> Workflow.salesNext("FULFILLED", "FULFILL")); }
}
