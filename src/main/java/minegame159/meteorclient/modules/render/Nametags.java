package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;

public class Nametags extends ToggleModule {
    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);
    private static final Color GREEN = new Color(25, 225, 25);
    private static final Color ORANGE = new Color(225, 105, 25);
    private static final Color RED = new Color(225, 25, 25);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    public Nametags() {
        super(Category.Render, "nametags", "Displays nametags above players.");
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity) || entity == mc.player) continue;

            renderNametag(event, (PlayerEntity) entity);
        }
    });

    private void renderNametag(RenderEvent event, PlayerEntity entity) {
        Camera camera = mc.gameRenderer.getCamera();

        // Compute scale
        double scale = 0.025;
        double dist = Utils.distanceToCamera(entity);
        if (dist > 10) scale *= dist / 10 * this.scale.get();

        // Compute health things
        float absorption = entity.getAbsorptionAmount();
        int health = Math.round(entity.getHealth() + absorption);
        double healthPercentage = health / (entity.getMaximumHealth() + absorption);

        String name = entity.getGameProfile().getName();
        String healthText = " " + health;

        // Setup the rotation
        Matrices.push();
        Matrices.translate(entity.getX() - event.offsetX, entity.getY() + entity.getHeight() + 0.5 - event.offsetY, entity.getZ() - event.offsetZ);
        Matrices.rotate(-camera.getYaw(), 0, 1, 0);
        Matrices.rotate(camera.getPitch(), 1, 0, 0);
        Matrices.scale(-scale, -scale, scale);

        // Render background
        double i = MeteorClient.FONT.getStringWidth(name) / 2.0 + MeteorClient.FONT.getStringWidth(healthText) / 2.0;
        ShapeBuilder.begin(null, GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        ShapeBuilder.quad(-i - 1, -1, 0, -i - 1, 8, 0, i + 1, 8, 0, i + 1, -1, 0, BACKGROUND);
        ShapeBuilder.end();

        // Get health color
        Color healthColor;
        if (healthPercentage <= 0.333) healthColor = RED;
        else if (healthPercentage <= 0.666) healthColor = ORANGE;
        else healthColor = GREEN;

        // Render name and health texts
        MeteorClient.FONT.begin();
        double hX = MeteorClient.FONT.renderString(name, -i, 0, TEXT);
        MeteorClient.FONT.renderString(healthText, hX, 0, healthColor);
        MeteorClient.FONT.end();

        Matrices.pop();
    }
}
