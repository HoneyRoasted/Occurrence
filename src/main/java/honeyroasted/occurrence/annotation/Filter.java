package honeyroasted.occurrence.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FilterList.class)
public @interface Filter {
    String DEFAULT = "<default>";

    String id();

    Arg[] args() default {};

}
