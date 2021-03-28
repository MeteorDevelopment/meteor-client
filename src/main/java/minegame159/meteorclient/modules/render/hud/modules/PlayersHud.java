package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

public class PlayersHud extends HudElement {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("display-friends")
            .description("Whether to show friends or not.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> coords = sgGeneral.add(new BoolSetting.Builder()
            .name("show-coords")
            .description("Whether to show coordinates of players or not.")
            .defaultValue(true)
            .build()
    );

    public PlayersHud(HUD hud) {
        super(hud, "player-info", "Displays players in your visual range.");
    }

    @Override
    public void update(HudRenderer renderer) {
        double width = renderer.textWidth("Players:");
        double height = renderer.textHeight();
        box.setSize(renderer.textWidth("Players:"), renderer.textHeight());

        int i = 0;
        if(mc.world == null) return;
        for (AbstractClientPlayerEntity e : mc.world.getPlayers()) {
            PlayerEntity entity = e;
            if (entity.equals(mc.player)) continue;
            if (!friends.get() && Friends.get().contains(Friends.get().get(entity))) continue;
            width = Math.max(width, getModuleWidth(renderer, entity));
            height += renderer.textHeight();
            if (i > 0) height += 2;
            i++;
        }

        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        renderer.text("Players:", x, y, hud.primaryColor.get());

        if(mc.world == null) return;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            PlayerEntity entity = p;
            if (entity.equals(mc.player)) continue;
            if (!friends.get() && Friends.get().contains(Friends.get().get(entity))) continue;

            y += 2 + renderer.textHeight();
            Color color = Friends.get().contains(Friends.get().get(entity)) ? Friends.get().getFriendColor(entity) : hud.secondaryColor.get();
            renderer.text(entity.getEntityName(), x, y, color);

            if (coords.get()) renderer.text(String.format("[%1$s, %2$s, %3$s]", Long.toString((long) Math.floor(entity.getX())), Long.toString((long) Math.floor(entity.getY())), Long.toString((long) Math.floor(entity.getZ()))), x + 5 + renderer.textWidth(entity.getEntityName()), y, hud.secondaryColor.get());
        }

    }

    private double getModuleWidth(HudRenderer renderer, PlayerEntity player) {
        return renderer.textWidth(player.getEntityName() + ((coords.get()) ? 5 + renderer.textWidth(String.format("[%1$s, %2$s, %3$s]", Double.toString(Math.floor(player.getX())), Double.toString(Math.floor(player.getY())), Double.toString(Math.floor(player.getZ())))): ""));
    }
}