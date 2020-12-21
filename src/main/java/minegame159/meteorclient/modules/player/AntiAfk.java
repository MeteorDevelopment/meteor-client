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
import minegame159.meteorclient.utils.Utils;

import java.util.Random;

public class AntiAfk extends ToggleModule {

    public AntiAfk() {
        super(Category.Player, "Anti-Afk", "Read the name");
    }

    @SuppressWarnings("unused")
    private final SettingGroup sGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> spinToggle = sGeneral.add(new BoolSetting.Builder()
            .name("spin")
            .description("Spin to win")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> jumpToggle = sGeneral.add(new BoolSetting.Builder()
            .name("jump")
            .description("Toggles jumping")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> clickToggle = sGeneral.add(new BoolSetting.Builder()
            .name("click")
            .description("Click Toggle")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> discoToggle = sGeneral.add(new BoolSetting.Builder()
            .name("disco")
            .description("Disco disco")
            .defaultValue(false)
            .build());

    Random random = new Random();

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (mc.player != null && mc.world != null) {
            if (spinToggle.get())
                mc.player.yaw = (mc.player.yaw == 360 || mc.player.yaw > 360) ? 0 : mc.player.yaw + random.nextInt(7) + 1;
            if (jumpToggle.get() && mc.options.keyJump.isPressed())
                ((IKeyBinding) mc.options.keyJump).setPressed(false);
            else if (jumpToggle.get() && random.nextInt(99) + 1 == 50)
                ((IKeyBinding) mc.options.keyJump).setPressed(true);
            if (clickToggle.get() && random.nextInt(99) + 1 == 45) {
                Utils.leftClick();
            }
            if (jumpToggle.get() && mc.options.keySneak.isPressed())
                ((IKeyBinding) mc.options.keySneak).setPressed(false);
            else if (discoToggle.get() && random.nextInt(24) + 1 == 15) {
                ((IKeyBinding) mc.options.keySneak).setPressed(true);
            }


        }
    });
}