package honeyroasted.occurrence.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Utility {
    String USE_METHOD = "<use_method>";

    @Retention(RetentionPolicy.RUNTIME)
    @interface Function { String value() default USE_METHOD; }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Predicate { String value() default USE_METHOD; }


}
