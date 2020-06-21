package honeyroasted.occurrence.annotation;

import honeyroasted.occurrence.EventPriority;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Listener {

    int priority() default EventPriority.NEUTRAL;

}
