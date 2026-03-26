/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.pathing;

import baritone.api.BaritoneAPI;
import baritone.api.process.IMineProcess;
import net.minecraft.world.level.block.Block;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.StreamSupport;

public class BaritoneUtils {
    public static boolean IS_AVAILABLE = false;

    private BaritoneUtils() {
    }

    public static String getPrefix() {
        if (IS_AVAILABLE) {
            return BaritoneAPI.getSettings().prefix.value;
        }

        return "";
    }

    public static void mineByBlockIds(IMineProcess mineProcess, Iterable<Block> blocks) {
        String[] blockIds = StreamSupport.stream(blocks.spliterator(), false)
            .map(block -> block.builtInRegistryHolder().key().identifier().toString())
            .toArray(String[]::new);

        invokeLookupMethod(mineProcess, "mine", new Class<?>[] { int.class }, new Object[] { 0 }, blockIds);
    }

    public static List<?> scanChunkRadiusByBlockIds(Object worldScanner, Object playerContext, Iterable<Block> blocks, int max, int yLevelThreshold, int maxSearchRadius) {
        String[] blockIds = StreamSupport.stream(blocks.spliterator(), false)
            .map(block -> block.builtInRegistryHolder().key().identifier().toString())
            .toArray(String[]::new);

        try {
            Class<?> lookupClass = Class.forName("baritone.api.utils.BlockOptionalMetaLookup");
            Constructor<?> constructor = lookupClass.getConstructor(String[].class);
            Object lookup = constructor.newInstance((Object) blockIds);

            Method scanChunkRadius = worldScanner.getClass().getMethod("scanChunkRadius", playerContext.getClass().getInterfaces()[0], lookupClass, int.class, int.class, int.class);
            return (List<?>) scanChunkRadius.invoke(worldScanner, playerContext, lookup, max, yLevelThreshold, maxSearchRadius);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke Baritone world scanner.", e);
        }
    }

    public static void clearArea(Object builderProcess, Object start, Object end) {
        try {
            Method clearArea = findCompatibleMethod(builderProcess.getClass(), "clearArea", start, end);
            clearArea.invoke(builderProcess, start, end);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke Baritone builder process.", e);
        }
    }

    private static void invokeLookupMethod(Object target, String methodName, Class<?>[] prefixParameterTypes, Object[] prefixArgs, String[] blockIds) {
        try {
            Class<?> lookupClass = Class.forName("baritone.api.utils.BlockOptionalMetaLookup");
            Constructor<?> constructor = lookupClass.getConstructor(String[].class);
            Object lookup = constructor.newInstance((Object) blockIds);

            Class<?>[] parameterTypes = new Class<?>[prefixParameterTypes.length + 1];
            Object[] args = new Object[prefixArgs.length + 1];

            System.arraycopy(prefixParameterTypes, 0, parameterTypes, 0, prefixParameterTypes.length);
            System.arraycopy(prefixArgs, 0, args, 0, prefixArgs.length);

            parameterTypes[prefixParameterTypes.length] = lookupClass;
            args[prefixArgs.length] = lookup;

            Method method = target.getClass().getMethod(methodName, parameterTypes);
            method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke Baritone lookup method.", e);
        }
    }

    private static Method findCompatibleMethod(Class<?> owner, String methodName, Object... args) throws NoSuchMethodException {
        for (Method method : owner.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) continue;
            if (!Modifier.isPublic(method.getModifiers())) continue;

            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean compatible = true;

            for (int i = 0; i < parameterTypes.length; i++) {
                if (args[i] != null && !parameterTypes[i].isInstance(args[i])) {
                    compatible = false;
                    break;
                }
            }

            if (compatible) return method;
        }

        throw new NoSuchMethodException(owner.getName() + "#" + methodName);
    }
}
