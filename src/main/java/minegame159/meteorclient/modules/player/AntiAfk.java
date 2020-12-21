package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class AntiAfk extends ToggleModule {

    public AntiAfk() {
        super(Category.Player, "Anti-Afk", "Read the name");
    }

    @SuppressWarnings("unused")
    private final SettingGroup sGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> jumpToggle = sGeneral.add(new BoolSetting.Builder()
            .name("jump-toggle")
            .description("Toggles jumping")
            .defaultValue(false)
            .build());

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (mc.player != null && mc.world != null) {
            mc.player.yaw = (mc.player.yaw == 360 || mc.player.yaw > 360) ? 0 : mc.player.yaw + 2;
            if (jumpToggle.get() && mc.options.keyJump.isPressed())
                ((IKeyBinding) mc.options.keyJump).setPressed(false);
            else if (jumpToggle.get())
                ((IKeyBinding) mc.options.keyJump).setPressed(true);
        }

    });
}