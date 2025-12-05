package com.example.employee.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Roles {
    ROLE_EMPLOYEE,
    ROLE_ADMIN,
    ROLE_HR,
    ROLE_TEAM_LEAD,
    ROLE_MANAGER;

    @JsonCreator
    public static Roles from(String value) {
        return Roles.valueOf(value.toUpperCase()); 
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }



}
