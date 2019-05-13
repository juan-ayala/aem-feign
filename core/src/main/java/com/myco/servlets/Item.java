package com.myco.servlets;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Code &amp; Theory
 */
final class Item {

    @JsonProperty
    private String name;

    public String getName() {

        return this.name;
    }
}
