/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.rendering.gl;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.lwjgl.opengl.GL30C.*;

public class Shader {
    private int id;
    private final Object2IntMap<String> locations = new Object2IntOpenHashMap<>();

    public Shader(String vertexPath, String fragmentPath) {
        // Vertex
        int vertex = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertex, read(vertexPath));
        glCompileShader(vertex);

        boolean compiled = glGetShaderi(vertex, GL_COMPILE_STATUS) == 1;
        if (!compiled) {
            String error = glGetShaderInfoLog(vertex);
            MeteorClient.LOG.error("Failed to compile vertex shader ({}):\n{}", vertexPath, error);
            return;
        }

        // Fragment
        int fragment = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragment, read(fragmentPath));
        glCompileShader(fragment);

        compiled = glGetShaderi(fragment, GL_COMPILE_STATUS) == 1;
        if (!compiled) {
            String error = glGetShaderInfoLog(fragment);
            MeteorClient.LOG.error("Failed to compile fragment shader ({}):\n{}", fragmentPath, error);
            return;
        }

        // Program
        id = glCreateProgram();
        glAttachShader(id, vertex);
        glAttachShader(id, fragment);
        glLinkProgram(id);

        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    public void bind() {
        glUseProgram(id);
    }

    public void unbind() {
        glUseProgram(0);
    }

    private int getLocation(String name) {
        if (!locations.containsKey(name)) {
            int location = glGetUniformLocation(id, name);
            locations.put(name, location);
            return location;
        }

        return locations.getInt(name);
    }

    public void set(String name, int v) {
        glUniform1i(getLocation(name), v);
    }

    public void set(String name, float v) {
        glUniform1f(getLocation(name), v);
    }

    public void set(String name, float v1, float v2) {
        glUniform2f(getLocation(name), v1, v2);
    }

    public void set(String name, float v1, float v2, float v3, float v4) {
        glUniform4f(getLocation(name), v1, v2, v3, v4);
    }

    public void set(String name, Color color) {
        set(name, color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f);
    }

    private String read(String path) {
        try {
            InputStream in = MinecraftClient.getInstance().getResourceManager().getResource(new Identifier("meteor-client", path)).getInputStream();
            StringBuilder sb = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                reader.lines().forEach(s -> sb.append(s).append('\n'));
            }

            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }
}
