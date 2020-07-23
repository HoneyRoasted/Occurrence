package honeyroasted.occurrence.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public @interface Filters {

    @Retention(RetentionPolicy.RUNTIME)
    @Filter(id = "default")
    @interface Default { }

    @Retention(RetentionPolicy.RUNTIME)
    @Filter(id = "invoke", args = {@Arg(delegate = "value"), @Arg(expand = "args")})
    @interface Invoke {
        String value() default Filter.DEFAULT;
        Arg[] args() default {};


        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "invoke.predicate", args = {@Arg(delegate = "value"), @Arg(expand = "args")})
        @interface Predicate {
            String value() default Filter.DEFAULT;
            Arg[] args() default {};
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "invoke.new", args = {@Arg(delegate = "value"), @Arg(expand = "args")})
        @interface New {
            Class<?> value() default Object.class;
            Arg[] args() default {};
        }
    }

    @interface Listener {

        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "listener.invoke", args = {@Arg(delegate = "value"), @Arg(expand = "args")})
        @interface Invoke {
            String value() default Filter.DEFAULT;
            Arg[] args() default {};


            @Retention(RetentionPolicy.RUNTIME)
            @Filter(id = "listener.invoke.predicate", args = {@Arg(delegate = "value"), @Arg(expand = "args")})
            @interface Predicate {
                String value() default Filter.DEFAULT;
                Arg[] args() default {};
            }
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Filter(id = "include", args = @Arg(delegate = "value"))
    @interface Include { Class[] value() default {}; }

    @Retention(RetentionPolicy.RUNTIME)
    @Filter(id = "exclude", args = @Arg(delegate = "value"))
    @interface Exclude { Class[] value() default {}; }

    @Retention(RetentionPolicy.RUNTIME)
    @Filter(id = "cancelled", args = @Arg(delegate = "value"))
    @interface Cancelled { boolean value() default true; }

    @Retention(RetentionPolicy.RUNTIME)
    @Filter(id = "nonnull", args = @Arg(delegate = "value"))
    @interface Nonnull { boolean value() default true; }

    @Retention(RetentionPolicy.RUNTIME)
    @Filter(id = "equal", args = @Arg(delegate = "value"))
    @interface Equal { Arg value() default @Arg; }

    @Retention(RetentionPolicy.SOURCE)
    @interface Optional {

        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "invoke", args = @Arg(stringVal = "get"))
        @interface Get { }

        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "invoke.predicate", args = @Arg(stringVal = "isPresent"))
        @interface IsPresent { }

        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "invoke", args = {@Arg(stringVal = "ofNullable"), @Arg(name = "source", classVal = java.util.Optional.class)})
        @interface Wrap { }

    }

    @Retention(RetentionPolicy.SOURCE)
    @interface Iterable {

        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "iterable.first", args = @Arg(delegate = "value"))
        @interface First { Class[] value() default Object.class; }

        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "iterable.last", args = @Arg(delegate = "value"))
        @interface Last { Class[] value() default Object.class; }

        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "iterable.all", args = @Arg(delegate = "value"))
        @interface All { Class[] value() default Object.class; }

    }

    @Retention(RetentionPolicy.SOURCE)
    @interface Map {

        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "invoke", args = @Arg(stringVal = "values"))
        @Filter(id = "iterable.first", args = @Arg(delegate = "value"))
        @interface First { Class[] value() default Object.class; }

        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "invoke", args = @Arg(stringVal = "values"))
        @Filter(id = "iterable.last", args = @Arg(delegate = "value"))
        @interface Last { Class[] value() default Object.class; }

        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "invoke", args = @Arg(stringVal = "values"))
        @Filter(id = "iterable.all", args = @Arg(delegate = "value"))
        @interface All { Class[] value() default Object.class; }

        @Retention(RetentionPolicy.RUNTIME)
        @Filter(id = "invoke", args = {@Arg(stringVal = "get"), @Arg(name = "0", delegate = "value")})
        @interface Get { String value(); }

    }

}
