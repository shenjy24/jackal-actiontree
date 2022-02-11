package com.jonas.action;

/**
 * @author jonas
 * @createTime 2021/4/7 17:46
 */
public enum TreeScope {
    ONCE("once"),
    REPEATED("repeated"),
    NOT_RESET_REPEATED("not_reset_repeated");

    private String scope;

    TreeScope(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    public boolean equals(String scope) {
        return this.scope.equalsIgnoreCase(scope);
    }
}
