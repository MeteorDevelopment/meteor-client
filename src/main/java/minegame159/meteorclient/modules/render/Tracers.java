package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accountsfriends.Friend;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.EntityUtils;
import minegame159.meteorclient.utils.RenderUtils;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Tracers extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgAnimals = settings.createGroup("Animals");
    private final SettingGroup sgMobs = settings.createGroup("Mobs");
    private final SettingGroup sgStorage = settings.createGroup("Storage");

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Other entities.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Color> othersColor = sgGeneral.add(new ColorSetting.Builder()
            .name("others-color")
            .description("Color of other entities.")
            .defaultValue(new Color(200, 200, 200))
            .build()
    );
    
    private final Setting<Boolean> players = sgPlayers.add(new BoolSetting.Builder()
            .name("players")
            .description("See players.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> playersColor = sgPlayers.add(new ColorSetting.Builder()
            .name("players-color")
            .description("Players color.")
            .defaultValue(new Color(255, 255, 255, 255))
            .build()
    );

    private final Setting<Boolean> animals = sgAnimals.add(new BoolSetting.Builder()
            .name("animals")
            .description("See animals.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> animalsColor = sgAnimals.add(new ColorSetting.Builder()
            .name("animals-color")
            .description("Animals color.")
            .defaultValue(new Color(145, 255, 145, 255))
            .build()
    );

    private final Setting<Boolean> mobs = sgMobs.add(new BoolSetting.Builder()
            .name("mobs")
            .description("See mobs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> mobsColor = sgMobs.add(new ColorSetting.Builder()
            .name("mobs-color")
            .description("Mobs color.")
            .defaultValue(new Color(255, 145, 145, 255))
            .build()
    );

    private final Setting<Boolean> storage = sgStorage.add(new BoolSetting.Builder()
            .name("storage")
            .description("See chests, barrels and shulkers.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Color> storageColor = sgStorage.add(new ColorSetting.Builder()
            .name("storage-color")
            .description("Storage color.")
            .defaultValue(new Color(255, 160, 0, 255))
            .build()
    );

    private Vec3d vec1;
    private int count;

    public Tracers() {
        super(Category.Render, "tracers", "Displays lines to entities.");
    }

    private void render(Entity entity, Color color, RenderEvent event) {
        Vec3d vec2 = entity.getPos().add(0, entity.getEyeHeight(entity.getPose()), 0);
        double y = (entity.getBoundingBox().y2 - entity.getBoundingBox().y1) / 2.0;
        RenderUtils.line(vec1.x - (mc.cameraEntity.x - event.offsetX), vec1.y - (mc.cameraEntity.y - event.offsetY), vec1.z - (mc.cameraEntity.z - event.offsetZ), vec2.x, vec2.y - y, vec2.z, color);

        count++;
    }

    private void render(BlockEntity blockEntity, RenderEvent event) {
        BlockPos pos = blockEntity.getPos();
        RenderUtils.line(vec1.x - (mc.cameraEntity.x - event.offsetX), vec1.y - (mc.cameraEntity.y - event.offsetY), vec1.z - (mc.cameraEntity.z - event.offsetZ), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5f, storageColor.get());

        count++;
    }

    @EventHandler
    private Listener<RenderEvent> onRender = new Listener<>(event -> {
        count = 0;

        vec1 = new Vec3d(0, 0, 1)
                .rotateX(-(float) Math.toRadians(mc.cameraEntity.pitch))
                .rotateY(-(float) Math.toRadians(mc.cameraEntity.yaw))
                .add(mc.cameraEntity.getPos());

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;

            if (players.get() && EntityUtils.isPlayer(entity)) {
                Color color = playersColor.get();
                Friend friend = FriendManager.INSTANCE.get(((PlayerEntity) entity).getGameProfile().getName());
                if (friend != null) color = friend.color;

                if (friend == null || friend.showInTracers) render(entity, color, event);
            }
            else if (animals.get() && EntityUtils.isAnimal(entity)) render(entity, animalsColor.get(), event);
            else if (mobs.get() && EntityUtils.isMob(entity)) render(entity, mobsColor.get(), event);
            else if (entities.get().contains(entity.getType())) render(entity, othersColor.get(), event);
        }

        if (storage.get()) {
            for (BlockEntity blockEntity : mc.world.blockEntities) {
                if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity) {
                    render(blockEntity, event);
                }
            }
        }
    });

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}
