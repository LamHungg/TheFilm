package vn.thefilm.backend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Restricts a controller operation to the authenticated user's effective cinema assignments. */
@Documented
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireCinemaScope {

    /** Parameter name or request parameter containing the target cinema ID. */
    String value() default "cinemaId";
}
