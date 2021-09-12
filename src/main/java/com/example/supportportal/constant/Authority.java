package com.example.supportportal.constant;

public class Authority {
    public static final String[] USER_AUTHORITIES = {"user:read"};
    public static final String[] HR_AUTHORITIES = {"user:read", "user:update"};
    public static final String[] MGR_AUTHORITIES = {"user:read", "user:update"};
    public static final String[] ADMIN_AUTHORITIES = {"user:read", "user:create", "user:update"};
    public static final String[] SU_AUTHORITIES = {"user:read", "user:create", "user:update", "user:delete"};
}
