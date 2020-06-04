package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.*;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.TickRate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class HUD extends ToggleModule {
    private static final Color white = new Color(255, 255, 255);
    private static final Color gray = new Color(185, 185, 185);
    private static final Color red = new Color(225, 45, 45);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTopLeft = settings.createGroup("Top Left");
    private final SettingGroup sgTopRight = settings.createGroup("Top Right");
    private final SettingGroup sgBottomRight = settings.createGroup("Bottom Right");

    private final Setting<Boolean> armor = sgGeneral.add(new BoolSetting.Builder()
            .name("armor")
            .description("Diplays your armor above hotbar.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> waterMark = sgTopLeft.add(new BoolSetting.Builder()
            .name("water-mark")
            .description("Water mark.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> fps = sgTopLeft.add(new BoolSetting.Builder()
            .name("fps")
            .description("Display fps.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ping = sgTopLeft.add(new BoolSetting.Builder()
            .name("ping")
            .description("Display ping.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> tps = sgTopLeft.add(new BoolSetting.Builder()
            .name("tps")
            .description("Display tps.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> speed = sgTopLeft.add(new BoolSetting.Builder()
            .name("speed")
            .description("Display speed in blocks per second.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> biome = sgTopLeft.add(new BoolSetting.Builder()
            .name("biome")
            .description("Displays biome you are in.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> time = sgTopLeft.add(new BoolSetting.Builder()
            .name("time")
            .description("Displays ingame time in ticks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> entities = sgTopLeft.add(new BoolSetting.Builder()
            .name("entities")
            .description("Display number of entities.")
            .defaultValue(true)
            .onChanged(aBoolean -> updateEntities = true)
            .build()
    );

    private final Setting<Boolean> entityCustomNames = sgTopLeft.add(new BoolSetting.Builder()
            .name("entity-custom-names")
            .description("Use custom names.")
            .defaultValue(true)
            .onChanged(aBoolean -> updateEntities = true)
            .build()
    );

    private final Setting<Boolean> separateSheepsByColor = sgTopLeft.add(new BoolSetting.Builder()
            .name("separate-sheeps-by-color")
            .description("Separates sheeps by color in entity list.")
            .defaultValue(false)
            .onChanged(aBoolean -> updateEntities = true)
            .build()
    );

    private final Setting<Boolean> activeModules = sgTopRight.add(new BoolSetting.Builder()
            .name("active-modules")
            .description("Display active modules.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> potionTimers = sgBottomRight.add(new BoolSetting.Builder()
            .name("potion-timers")
            .description("Display potion timers and hide minecraft default potion icons.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> position = sgBottomRight.add(new BoolSetting.Builder()
            .name("position")
            .description("Display your position.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotation = sgBottomRight.add(new BoolSetting.Builder()
            .name("rotation")
            .description("Display your rotation.")
            .defaultValue(true)
            .build()
    );

    private final HashMap<String, EntityInfo> entityCounts = new HashMap<>();
    private int maxLetterCount = 0;
    private boolean updateEntities;
    private int updateEntitiesTimer = 2;

    private final List<ToggleModule> modules = new ArrayList<>();

    private final BlockPos.Mutable playerBlockPos = new BlockPos.Mutable();

    public HUD() {
        super(Category.Render, "HUD", "Displays various info to the screen.");
    }

    @Override
    public void onActivate() {
        updateEntities = true;

        recalculateActiveModules();
    }

    @EventHandler
    private final Listener<ActiveModulesChangedEvent> activeModulesChangedEventListener = new Listener<>(event -> recalculateActiveModules());

    @EventHandler
    private final Listener<ModuleVisibilityChangedEvent> onModuleVisibilityChanged = new Listener<>(event -> {
        if (event.module.isActive()) recalculateActiveModules();
    });

    @EventHandler
    private final Listener<EntityAddedEvent> onEntityAdded = new Listener<>(event -> {
        if (entities.get()) {
            if (!isValidEntity(event.entity)) return;
            updateEntities = true;
        }
    });

    @EventHandler
    private final Listener<EntityRemovedEvent> onEntityRemoved = new Listener<>(event -> {
        if (entities.get()) {
            if (!isValidEntity(event.entity)) return;
            updateEntities = true;
        }
    });

    private String getEntityName(Entity entity) {
        if (entity instanceof PlayerEntity) return "Player";
        if (entity instanceof ItemEntity) return "Item";
        String name = entityCustomNames.get() ? entity.getDisplayName().asString() : entity.getType().getName().asString();
        if (separateSheepsByColor.get() && entity instanceof SheepEntity) return StringUtils.capitalize(((SheepEntity) entity).getColor().getName().replace('_', ' ')) + " - " + name;
        return name;
    }

    private EntityInfo getEntityInfo(Entity entity) {
        return entityCounts.computeIfAbsent(getEntityName(entity), EntityInfo::new);
    }

    private boolean isValidEntity(Entity entity) {
        return entity != mc.player;
    }

    private void calculateMaxLetterCount() {
        maxLetterCount = 0;

        for (EntityInfo a : entityCounts.values()) {
            maxLetterCount = Math.max(maxLetterCount, a.countStr.length());
        }
    }

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        updateEntitiesTimer--;

        if (entities.get()) {
            if (updateEntities && updateEntitiesTimer <= 0 && entities.get()) {
                for (EntityInfo entityInfo : entityCounts.values()) {
                    entityInfo.reset();
                }

                for (Entity entity : mc.world.getEntities()) {
                    if (!isValidEntity(entity)) continue;

                    getEntityInfo(entity).increment(entity);
                }

                updateEntities = false;
                updateEntitiesTimer = 2;
                calculateMaxLetterCount();
            }
        }
    });

    @EventHandler
    private final Listener<Render2DEvent> onRender2D = new Listener<>(event -> {
        MeteorClient.FONT.begin();
        renderTopLeft(event);
        renderTopRight(event);
        renderBottomRight(event);
        MeteorClient.FONT.end();

        if (armor.get()) {
            int x = event.screenWidth / 2 + 12;
            int y = event.screenHeight - 38;

            if (!mc.player.abilities.creativeMode) y -= 18;

            for (int i = mc.player.inventory.armor.size() - 1; i >= 0; i--) {
                ItemStack itemStack = mc.player.inventory.armor.get(i);

                mc.getItemRenderer().renderGuiItem(itemStack, x, y);
                mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y);

                x += 20;
            }

            DiffuseLighting.disable();
        }

    });

    private void renderTopLeft(Render2DEvent event) {
        if (mc.options.debugEnabled) return;
        int y = 2;

        if (waterMark.get()) {
            drawInfo("Meteor Client ", Config.INSTANCE.getVersion(), y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (fps.get()) {
            drawInfo("FPS: ", ((IMinecraftClient) MinecraftClient.getInstance()).getFps() + "", y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (ping.get() && playerListEntry != null) {
            drawInfo("Ping: ", playerListEntry.getLatency() + "", y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (tps.get()) {
            drawInfo("TPS: ", String.format("%.1f", TickRate.INSTANCE.getTickRate()), y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        float timeSinceLastTick = TickRate.INSTANCE.getTimeSinceLastTick();
        if (timeSinceLastTick >= 1f) {
            drawInfo("Since last tick: ", String.format("%.1f", timeSinceLastTick), y, red);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (speed.get()) {
            double tX = Math.abs(mc.player.getX() - mc.player.prevX);
            double tZ = Math.abs(mc.player.getZ() - mc.player.prevZ);
            double length = Math.sqrt(tX * tX + tZ * tZ);

            drawInfo("Speed: ", String.format("%.1f", length * 20), y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (biome.get()) {
            playerBlockPos.set(mc.player);
            drawInfo("Biome: ", mc.world.getBiome(playerBlockPos).getName().asString(), y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (time.get()) {
            drawInfo("Time: ", mc.world.getTimeOfDay() % 24000 + "", y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (entities.get()) {
            for (EntityInfo renderInfo : entityCounts.values()) {
                if (!renderInfo.render) continue;

                drawEntityCount(renderInfo, y);
                y += MeteorClient.FONT.getHeight() + 2;
            }
        }
    }

    private void drawInfo(String text1, String text2, int x, int y, Color text1Color) {
        MeteorClient.FONT.renderStringWithShadow(text1, x, y, text1Color);
        MeteorClient.FONT.renderStringWithShadow(text2, x + MeteorClient.FONT.getStringWidth(text1), y, gray);
    }
    private void drawInfo(String text1, String text2, int y, Color text1Color) {
        drawInfo(text1, text2, 2, y, text1Color);
    }
    private void drawInfo(String text1, String text2, int y) {
        drawInfo(text1, text2, y, white);
    }
    private void drawInfoRight(String text1, String text2, int y, Color text1Color) {
        drawInfo(text1, text2, mc.getWindow().getScaledWidth() - MeteorClient.FONT.getStringWidth(text1) - MeteorClient.FONT.getStringWidth(text2) - 2, y, text1Color);
    }
    private void drawInfoRight(String text1, String text2, int y) {
        drawInfoRight(text1, text2, y, white);
    }

    private void drawEntityCount(EntityInfo entityInfo, int y) {
        MeteorClient.FONT.renderStringWithShadow(entityInfo.countStr, 2, y, gray);
        MeteorClient.FONT.renderStringWithShadow(entityInfo.name, 2 + (maxLetterCount - entityInfo.countStr.length()) * 4 + 4 + MeteorClient.FONT.getStringWidth(entityInfo.countStr), y, white);
    }

    private void renderTopRight(Render2DEvent event) {
        if (mc.options.debugEnabled) return;
        int y = 2;

        if (activeModules.get()) {
            for (ToggleModule module : modules) {
                String infoString = module.getInfoString();
                int x;
                if (infoString == null) {
                    x = event.screenWidth - MeteorClient.FONT.getStringWidth(module.title) - 2;
                    MeteorClient.FONT.renderStringWithShadow(module.title, x, y, module.color);
                } else {
                    x = event.screenWidth - MeteorClient.FONT.getStringWidth(module.title + " " + infoString) - 2;
                    MeteorClient.FONT.renderStringWithShadow(module.title, x, y, module.color);
                    MeteorClient.FONT.renderStringWithShadow(module.getInfoString(), x + MeteorClient.FONT.getStringWidth(module.title + " "), y, gray);
                }
                y += MeteorClient.FONT.getHeight() + 1;
            }
        }
    }

    private void recalculateActiveModules() {
        modules.clear();

        for (ToggleModule oldModule : ModuleManager.INSTANCE.getActive()) {
            if (oldModule.isVisible()) modules.add(oldModule);
        }

        modules.sort((o1, o2) -> {
            int a = Integer.compare(o1.getInfoString() == null ? MeteorClient.FONT.getStringWidth(o1.title) : MeteorClient.FONT.getStringWidth(o1.title + " " + o1.getInfoString()), o2.getInfoString() == null ? MeteorClient.FONT.getStringWidth(o2.title) : MeteorClient.FONT.getStringWidth(o2.title + " " + o2.getInfoString()));
            if (a == 0) return 0;
            return a < 0 ? 1 : -1;
        });
    }

    private void renderBottomRight(Render2DEvent event) {
        int y = event.screenHeight - MeteorClient.FONT.getHeight() - 2;

        if (rotation.get()) {
            Direction direction = mc.player.getHorizontalFacing();
            String axis = "invalid";
            switch (direction) {
                case NORTH: axis = "-Z"; break;
                case SOUTH: axis = "+Z"; break;
                case WEST:  axis = "-X"; break;
                case EAST:  axis = "+X"; break;
            }

            float yaw = mc.player.yaw % 360;
            if (yaw < 0) yaw += 360;
            if (yaw > 180) yaw -= 360;

            float pitch = mc.player.pitch % 360;
            if (pitch < 0) pitch += 360;
            if (pitch > 180) pitch -= 360;

            drawInfoRight(String.format("%s %s ", StringUtils.capitalize(direction.getName()), axis), String.format("(%.1f, %.1f)", yaw, pitch), y);
            y -= MeteorClient.FONT.getHeight() + 2;
        }

        if (position.get()) {
            if (mc.player.dimension == DimensionType.OVERWORLD) {
                drawPosition(event.screenWidth, "Nether Pos: ", y, mc.player.getX() / 8.0, mc.player.getY() / 8.0, mc.player.getZ() / 8.0);
                y -= MeteorClient.FONT.getHeight() + 2;
                drawPosition(event.screenWidth, "Pos: ", y, mc.player.getX(), mc.player.getY(), mc.player.getZ());
                y -= MeteorClient.FONT.getHeight() + 2;
            } else if (mc.player.dimension == DimensionType.THE_NETHER) {
                drawPosition(event.screenWidth, "Overworld Pos: ", y, mc.player.getX() * 8.0, mc.player.getY() * 8.0, mc.player.getZ() * 8.0);
                y -= MeteorClient.FONT.getHeight() + 2;
                drawPosition(event.screenWidth, "Pos: ", y, mc.player.getX(), mc.player.getY(), mc.player.getZ());
                y -= MeteorClient.FONT.getHeight() + 2;
            } else if (mc.player.dimension == DimensionType.THE_END) {
                drawPosition(event.screenWidth, "Pos: ", y, mc.player.getX(), mc.player.getY(), mc.player.getZ());
                y -= MeteorClient.FONT.getHeight() + 2;
            }
        }

        if (potionTimers.get()) {
            for (StatusEffectInstance statusEffectInstance : mc.player.getStatusEffects()) {
                StatusEffect statusEffect = statusEffectInstance.getEffectType();

                drawInfoRight(statusEffect.getName().asString(), " " + (statusEffectInstance.getAmplifier() + 1) + " (" + StatusEffectUtil.durationToString(statusEffectInstance, 1) + ")", y, new Color(statusEffect.getColor()));
                y -= MeteorClient.FONT.getHeight() + 2;
            }
        }
    }

    private void drawPosition(int screenWidth, String text, int yy, double x, double y, double z) {
        String msg1 = String.format("%.1f %.1f %.1f", x, y, z);
        int x1 = screenWidth - MeteorClient.FONT.getStringWidth(msg1) - 2;
        int x2 = screenWidth - MeteorClient.FONT.getStringWidth(msg1) - MeteorClient.FONT.getStringWidth(text) - 2;
        MeteorClient.FONT.renderStringWithShadow(msg1, x1, yy, gray);
        MeteorClient.FONT.renderStringWithShadow(text, x2, yy, white);
    }

    private static class EntityInfo {
        public String name;
        public int count;
        public String countStr;
        public boolean render;

        public EntityInfo(String name) {
            this.name = name;
        }

        public void increment(Entity entity) {
            if (entity instanceof ItemEntity) count += ((ItemEntity) entity).getStack().getCount();
            else count++;
            countStr = Integer.toString(count);
            render = true;
        }

        public void reset() {
            count = 0;
            render = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntityInfo that = (EntityInfo) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
