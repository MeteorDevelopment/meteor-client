package meteordevelopment.meteorclient.systems.modules.donut;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.core.BlockPos;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Saves base-hunting finds per server and dimension so they survive relogging. */
public final class FindsStorage {
    public static final class Find {
        public String type;
        public String dimension;
        public String note;
        public int x, y, z;
        public long time;
    }

    /** Captured on the render thread; scanning threads must not read world state themselves. */
    public record Context(String world, String dimension) {
        public boolean valid() {
            return world != null && !world.isEmpty() && dimension != null && !dimension.isEmpty();
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Find>>() {}.getType();
    private static final File FOLDER = new File(MeteorClient.FOLDER, "ember-finds");

    private static String loadedWorld;
    private static final List<Find> finds = new ArrayList<>();

    private FindsStorage() {
    }

    public static Context context() {
        try {
            if (mc.level == null) return new Context("", "");
            return new Context(Utils.getFileWorldName(), PlayerUtils.getDimension().name());
        } catch (Exception e) {
            return new Context("", "");
        }
    }

    public static synchronized List<Find> get(Context ctx, String type) {
        List<Find> out = new ArrayList<>();
        if (!ctx.valid()) return out;

        ensureLoaded(ctx.world());
        for (Find find : finds) {
            if (find.type.equals(type) && find.dimension.equals(ctx.dimension())) out.add(find);
        }
        return out;
    }

    /** @return false if this exact find was already saved */
    public static synchronized boolean add(Context ctx, String type, BlockPos pos, String note) {
        if (!ctx.valid()) return false;

        ensureLoaded(ctx.world());
        for (Find find : finds) {
            if (find.type.equals(type) && find.dimension.equals(ctx.dimension())
                && find.x == pos.getX() && find.y == pos.getY() && find.z == pos.getZ()) {
                return false;
            }
        }

        Find find = new Find();
        find.type = type;
        find.dimension = ctx.dimension();
        find.note = note;
        find.x = pos.getX();
        find.y = pos.getY();
        find.z = pos.getZ();
        find.time = System.currentTimeMillis();
        finds.add(find);

        save(ctx.world());
        return true;
    }

    /** Adds a waypoint unless the same one already exists. Must run on the render thread. */
    public static void waypoint(String name, BlockPos pos) {
        for (Waypoint waypoint : Waypoints.get()) {
            if (waypoint.name.get().equals(name) && waypoint.getPos().equals(pos)) return;
        }

        Waypoints.get().add(new Waypoint.Builder()
            .name(name)
            .icon("Square")
            .pos(pos)
            .dimension(PlayerUtils.getDimension())
            .build());
    }

    private static void ensureLoaded(String world) {
        if (world.equals(loadedWorld)) return;

        loadedWorld = world;
        finds.clear();

        File file = new File(FOLDER, world + ".json");
        if (!file.isFile()) return;

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            List<Find> loaded = GSON.fromJson(reader, LIST_TYPE);
            if (loaded == null) return;

            for (Find find : loaded) {
                if (find != null && find.type != null && find.dimension != null) finds.add(find);
            }
        } catch (Exception e) {
            MeteorClient.LOG.warn("Could not read saved finds for {}", world, e);
        }
    }

    private static void save(String world) {
        try {
            Files.createDirectories(FOLDER.toPath());
            File file = new File(FOLDER, world + ".json");

            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(finds, LIST_TYPE, writer);
            }
        } catch (IOException e) {
            MeteorClient.LOG.warn("Could not save finds for {}", world, e);
        }
    }
}
