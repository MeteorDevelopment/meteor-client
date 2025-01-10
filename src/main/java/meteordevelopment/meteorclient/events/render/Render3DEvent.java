/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.utils.Utils;
import org.joml.Matrix4f;
import org.meteordev.juno.api.commands.CommandList;
import org.meteordev.juno.api.commands.RenderPass;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Render3DEvent {
    private static final Render3DEvent INSTANCE = new Render3DEvent();

    public CommandList commandList;
    public Matrix4f projection;
    public Matrix4f view;
    public Matrix4f projectionView;

    public Renderer3D renderer;

    public float tickDelta;
    public double frameTime;

    public final List<Consumer<RenderPass>> renderToDefaultPass = new ArrayList<>();

    public static Render3DEvent get(CommandList commandList, Matrix4f projection, Matrix4f view, Renderer3D renderer, float tickDelta) {
        INSTANCE.commandList = commandList;
        INSTANCE.projection = projection;
        INSTANCE.view = view;
        INSTANCE.projectionView = new Matrix4f(projection).mul(view);

        INSTANCE.renderer = renderer;

        INSTANCE.tickDelta = tickDelta;
        INSTANCE.frameTime = Utils.frameTime;

        INSTANCE.renderToDefaultPass.clear();

        return INSTANCE;
    }
}
