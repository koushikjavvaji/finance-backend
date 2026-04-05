package com.financeapp.enums;

/**
 * Defines the three access levels in the system.
 *
 * VIEWER  → read-only dashboard access
 * ANALYST → read access + summary/insights
 * ADMIN   → full create / update / delete access
 */
public enum Role {
    VIEWER,
    ANALYST,
    ADMIN
}
