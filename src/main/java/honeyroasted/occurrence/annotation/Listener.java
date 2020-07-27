package honeyroasted.occurrence.annotation;

import honeyroasted.occurrence.EventPriority;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Filter(id = "listener", args = {@Arg(name = "priority", delegate = "priority"), @Arg(name = "event", delegate = "event")})
@Filter(id = "cancelled", args = @Arg(delegate = "acceptCancelled"))
@Filter(id = "include", args = @Arg(delegate = "event"))
public @interface Listener {

    Class<?>[] event() default Object.class;

    int priority() default EventPriority.NEUTRAL;

    Tristate acceptCancelled() default Tristate.FALSE;


}
