package minegame159.meteorclient.modules.render;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.EntityAddedEvent;
import minegame159.meteorclient.events.EntityRemovedEvent;
import minegame159.meteorclient.events.Render2DEvent;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.BoolSettingBuilder;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;

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

    private HashMap<Class, EntityInfo> entityCounts = new HashMap<>();
    private int maxLetterCount = 0;

    public Info() {
        super(Category.Render, "info", "Displays various info.");
    }

    private void recalculateEntityCounts() {
        entityCounts.clear();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity || entity instanceof ItemEntity) continue;

            EntityInfo a = entityCounts.computeIfAbsent(entity.getClass(), aClass -> new EntityInfo(entity.getDisplayName().asString()));
            a.increment();
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

    @SubscribeEvent
    private void onEntityAdded(EntityAddedEvent e) {
        if (e.entity instanceof PlayerEntity || e.entity instanceof ItemEntity) return;

        EntityInfo a = entityCounts.computeIfAbsent(e.entity.getClass(), aClass -> new EntityInfo(e.entity.getDisplayName().asString()));
        a.increment();

        calculateMaxLetterCount();
    }

    @SubscribeEvent
    private void onEntityRemoved(EntityRemovedEvent e) {
        if (e.entity instanceof PlayerEntity || e.entity instanceof ItemEntity) return;

        EntityInfo a = entityCounts.get(e.entity.getClass());
        a.decrement();
        if (a.count <= 0) entityCounts.remove(e.entity.getClass());

        calculateMaxLetterCount();
    }

    private void drawInfo(String text1, String text2, int y) {
        Utils.drawText(text1, 2, y, Color.fromRGBA(255, 255, 255, 255));
        Utils.drawText(text2, 2 + Utils.getTextWidth(text1), y, Color.fromRGBA(185, 185, 185, 255));
    }

    private void drawEntityCount(EntityInfo entityInfo, int y) {
        Utils.drawText(entityInfo.countStr, 2, y, Color.fromRGBA(185, 185, 185, 255));
        Utils.drawText(entityInfo.name, 2 + (maxLetterCount - entityInfo.countStr.length()) * 4 + 4 + Utils.getTextWidth(entityInfo.countStr), y, Color.fromRGBA(255, 255, 255, 255));
    }

    @SubscribeEvent
    private void onRender2D(Render2DEvent e) {
        int y = 2;

        if (fps.value()) {
            drawInfo("FPS: ", ((IMinecraftClient) mc).getCurrentFps() + "", y);
            y += Utils.getTextHeight() + 2;
        }

        if (entities.value()) {
            for (EntityInfo a : entityCounts.values()) {
                drawEntityCount(a, y);
                y += Utils.getTextHeight() + 2;
            }
        }
    }

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
