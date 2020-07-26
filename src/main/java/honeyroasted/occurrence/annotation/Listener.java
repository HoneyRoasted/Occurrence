package honeyroasted.occurrence.annotation;

import honeyroasted.occurrence.EventPriority;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Filter(id = "listener", args = {@Arg(name = "priority", delegate = "priority"), @Arg(name = "event", delegate = "event")})
public @interface Listener {

    Class<?> event() default Object.class;

    int priority() default EventPriority.NEUTRAL;


}
