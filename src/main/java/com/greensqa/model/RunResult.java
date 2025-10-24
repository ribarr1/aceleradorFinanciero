package com.greensqa.model;

public class RunResult {
    public final String id;
    public final String variable;
    public final String status;   // PASS/FAIL/ERROR
    public final Object actual;
    public final Object expected;
    public final String reason;

    public RunResult(String id, String variable, String status, Object actual, Object expected, String reason) {
        this.id = id; this.variable = variable; this.status = status;
        this.actual = actual; this.expected = expected; this.reason = reason;
    }
}
