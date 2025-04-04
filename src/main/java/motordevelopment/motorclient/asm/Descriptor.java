/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.asm;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;

public class Descriptor {
    private final String[] components;

    public Descriptor(String... components) {
        this.components = components;
    }

    public String toString(boolean method, boolean map) {
        MappingResolver mappings = FabricLoader.getInstance().getMappingResolver();
        StringBuilder sb = new StringBuilder();

        if (method) sb.append('(');
        for (int i = 0; i < components.length; i++) {
            if (method && i == components.length - 1) sb.append(')');

            String component = components[i];

            if (map && component.startsWith("L") && component.endsWith(";")) {
                sb.append('L').append(mappings.mapClassName("intermediary", component.substring(1, component.length() - 1).replace('/', '.')).replace('.', '/')).append(';');
            }
            else sb.append(component);
        }

        return sb.toString();
    }
}
