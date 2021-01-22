/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.waypoints;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.game.GameJoinedEvent;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.meteor.WaypointListChangedEvent;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.MeshBuilder;
import minegame159.meteorclient.rendering.text.TextRenderer;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.files.Savable;
import minegame159.meteorclient.utils.misc.NbtUtils;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.world.Dimension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.util.*;

public class Waypoints extends Savable<Waypoints> implements Listenable, Iterable<Waypoint> {
    public static final Map<String, AbstractTexture> ICONS = new HashMap<>();
    public static final Waypoints INSTANCE = new Waypoints();

    private static final String[] BUILTIN_ICONS = { "Square", "Circle", "Triangle", "Star", "Diamond" };

    private static final MeshBuilder MB = new MeshBuilder(128);

    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);

    private List<Waypoint> waypoints = new ArrayList<>();

    private Waypoints() {
        super(null);
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void add(Waypoint waypoint) {
        waypoints.add(waypoint);
        MeteorClient.EVENT_BUS.post(WaypointListChangedEvent.get());
        save();
    }

    public void remove(Waypoint waypoint) {
        if (waypoints.remove(waypoint)) {
            MeteorClient.EVENT_BUS.post(WaypointListChangedEvent.get());
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

    public Vec3d getCoords(Waypoint waypoint) {

        double x = waypoint.x;
        double y = waypoint.y;
        double z = waypoint.z;

        if (waypoint.actualDimension == Dimension.Overworld && Utils.getDimension() == Dimension.Nether) {
            x = waypoint.x / 8;
            z = waypoint.z / 8;
        } else if (waypoint.actualDimension == Dimension.Nether && Utils.getDimension() == Dimension.Overworld) {
            x = waypoint.x * 8;
            z = waypoint.z * 8;
        }

        return new Vec3d(x, y, z);
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        for (Waypoint waypoint : this) {
            if (!waypoint.visible || !checkDimension(waypoint)) continue;

            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

            double x = getCoords(waypoint).x;
            double y = getCoords(waypoint).y;
            double z = getCoords(waypoint).z;

            // Compute scale
            double dist = Utils.distanceToCamera(x, y, z);
            if (dist > waypoint.maxVisibleDistance) continue;
            double scale = 0.01 * waypoint.scale;
            if(dist > 8) scale *= dist / 8;

            double a = 1;
            if (dist < 10) {
                a = dist / 10;
                if (a < 0.1) continue;
            }

            int preBgA = BACKGROUND.a;
            int preTextA = TEXT.a;
            BACKGROUND.a *= a;
            TEXT.a *= a;

            double maxViewDist = MinecraftClient.getInstance().options.viewDistance * 16;
            if (dist > maxViewDist) {
                double dx = x - camera.getPos().x;
                double dy = y - camera.getPos().y;
                double dz = z - camera.getPos().z;

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
            Matrices.scale(-scale, -scale, scale);

            String distText = Math.round(dist) + " blocks";

            // Render background
            TextRenderer.get().begin(1, false, true);
            double w = TextRenderer.get().getWidth(waypoint.name) / 2.0;
            double w2 = TextRenderer.get().getWidth(distText) / 2.0;
            double h = TextRenderer.get().getHeight();

            // TODO: I HATE EVERYTHING ABOUT HOW RENDERING ROTATING THINGS WORKS AND I CANNOT BE ASKED TO WORK THIS OUT, THE WHOLE THING NEEDS TO BE RECODED REEEEEEEEEEEEEEEEEEEE
            /*MB.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
            MB.quad(-w - 1, -h + 1, 0, -w - 1, 9 - h, 0, w + 1, 9 - h, 0, w + 1, -h + 1, 0, BACKGROUND);
            MB.quad(-w2 - 1, 0, 0, -w2 - 1, 8, 0, w2 + 1, 8, 0, w2 + 1, 0, 0, BACKGROUND);
            MB.end();*/

            waypoint.renderIcon(-8, h, 0, a, 16);

            // Render name text
            TextRenderer.get().render(waypoint.name, -w, -h + 1, TEXT);
            TextRenderer.get().render(distText, -w2, 0, TEXT);

            TextRenderer.get().end();
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
