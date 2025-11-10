/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ReflectInit {
    private static final List<String> packages = new ArrayList<>();

    private ReflectInit() {
    }

    public static void registerPackages() {
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
        packages.add(pkg);
    }

    public static void init(Class<? extends Annotation> annotation) {
        for (String pkg : packages) {
            try (ScanResult scanResult = new ClassGraph()
                .acceptPackages(pkg)
                .enableMethodInfo()
                .enableAnnotationInfo()
                .scan()) {

                Set<Method> initTasks = scanResult.getClassesWithMethodAnnotation(annotation)
                    .stream()
                    .flatMap(classInfo -> classInfo.getMethodInfo().stream())
                    .filter(methodInfo -> methodInfo.hasAnnotation(annotation))
                    .map(MethodInfo::loadClassAndGetMethod)
                    .collect(Collectors.toSet());

                if (initTasks.isEmpty()) continue;

                Map<Class<?>, List<Method>> byClass = initTasks.stream().collect(Collectors.groupingBy(Method::getDeclaringClass));
                Set<Method> left = new HashSet<>(initTasks);

                for (Method m; (m = left.stream().findAny().orElse(null)) != null; ) {
                    reflectInit(m, annotation, left, byClass);
                }
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
            throw new IllegalStateException("Error running @%s task '%s.%s'".formatted(annotation.getSimpleName(), task.getDeclaringClass().getSimpleName(), task.getName()), e);
        } catch (NullPointerException e) {
            throw new RuntimeException("Method \"%s\" using Init annotations from non-static context".formatted(task.getName()), e);
        }
    }

    private static <T extends Annotation> Class<?>[] getDependencies(Method task, Class<T> annotation) {
        T init = task.getAnnotation(annotation);

        return switch (init) {
            case PreInit pre -> pre.dependencies();
            case PostInit post -> post.dependencies();
            default -> new Class<?>[]{};
        };
    }
}
