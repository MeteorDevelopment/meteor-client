/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.compat;

import java.io.InputStream;
import java.util.Properties;

/**
 * Entry point of the Minecraft compatibility layer.
 *
 * <p>The active {@link ICompat} implementation is resolved reflectively so that the common code
 * never has a compile-time dependency on a specific Minecraft version. The implementation is
 * looked up in the following order:</p>
 *
 * <ol>
 *     <li>the {@code meteor.compat.impl} system property (useful for tests),</li>
 *     <li>the {@code impl} key of the {@code /meteor-compat.properties} resource (overridden at
 *         build time for each supported Minecraft version),</li>
 *     <li>{@link #DEFAULT_IMPL}.</li>
 * </ol>
 */
public final class Compat {
    private static final String DEFAULT_IMPL = "meteordevelopment.meteorclient.compat.impl.Mc262Compat";
    private static final String RESOURCE = "/meteor-compat.properties";

    private static ICompat impl;

    private Compat() {
    }

    /** Loads the implementation for the running Minecraft version. Safe to call multiple times. */
    public static void init() {
        if (impl != null) return;

        String className = resolveImplClassName();

        try {
            impl = (ICompat) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Minecraft compatibility implementation: " + className, e);
        }
    }

    /** The active implementation. Initializes it on first use if it was not initialized yet. */
    public static ICompat get() {
        if (impl == null) init();
        return impl;
    }

    private static String resolveImplClassName() {
        String property = System.getProperty("meteor.compat.impl");
        if (property != null && !property.isBlank()) return property;

        try (InputStream in = Compat.class.getResourceAsStream(RESOURCE)) {
            if (in != null) {
                Properties properties = new Properties();
                properties.load(in);

                String value = properties.getProperty("impl");
                if (value != null && !value.isBlank()) return value.trim();
            }
        } catch (Exception ignored) {
        }

        return DEFAULT_IMPL;
    }
}
