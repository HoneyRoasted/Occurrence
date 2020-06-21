package honeyroasted.occurrence.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface FilterList {

    Filter[] value() default {};

}
