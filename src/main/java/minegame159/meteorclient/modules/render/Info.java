package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.EntityAddedEvent;
import minegame159.meteorclient.events.EntityRemovedEvent;
import minegame159.meteorclient.events.Render2DEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.BoolSettingBuilder;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

public class Info extends Module {
    private Setting<Boolean> fps = addSetting(new BoolSettingBuilder()
            .name("fps")
            .description("Display fps.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> entities = addSetting(new BoolSettingBuilder()
            .name("entities")
            .description("Display number of entities.")
            .defaultValue(true)
            .consumer((aBoolean, aBoolean2) -> recalculateEntityCounts())
            .build()
    );

    private Setting<Boolean> entityCustomNames = addSetting(new BoolSettingBuilder()
            .name("entity-custom-names")
            .description("Use custom names.")
            .defaultValue(true)
            .consumer((aBoolean, aBoolean2) -> recalculateEntityCounts())
            .build()
    );

    private Setting<Boolean> separateSheepsByColor = addSetting(new BoolSettingBuilder()
            .name("separate-sheeps-by-color")
            .description("Separates sheeps by color in entity list.")
            .defaultValue(false)
            .consumer((aBoolean, aBoolean2) -> recalculateEntityCounts())
            .build()
    );

    private HashMap<String, EntityInfo> entityCounts = new HashMap<>();
    private int maxLetterCount = 0;

    public Info() {
        super(Category.Render, "info", "Displays various info.");
    }

    private String getEntityName(Entity entity) {
        String name = entityCustomNames.value() ? entity.getDisplayName().asString() : entity.getType().getName().asString();
        if (separateSheepsByColor.value() && entity instanceof SheepEntity) return StringUtils.capitalize(((SheepEntity) entity).getColor().getName()) + " - " + name;
        return name;
    }

    private EntityInfo getEntityInfo(Entity entity) {
        return entityCounts.computeIfAbsent(getEntityName(entity), EntityInfo::new);
    }

    private void recalculateEntityCounts() {
        entityCounts.clear();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity || entity instanceof ItemEntity) continue;

            getEntityInfo(entity).increment();
        }

        calculateMaxLetterCount();
    }

    private void calculateMaxLetterCount() {
        maxLetterCount = 0;

        for (EntityInfo a : entityCounts.values()) {
            maxLetterCount = Math.max(maxLetterCount, a.countStr.length());
        }
    }

    @Override
    public void onActivate() {
        recalculateEntityCounts();
    }

    @EventHandler
    private Listener<EntityAddedEvent> onEntityAdded = new Listener<>(event -> {
        if (event.entity instanceof PlayerEntity || event.entity instanceof ItemEntity) return;

        getEntityInfo(event.entity).increment();

        calculateMaxLetterCount();
    });

    @EventHandler
    private Listener<EntityRemovedEvent> onEntityRemoved = new Listener<>(event -> {
        if (event.entity instanceof PlayerEntity || event.entity instanceof ItemEntity) return;

        EntityInfo entityInfo = getEntityInfo(event.entity);
        entityInfo.decrement();
        if (entityInfo.count <= 0) entityCounts.remove(getEntityName(event.entity));

        calculateMaxLetterCount();
    });

    private void drawInfo(String text1, String text2, int y) {
        Utils.drawTextWithShadow(text1, 2, y, Color.fromRGBA(255, 255, 255, 255));
        Utils.drawTextWithShadow(text2, 2 + Utils.getTextWidth(text1), y, Color.fromRGBA(185, 185, 185, 255));
    }

    private void drawEntityCount(EntityInfo entityInfo, int y) {
        Utils.drawTextWithShadow(entityInfo.countStr, 2, y, Color.fromRGBA(185, 185, 185, 255));
        Utils.drawTextWithShadow(entityInfo.name, 2 + (maxLetterCount - entityInfo.countStr.length()) * 4 + 4 + Utils.getTextWidth(entityInfo.countStr), y, Color.fromRGBA(255, 255, 255, 255));
    }

    @EventHandler
    private Listener<Render2DEvent> onRender2D = new Listener<>(event -> {
        if (mc.options.debugEnabled) return;
        int y = 2;

        if (fps.value()) {
            drawInfo("FPS: ", MinecraftClient.getCurrentFps() + "", y);
            y += Utils.getTextHeight() + 2;
        }

        if (entities.value()) {
            for (EntityInfo a : entityCounts.values()) {
                drawEntityCount(a, y);
                y += Utils.getTextHeight() + 2;
            }
        }
    });

    private static class EntityInfo {
        public String name;
        public int count;
        public String countStr;

        public EntityInfo(String name) {
            this.name = name;
        }

        public void increment() {
            count++;
            countStr = Integer.toString(count);
        }

        public void decrement() {
            count--;
            countStr = Integer.toString(count);
        }
    }
}
