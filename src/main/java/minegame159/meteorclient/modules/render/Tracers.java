package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.BoolSettingBuilder;
import minegame159.meteorclient.settings.builders.ColorSettingBuilder;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.EntityUtils;
import minegame159.meteorclient.utils.RenderUtils;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Tracers extends Module {
    private Setting<Boolean> center = addSetting(new BoolSettingBuilder()
            .name("center")
            .description("Tracers go to the center of the entity instead of its head.")
            .defaultValue(false)
            .build()
    );

    private Setting<Boolean> players = addSetting(new BoolSettingBuilder()
            .name("players")
            .description("See players.")
            .defaultValue(true)
            .build()
    );

    private Setting<Color> playersColor = addSetting(new ColorSettingBuilder()
            .name("players-color")
            .description("Players color.")
            .defaultValue(new Color(255, 255, 255, 255))
            .build()
    );

    private Setting<Boolean> animals = addSetting(new BoolSettingBuilder()
            .name("animals")
            .description("See animals.")
            .defaultValue(true)
            .build()
    );

    private Setting<Color> animalsColor = addSetting(new ColorSettingBuilder()
            .name("animals-color")
            .description("Animals color.")
            .defaultValue(new Color(145, 255, 145, 255))
            .build()
    );

    private Setting<Boolean> mobs = addSetting(new BoolSettingBuilder()
            .name("mobs")
            .description("See mobs.")
            .defaultValue(true)
            .build()
    );

    private Setting<Color> mobsColor = addSetting(new ColorSettingBuilder()
            .name("mobs-color")
            .description("Mobs color.")
            .defaultValue(new Color(255, 145, 145, 255))
            .build()
    );

    private Setting<Boolean> storage = addSetting(new BoolSettingBuilder()
            .name("storage")
            .description("See chests, barrels and shulkers.")
            .defaultValue(false)
            .build()
    );

    private Setting<Color> storageColor = addSetting(new ColorSettingBuilder()
            .name("storage-color")
            .description("Storage color.")
            .defaultValue(new Color(255, 160, 0, 255))
            .build()
    );

    private Vec3d vec1;

    public Tracers() {
        super(Category.Render, "tracers", "Displays lines to entities.");
    }

    private void render(Entity entity, Color color) {
        Vec3d vec2 = entity.getPos().add(0, entity.getEyeHeight(entity.getPose()), 0);
        RenderUtils.line(vec1.x, vec1.y, vec1.z, vec2.x, vec2.y, vec2.z, color);
    }

    private void render(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getPos();
        RenderUtils.line(vec1.x, vec1.y, vec1.z, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5f, storageColor.value());
    }

    @EventHandler
    private Listener<RenderEvent> onRender = new Listener<>(event -> {
        vec1 = new Vec3d(0, 0, 1).rotateX(-(float) Math.toRadians(mc.cameraEntity.pitch)).rotateY(-(float) Math.toRadians(mc.cameraEntity.yaw));
        vec1 = vec1.add(mc.cameraEntity.getPos());

        if (!center.value()) vec1 = vec1.add(0, mc.cameraEntity.getEyeHeight(mc.cameraEntity.getPose()), 0);

        for (Entity entity : mc.world.getEntities()) {
            if (players.value() && EntityUtils.isPlayer(entity) && entity != mc.player) render(entity, playersColor.value());
            else if (animals.value() && EntityUtils.isAnimal(entity)) render(entity, animalsColor.value());
            else if (mobs.value() && EntityUtils.isMob(entity)) render(entity, mobsColor.value());
        }

        if (storage.value()) {
            for (BlockEntity blockEntity : mc.world.blockEntities) {
                if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity) {
                    render(blockEntity);
                }
            }
        }
    });
}
