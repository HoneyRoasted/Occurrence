package honeyroasted.occurrence.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Arg {

    String name() default "value";

    boolean forceArray() default false;

    String delegate() default Filter.DEFAULT;

    String expand() default Filter.DEFAULT;

    boolean[] boolVal() default {};

    byte[] byteVal() default {};

    short[] shortVal() default {};

    char[] charVal() default {};

    int[] intVal() default {};

    long[] longVal() default {};

    float[] floatVal() default {};

    double[] doubleVal() default {};

    String[] stringVal() default {};

    Class<?>[] classVal() default {};

    Enum enumVal() default @Enum(type = java.lang.Enum.class);

    @interface Enum {

        Class<? extends java.lang.Enum> type();

        String[] val() default {};

    }

}
