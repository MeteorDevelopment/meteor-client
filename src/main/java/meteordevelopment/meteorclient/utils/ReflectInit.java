/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ReflectInit {
    private static final List<Reflections> reflections = new ArrayList<>();

    public static void registerPackages() {
        add(MeteorClient.ADDON);

        for (MeteorAddon addon : AddonManager.ADDONS) {
            try {
                add(addon);
            } catch (AbstractMethodError e) {
                throw new RuntimeException("Addon \"%s\" is too old and cannot be ran.".formatted(addon.name), e);
            }
        }
    }

    private static void add(MeteorAddon addon) {
        String pkg = addon.getPackage();
        if (pkg == null || pkg.isBlank()) return;
        reflections.add(new Reflections(pkg, Scanners.MethodsAnnotated));
    }

    public static void init(Class<? extends Annotation> annotation) {
        for (Reflections reflection : reflections) {
            Set<Method> initTasks = reflection.getMethodsAnnotatedWith(annotation);
            if (initTasks == null) return;

            Map<Class<?>, List<Method>> byClass = initTasks.stream().collect(Collectors.groupingBy(Method::getDeclaringClass));
            Set<Method> left = new HashSet<>(initTasks);

            for (Method m; (m = left.stream().findAny().orElse(null)) != null;) {
                reflectInit(m, annotation, left, byClass);
            }
        }
    }

    private static <T extends Annotation> void reflectInit(Method task, Class<T> annotation, Set<Method> left, Map<Class<?>, List<Method>> byClass) {
        left.remove(task);

        for (Class<?> clazz : getDependencies(task, annotation)) {
            for (Method m : byClass.getOrDefault(clazz, Collections.emptyList())) {
                if (left.contains(m)) {
                    reflectInit(m, annotation, left, byClass);
                }
            }
        }

        try {
            task.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            throw new RuntimeException("Method \"%s\" using Init annotations from non-static context".formatted(task.getName()), e);
        }
    }

    private static <T extends Annotation> Class<?>[] getDependencies(Method task, Class<T> annotation) {
        T init = task.getAnnotation(annotation);

        if (init instanceof PreInit pre) {
            return pre.dependencies();
        }
        else if (init instanceof PostInit post) {
            return post.dependencies();
        }

        return new Class<?>[]{};
    }
}
