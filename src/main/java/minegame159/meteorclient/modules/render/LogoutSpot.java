package minegame159.meteorclient.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.EntityAddedEvent;
import minegame159.meteorclient.events.EntityDestroyEvent;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.RenderUtils;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class LogoutSpot extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private Color sideColor = new Color();

    private Setting<Color> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("Color.")
            .defaultValue(new Color(255, 0, 255))
            .onChanged(color1 -> {
                sideColor.set(color1);
                sideColor.a -= 200;
                sideColor.validate();
            })
            .build()
    );

    private List<Entry> players = new ArrayList<>();

    public LogoutSpot() {
        super(Category.Render, "logout-spot", "Displays players logout position.");
        lineColor.changed();
    }

    @Override
    public void onDeactivate() {
        players.clear();
    }

    @EventHandler
    private Listener<EntityDestroyEvent> onEntityDestroy = new Listener<>(event -> {
        if (event.entity instanceof PlayerEntity) players.add(new Entry((LivingEntity) event.entity));
    });

    @EventHandler
    private Listener<EntityAddedEvent> onEntityAdded = new Listener<>(event -> {
        if (event.entity instanceof PlayerEntity) {
            int toRemove = -1;

            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).uuid.equals(event.entity.getUuidAsString())) {
                    toRemove = i;
                    break;
                }
            }

            if (toRemove != -1) {
                players.remove(toRemove);
            }
        }
    });

    @EventHandler
    private Listener<RenderEvent> onRender = new Listener<>(event -> {
        for (Entry player : players) player.render(event);

        GlStateManager.disableDepthTest();
        GlStateManager.disableTexture();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
    });

    @Override
    public String getInfoString() {
        return Integer.toString(players.size());
    }

    private class Entry {
        public final double x, y, z;
        public final double width, height;

        public final String uuid, name;
        public final int health, maxHealth;

        public Entry(LivingEntity entity) {
            x = entity.x;
            y = entity.y;
            z = entity.z;

            width = entity.getBoundingBox().getXLength();
            height = entity.getBoundingBox().getZLength();

            uuid = entity.getUuidAsString();
            name = entity.getDisplayName().asString();
            health = (int) (entity.getHealth() + entity.getAbsorptionAmount());
            maxHealth = (int) (entity.getMaximumHealth() + entity.getAbsorptionAmount());
        }

        public void render(RenderEvent event) {
            EntityRenderDispatcher a = mc.getEntityRenderManager();
            double dist = Math.sqrt(a.getSquaredDistanceToCamera(x, a.camera.getPos().y, z));

            if (dist > mc.options.viewDistance * 16) return;
            dist = Math.sqrt(a.getSquaredDistanceToCamera(x, y, z));

            RenderUtils.boxWithLines(x, y, z, width, height, sideColor, lineColor.get());
            ModuleManager.INSTANCE.get(Nametags.class).render(dist, 0.5f, x + 0.5 - event.offsetX, y - event.offsetY, z + 0.5 - event.offsetZ, a.cameraYaw, a.cameraPitch, name, health, maxHealth);
        }
    }
}
