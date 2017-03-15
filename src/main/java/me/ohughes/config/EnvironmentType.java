package me.ohughes.config;

import lombok.Data;

/**
 * Created by ohughes on 3/15/17.
 */
public enum EnvironmentType {
    GIT("git"),
    VAULT("vault");

    private String type;

    EnvironmentType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
