package com.myco.servlets;

import java.util.List;

import feign.RequestLine;

/**
 * @author Code &amp; Theory
 */
interface ItemService {

    @RequestLine("GET /list-items")
    List<Item> listItems();
}
