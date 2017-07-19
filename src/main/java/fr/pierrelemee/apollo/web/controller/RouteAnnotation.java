package fr.pierrelemee.apollo.web.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD) //on class level
public @interface RouteAnnotation {

    String uri() default "/";

    String method() default "GET";

    String[] parameters() default {};

}
