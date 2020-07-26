package honeyroasted.occurrence.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Delegate {

    Class<? extends Annotation> type();

    String target();

}
