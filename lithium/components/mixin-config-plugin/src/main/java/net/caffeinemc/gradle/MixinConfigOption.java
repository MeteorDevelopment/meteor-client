package net.caffeinemc.gradle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
//RUNTIME is required due to the current method of accessing the annotation by loading the class
@Target(ElementType.PACKAGE)
public @interface MixinConfigOption {
    boolean enabled() default true;

    MixinConfigDependency[] depends() default {};

    String description();
}
