package net.caffeinemc.gradle;

public @interface MixinConfigDependency {
    String dependencyPath();

    boolean enabled() default true;
}
