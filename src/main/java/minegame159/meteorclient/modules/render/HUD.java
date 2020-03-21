package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.*;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.TickRate;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class HUD extends Module {
    private static int white = Color.fromRGBA(255, 255, 255, 255);
    private static int gray = Color.fromRGBA(185, 185, 185, 255);
    private static int red = Color.fromRGBA(225, 45, 45, 255);

    private Setting<Boolean> fps = addSetting(new BoolSetting.Builder()
            .name("fps")
            .description("Display fps.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> tps = addSetting(new BoolSetting.Builder()
            .name("tps")
            .description("Display tps.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> speed = addSetting(new BoolSetting.Builder()
            .name("speed")
            .description("Display speed in blocks per second.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> entities = addSetting(new BoolSetting.Builder()
            .name("entities")
            .description("Display number of entities.")
            .defaultValue(true)
            .onChanged(aBoolean -> updateEntities = true)
            .build()
    );

    private Setting<Boolean> entityCustomNames = addSetting(new BoolSetting.Builder()
            .name("entity-custom-names")
            .description("Use custom names.")
            .defaultValue(true)
            .onChanged(aBoolean -> updateEntities = true)
            .build()
    );

    private Setting<Boolean> separateSheepsByColor = addSetting(new BoolSetting.Builder()
            .name("separate-sheeps-by-color")
            .description("Separates sheeps by color in entity list.")
            .defaultValue(false)
            .onChanged(aBoolean -> updateEntities = true)
            .build()
    );

    private Setting<Boolean> activeModules = addSetting(new BoolSetting.Builder()
            .name("active-modules")
            .description("Display active modules.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> position = addSetting(new BoolSetting.Builder()
            .name("position")
            .description("Display your position.")
            .defaultValue(true)
            .build()
    );

    private HashMap<String, EntityInfo> entityCounts = new HashMap<>();
    private int maxLetterCount = 0;
    private boolean updateEntities;
    private int updateEntitiesTimer = 2;

    private List<Module> modules = new ArrayList<>();

    public HUD() {
        super(Category.Render, "HUD", "Displays various info to the screen.");
    }

    @Override
    public void onActivate() {
        updateEntities = true;

        recalculateActiveModules();
    }

    @EventHandler
    private Listener<ActiveModulesChangedEvent> activeModulesChangedEventListener = new Listener<>(event -> recalculateActiveModules());

    @EventHandler
    private Listener<ModuleVisibilityChangedEvent> onModuleVisibilityChanged = new Listener<>(event -> {
        if (event.module.isActive()) recalculateActiveModules();
    });

    @EventHandler
    private Listener<EntityAddedEvent> onEntityAdded = new Listener<>(event -> {
        if (entities.get()) {
            if (!isValidEntity(event.entity)) return;
            updateEntities = true;
        }
    });

    @EventHandler
    private Listener<EntityRemovedEvent> onEntityRemoved = new Listener<>(event -> {
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
    private Listener<TickEvent> onTick = new Listener<>(event -> {
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
    private Listener<Render2DEvent> onRender2D = new Listener<>(event -> {
        renderTopLeft(event);
        renderTopRight(event);
        renderBottomRight(event);
    });

    private void renderTopLeft(Render2DEvent event) {
        if (mc.options.debugEnabled) return;
        int y = 2;

        if (fps.get()) {
            drawInfo("FPS: ", MinecraftClient.getCurrentFps() + "", y);
            y += Utils.getTextHeight() + 2;
        }

        if (tps.get()) {
            drawInfo("TPS: ", String.format("%.1f", TickRate.INSTANCE.getTickRate()), y);
            y += Utils.getTextHeight() + 2;
        }

        float timeSinceLastTick = TickRate.INSTANCE.getTimeSinceLastTick();
        if (timeSinceLastTick >= 1f) {
            drawInfo("Since last tick: ", String.format("%.1f", timeSinceLastTick), y, red);
            y += Utils.getTextHeight() + 2;
        }

        if (speed.get()) {
            double tX = Math.abs(mc.player.x - mc.player.prevX);
            double tZ = Math.abs(mc.player.z - mc.player.prevZ);
            double length = Math.sqrt(tX * tX + tZ * tZ);

            drawInfo("Speed: ", String.format("%.1f", length * 20), y);
            y += Utils.getTextHeight() + 2;
        }

        if (entities.get()) {
            for (EntityInfo renderInfo : entityCounts.values()) {
                if (!renderInfo.render) continue;

                drawEntityCount(renderInfo, y);
                y += Utils.getTextHeight() + 2;
            }
        }
    }

    private void drawInfo(String text1, String text2, int y, int text1Color) {
        Utils.drawTextWithShadow(text1, 2, y, text1Color);
        Utils.drawTextWithShadow(text2, 2 + Utils.getTextWidth(text1), y, gray);
    }
    private void drawInfo(String text1, String text2, int y) {
        drawInfo(text1, text2, y, white);
    }

    private void drawEntityCount(EntityInfo entityInfo, int y) {
        Utils.drawTextWithShadow(entityInfo.countStr, 2, y, gray);
        Utils.drawTextWithShadow(entityInfo.name, 2 + (maxLetterCount - entityInfo.countStr.length()) * 4 + 4 + Utils.getTextWidth(entityInfo.countStr), y, white);
    }

    private void renderTopRight(Render2DEvent event) {
        if (mc.options.debugEnabled) return;
        int y = 2;

        if (activeModules.get()) {
            for (Module module : modules) {
                String infoString = module.getInfoString();
                if (infoString == null) {
                    int x = event.screenWidth - Utils.getTextWidth(module.title) - 2;
                    Utils.drawTextWithShadow(module.title, x, y, module.getColor());
                    y += Utils.getTextHeight() + 1;
                } else {
                    int x = event.screenWidth - Utils.getTextWidth(module.title + " " + infoString) - 2;
                    Utils.drawTextWithShadow(module.title, x, y, module.getColor());
                    Utils.drawTextWithShadow(module.getInfoString(), x + Utils.getTextWidth(module.title + " "), y, gray);
                    y += Utils.getTextHeight() + 1;
                }
            }
        }
    }

    private void recalculateActiveModules() {
        modules.clear();

        for (Module module : ModuleManager.INSTANCE.getActive()) {
            if (module.isVisible()) modules.add(module);
        }

        modules.sort((o1, o2) -> {
            int a = Integer.compare(o1.getInfoString() == null ? Utils.getTextWidth(o1.title) : Utils.getTextWidth(o1.title + " " + o1.getInfoString()), o2.getInfoString() == null ? Utils.getTextWidth(o2.title) : Utils.getTextWidth(o2.title + " " + o2.getInfoString()));
            if (a == 0) return 0;
            return a < 0 ? 1 : -1;
        });
    }

    private void renderBottomRight(Render2DEvent event) {
        int y = event.screenHeight - Utils.getTextHeight() - 2;

        if (position.get()) {
            if (mc.player.dimension == DimensionType.OVERWORLD) {
                drawPosition(event.screenWidth, "Nether Pos: ", y, mc.player.x / 8.0, mc.player.y / 8.0, mc.player.z / 8.0);
                y -= Utils.getTextHeight() + 2;
                drawPosition(event.screenWidth, "Pos: ", y, mc.player.x, mc.player.y, mc.player.z);
            } else if (mc.player.dimension == DimensionType.THE_NETHER) {
                drawPosition(event.screenWidth, "Overworld Pos: ", y, mc.player.x * 8.0, mc.player.y * 8.0, mc.player.z * 8.0);
                y -= Utils.getTextHeight() + 2;
                drawPosition(event.screenWidth, "Pos: ", y, mc.player.x, mc.player.y, mc.player.z);
            } else if (mc.player.dimension == DimensionType.THE_END) {
                drawPosition(event.screenWidth, "Pos: ", y, mc.player.x, mc.player.y, mc.player.z);
            }
        }
    }

    private void drawPosition(int screenWidth, String text, int yy, double x, double y, double z) {
        String msg1 = String.format("%.1f %.1f %.1f", x, y, z);
        int x1 = screenWidth - Utils.getTextWidth(msg1) - 2;
        int x2 = screenWidth - Utils.getTextWidth(msg1) - Utils.getTextWidth(text) - 2;
        Utils.drawTextWithShadow(msg1, x1, yy, Color.fromRGBA(185, 185, 185, 255));
        Utils.drawTextWithShadow(text, x2, yy, Color.fromRGBA(255, 255, 255, 255));
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
            int preCount = count;
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
