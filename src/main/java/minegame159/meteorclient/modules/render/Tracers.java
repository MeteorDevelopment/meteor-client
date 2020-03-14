package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.Setting;
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
    private Setting<Boolean> players = addSetting(new BoolSetting.Builder()
            .name("players")
            .description("See players.")
            .defaultValue(true)
            .build()
    );

    private Setting<Color> playersColor = addSetting(new ColorSetting.Builder()
            .name("players-color")
            .description("Players color.")
            .defaultValue(new Color(255, 255, 255, 255))
            .build()
    );

    private Setting<Boolean> animals = addSetting(new BoolSetting.Builder()
            .name("animals")
            .description("See animals.")
            .defaultValue(true)
            .build()
    );

    private Setting<Color> animalsColor = addSetting(new ColorSetting.Builder()
            .name("animals-color")
            .description("Animals color.")
            .defaultValue(new Color(145, 255, 145, 255))
            .build()
    );

    private Setting<Boolean> mobs = addSetting(new BoolSetting.Builder()
            .name("mobs")
            .description("See mobs.")
            .defaultValue(true)
            .build()
    );

    private Setting<Color> mobsColor = addSetting(new ColorSetting.Builder()
            .name("mobs-color")
            .description("Mobs color.")
            .defaultValue(new Color(255, 145, 145, 255))
            .build()
    );

    private Setting<Boolean> storage = addSetting(new BoolSetting.Builder()
            .name("storage")
            .description("See chests, barrels and shulkers.")
            .defaultValue(false)
            .build()
    );

    private Setting<Color> storageColor = addSetting(new ColorSetting.Builder()
            .name("storage-color")
            .description("Storage color.")
            .defaultValue(new Color(255, 160, 0, 255))
            .build()
    );

    private Vec3d vec1;

    public Tracers() {
        super(Category.Render, "tracers", "Displays lines to entities.");
    }

    private void render(Entity entity, Color color, RenderEvent event) {
        Vec3d vec2 = entity.getPos().add(0, entity.getEyeHeight(entity.getPose()), 0);
        double y = (entity.getBoundingBox().y2 - entity.getBoundingBox().y1) / 2.0;
        RenderUtils.line(vec1.x - (mc.cameraEntity.x - event.offsetX), vec1.y - (mc.cameraEntity.y - event.offsetY), vec1.z - (mc.cameraEntity.z - event.offsetZ), vec2.x, vec2.y - y, vec2.z, color);
    }

    private void render(BlockEntity blockEntity, RenderEvent event) {
        BlockPos pos = blockEntity.getPos();
        RenderUtils.line(vec1.x - (mc.cameraEntity.x - event.offsetX), vec1.y - (mc.cameraEntity.y - event.offsetY), vec1.z - (mc.cameraEntity.z - event.offsetZ), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5f, storageColor.get());
    }

    @EventHandler
    private Listener<RenderEvent> onRender = new Listener<>(event -> {
        vec1 = new Vec3d(0, 0, 1)
                .rotateX(-(float) Math.toRadians(mc.cameraEntity.pitch))
                .rotateY(-(float) Math.toRadians(mc.cameraEntity.yaw))
                .add(mc.cameraEntity.getPos());

        for (Entity entity : mc.world.getEntities()) {
            if (players.get() && EntityUtils.isPlayer(entity) && entity != mc.player) render(entity, playersColor.get(), event);
            else if (animals.get() && EntityUtils.isAnimal(entity)) render(entity, animalsColor.get(), event);
            else if (mobs.get() && EntityUtils.isMob(entity)) render(entity, mobsColor.get(), event);
        }

        if (storage.get()) {
            for (BlockEntity blockEntity : mc.world.blockEntities) {
                if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity) {
                    render(blockEntity, event);
                }
            }
        }
    });
}
