package meteordevelopment.meteorclient.systems.modules.world;

import org.joml.Vector3d;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

import meteordevelopment.meteorclient.utils.player.PlayerUtils;

public class Telekinesis extends Module {
   private final SettingGroup sgGeneral = settings.getDefaultGroup();
   private final SettingGroup sgBounds = settings.createGroup("Bounds");
    
   public final Setting<Vector3d> velocity = sgGeneral.add(new Vector3dSetting.Builder()
        .name("velocity")
        .description("Entites velocity")
        .defaultValue(0, 0, 0)
        .build()
    );

   public final Setting<Set<EntityType<?>>> entities = sgBounds.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Select specific entities.")
        .build()
    );

    private final Setting<Double> fov = sgBounds.add(new DoubleSetting.Builder()
       .name("fov")
       .description("Will only move entities in the fov.")
       .build()
    );

    private final Setting<Double> range = sgBounds.add(new DoubleSetting.Builder()
       .name("range")
       .description("The range at which an entity can be moved.")
       .build()
    );

    private final Setting<Filter> filter = sgBounds.add(new EnumSetting.Builder<Filter>()
        .name("filter")
        .description("Entity filter settings.")
        .defaultValue(Filter.Black)
        .build()
    );

    private final Setting<List<String>> whiteuuids = sgBounds.add(new StringListSetting.Builder()
        .name("white-uuids")
        .description("Filter entities by uuid")
        .visible(() -> filter.get() == Filter.White)
        .build()
    );

    private final Setting<List<String>> blackuuids = sgBounds.add(new StringListSetting.Builder()
        .name("black-uuids")
        .description("Filter entities by uuid")
        .visible(() -> filter.get() == Filter.Black)
        .build()
    );

    public Telekinesis()
    {
        super(Categories.World, "telekinesis", "Move entities in third axis.");
    }

    public boolean inList(Entity entity)
    {
        return isActive() && inFrames(entity);
    }

    public boolean inFrames(Entity entity)
    {
        return switch(filter.get()) {case White -> {entities.get().contains(entity.getType());} case Black -> {!entities.get().contains(entity.getType());}};
    }

    public enum Filter
    {
        White,
        Black
    }	
}
