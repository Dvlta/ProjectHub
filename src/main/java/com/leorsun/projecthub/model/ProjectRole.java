package com.leorsun.projecthub.model;

public enum ProjectRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER;

    public boolean atLeast(ProjectRole other) {
        return this.ordinal() <= other.ordinal();
    }
}

