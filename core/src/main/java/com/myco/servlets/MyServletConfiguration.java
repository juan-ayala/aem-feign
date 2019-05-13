package com.myco.servlets;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * @author Code &amp; Theory
 */
@ObjectClassDefinition
public @interface MyServletConfiguration {

    @AttributeDefinition String host();

    @AttributeDefinition String username();

    @AttributeDefinition(type = AttributeType.PASSWORD) String password();
}
