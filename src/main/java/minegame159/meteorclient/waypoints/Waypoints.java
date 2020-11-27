/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.waypoints;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.GameLeftEvent;
import minegame159.meteorclient.events.GameJoinedEvent;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.rendering.Fonts;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.utils.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.nbt.CompoundTag;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.util.*;

public class Waypoints extends Savable<Waypoints> implements Listenable, Iterable<Waypoint> {
    public static final Map<String, AbstractTexture> ICONS = new HashMap<>();
    public static final Waypoints INSTANCE = new Waypoints();

    private static final String[] BUILTIN_ICONS = { "Square", "Circle", "Triangle", "Star", "Diamond" };

    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);

    private List<Waypoint> waypoints = new ArrayList<>();

    private Waypoints() {
        super(null);
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void add(Waypoint waypoint) {
        waypoints.add(waypoint);
        MeteorClient.EVENT_BUS.post(EventStore.waypointListChangedEvent());
        save();
    }

    public void remove(Waypoint waypoint) {
        if (waypoints.remove(waypoint)) {
            MeteorClient.EVENT_BUS.post(EventStore.waypointListChangedEvent());
            save();
        }
    }

    @EventHandler
    private final Listener<GameJoinedEvent> onGameJoined = new Listener<>(event -> load());

    @EventHandler
    private final Listener<GameLeftEvent> onGameDisconnected = new Listener<>(event -> {
        save();
        waypoints.clear();
    });

    private boolean checkDimension(Waypoint waypoint) {
        Dimension dimension = Utils.getDimension();

        if (waypoint.overworld && dimension == Dimension.Overworld) return true;
        if (waypoint.nether && dimension == Dimension.Nether) return true;
        return waypoint.end && dimension == Dimension.End;
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        for (Waypoint waypoint : this) {
            if (!waypoint.visible || !checkDimension(waypoint)) continue;

            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

            // Compute scale
            double dist = Utils.distanceToCamera(waypoint.x, waypoint.y, waypoint.z);
            if (dist > waypoint.maxVisibleDistance) continue;
            double scale = 0.04;
            if(dist > 10) scale *= dist / 10;

            double a = 1;
            if (dist < 10) {
                a = dist / 10;
                if (a < 0.1) continue;
            }

            int preBgA = BACKGROUND.a;
            int preTextA = TEXT.a;
            BACKGROUND.a *= a;
            TEXT.a *= a;

            double x = waypoint.x;
            double y = waypoint.y;
            double z = waypoint.z;

            double maxViewDist = MinecraftClient.getInstance().options.viewDistance * 16;
            if (dist > maxViewDist) {
                double dx = waypoint.x - camera.getPos().x;
                double dy = waypoint.y - camera.getPos().y;
                double dz = waypoint.z - camera.getPos().z;

                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                dx /= length;
                dy /= length;
                dz /= length;

                dx *= maxViewDist;
                dy *= maxViewDist;
                dz *= maxViewDist;

                x = camera.getPos().x + dx;
                y = camera.getPos().y + dy;
                z = camera.getPos().z + dz;

                scale /= dist / 15;
                scale *= maxViewDist / 15;
            }

            // Setup the rotation
            Matrices.push();
            Matrices.translate(x + 0.5 - event.offsetX, y - event.offsetY, z + 0.5 - event.offsetZ);
            Matrices.translate(0, -0.5 + waypoint.scale - 1, 0);
            Matrices.rotate(-camera.getYaw(), 0, 1, 0);
            Matrices.rotate(camera.getPitch(), 1, 0, 0);
            Matrices.translate(0, 0.5, 0);
            Matrices.scale(-scale * waypoint.scale, -scale * waypoint.scale, scale);

            String distText = Math.round(dist) + " blocks";

            // Render background
            double ii = Fonts.get(2).getWidth(waypoint.name) / 2.0;
            double i = ii * 0.25;
            double ii2 = Fonts.get(2).getWidth(distText) / 2.0;
            double i2 = ii2 * 0.25;
            ShapeBuilder.begin(null, GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
            ShapeBuilder.quad(-i - 1, -Fonts.get(2).getHeight() * 0.25 + 1, 0, -i - 1, 9 - Fonts.get(2).getHeight() * 0.25, 0, i + 1, 9 - Fonts.get(2).getHeight() * 0.25, 0, i + 1, -Fonts.get(2).getHeight() * 0.25 + 1, 0, BACKGROUND);
            ShapeBuilder.quad(-i2 - 1, 0, 0, -i2 - 1, 8, 0, i2 + 1, 8, 0, i2 + 1, 0, 0, BACKGROUND);
            ShapeBuilder.end();

            waypoint.renderIcon(-8, 9, 0, a, 16);

            // Render name text
            Matrices.scale(0.25, 0.25, 0.25);
            Fonts.get(2).begin();
            Fonts.get(2).render(waypoint.name, -ii, -Fonts.get(2).getHeight() + 1, TEXT);
            Fonts.get(2).render(distText, -ii2, 0, TEXT);
            Fonts.get(2).end();

            Matrices.pop();

            BACKGROUND.a = preBgA;
            TEXT.a = preTextA;
        }
    });

    @Override
    public File getFile() {
        return new File(new File(MeteorClient.FOLDER, "waypoints"), Utils.getWorldName() + ".nbt");
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("waypoints", NbtUtils.listToTag(waypoints));
        return tag;
    }

    @Override
    public Waypoints fromTag(CompoundTag tag) {
        waypoints = NbtUtils.listFromTag(tag.getList("waypoints", 10), tag1 -> new Waypoint().fromTag((CompoundTag) tag1));

        return this;
    }

    @Override
    public Iterator<Waypoint> iterator() {
        return waypoints.iterator();
    }

    public static void loadIcons() {
        File iconsFolder = new File(new File(MeteorClient.FOLDER, "waypoints"), "icons");
        iconsFolder.mkdirs();

        for (String builtinIcon : BUILTIN_ICONS) {
            File iconFile = new File(iconsFolder, builtinIcon + ".png");
            if (!iconFile.exists()) copyIcon(iconFile);
        }

        File[] files = iconsFolder.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".png")) {
                try {
                    String name = file.getName().replace(".png", "");
                    AbstractTexture texture = new NativeImageBackedTexture(NativeImage.read(new FileInputStream(file)));
                    ICONS.put(name, texture);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void copyIcon(File file) {
        try {
            InputStream in = Waypoints.class.getResourceAsStream("/assets/meteor-client/waypoint-icons/" + file.getName());
            OutputStream out = new FileOutputStream(file);

            byte[] bytes = new byte[256];
            int read;
            while ((read = in.read(bytes)) > 0) out.write(bytes, 0, read);

            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
