package minegame159.meteorclient.modules.render;

import com.google.common.reflect.TypeToken;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.utils.HttpUtils;
import minegame159.meteorclient.utils.MeteorTaskExecutor;
import minegame159.meteorclient.utils.UuidNameHistoryResponseItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EntityOwner extends ToggleModule {
    private static final Type RESPONSE_TYPE = new TypeToken<List<UuidNameHistoryResponseItem>>() {}.getType();

    private final Map<UUID, String> uuidToName = new HashMap<>();

    public EntityOwner() {
        super(Category.Render, "entity-owner", "Displays name of the player that owns that entity.");
    }

    @Override
    public void onActivate() {
        MeteorTaskExecutor.start();
    }

    @Override
    public void onDeactivate() {
        MeteorTaskExecutor.stop();

        uuidToName.clear();
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        for (Entity entity : mc.world.getEntities()) {
            UUID ownerUuid = null;
            if (entity instanceof TameableEntity) ownerUuid = ((TameableEntity) entity).getOwnerUuid();
            else if (entity instanceof HorseBaseEntity) ownerUuid = ((HorseBaseEntity) entity).getOwnerUuid();
            if (ownerUuid == null) continue;

            String name = getOwnerName(ownerUuid);

            event.matrixStack.push();
            event.matrixStack.translate(entity.getX() - cameraPos.x, entity.getY() - cameraPos.y + entity.getHeight(), entity.getZ() - cameraPos.z);
            event.matrixStack.multiply(mc.getEntityRenderManager().getRotation());
            //event.matrixStack.translate(cameraPos.x, cameraPos.y, cameraPos.z);
            event.matrixStack.scale(-0.025F, -0.025F, 0.025F);
            //event.matrixStack.scale(-0.5f, -0.5f, -0.5f);
            float g = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
            int k = (int)(g * 255.0F) << 24;
            TextRenderer textRenderer = mc.textRenderer;
            float h = (float)(-textRenderer.getStringWidth(name) / 2);
            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
            textRenderer.draw(name, h, (float)-10, 553648127, false, event.matrixStack.peek().getModel(), immediate, false, k, 0);
            textRenderer.draw(name, h, (float)-10, -1, false, event.matrixStack.peek().getModel(), immediate, false, k, 0);
            immediate.draw();

            event.matrixStack.pop();
            //GameRenderer.renderFloatingText(mc.textRenderer, name, (float) (entity.x - cameraPos.x), (float) (entity.y - cameraPos.y + entity.getHeight()), (float) (entity.z - cameraPos.z), -10, camera.getYaw(), camera.getPitch(), false);
        }
    });

    private String getOwnerName(UUID uuid) {
        // Get name if owner is online
        PlayerEntity player = mc.world.getPlayerByUuid(uuid);
        if (player != null) return player.getGameProfile().getName();

        // Check cache
        String name = uuidToName.get(uuid);
        if (name != null) return name;

        // Make http request to mojang api
        MeteorTaskExecutor.execute(() -> {
            if (isActive()) {
                List<UuidNameHistoryResponseItem> response = HttpUtils.get("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names", RESPONSE_TYPE);

                if (isActive()) {
                    if (response == null || response.size() <= 0) uuidToName.put(uuid, "Failed to get name");
                    else uuidToName.put(uuid, response.get(response.size() - 1).name);
                }
            }
        });

        name = "Retrieving";
        uuidToName.put(uuid, name);
        return name;
    }
}
