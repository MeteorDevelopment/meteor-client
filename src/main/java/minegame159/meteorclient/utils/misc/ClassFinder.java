/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.misc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassFinder {
    public static List<Class<?>> findSubTypesOf(String packageName, Class<?> klass) {
        List<Class<?>> classes = find(packageName);

        classes.removeIf(aClass -> !klass.isAssignableFrom(aClass) || aClass == klass);

        return classes;
    }

    public static List<Class<?>> find(String packageName) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(packageName.replace('.', '/'));

        if (url == null) {
            throw new IllegalArgumentException(String.format("Unable to get resources from path '%s'.", packageName));
        }

        List<Class<?>> classes = new ArrayList<>();

        if (url.getProtocol().equals("jar")) {
            try {
                String jarFileName = URLDecoder.decode(url.getFile(), "UTF-8");
                jarFileName = jarFileName.substring(5, jarFileName.indexOf('!'));

                JarFile jarFile = new JarFile(jarFileName);
                for (Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements();) {
                    String name = it.nextElement().getName().replace('/', '.');

                    if (name.startsWith(packageName) && name.endsWith(".class")) {
                        try {
                            classes.add(Class.forName(name.substring(0, name.length() - 6)));
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            for (File child : new File(url.getFile()).listFiles()) {
                find(classes, child, packageName);
            }
        }

        return classes;
    }

    private static void find(List<Class<?>> classes, File file, String packageName) {
        String resource = packageName + "." + file.getName();

        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                find(classes, child, resource);
            }
        }
        else if (resource.endsWith(".class")) {
            String className = resource.substring(0, resource.length() - 6);

            try {
                classes.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
