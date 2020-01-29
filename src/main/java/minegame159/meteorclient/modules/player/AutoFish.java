package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.events.packets.PlaySoundPacketEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.BoolSettingBuilder;
import minegame159.meteorclient.settings.builders.IntSettingBuilder;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.item.FishingRodItem;

public class AutoFish extends Module {
    private Setting<Boolean> autoCast = addSetting(new BoolSettingBuilder()
            .name("auto-cast")
            .description("Automatically casts when activated.")
            .defaultValue(true)
            .build()
    );

    private Setting<Integer> ticksCatch = addSetting(new IntSettingBuilder()
            .name("ticks-catch")
            .description("Ticks to wait before catching the fish")
            .defaultValue(6)
            .min(0)
            .build()
    );

    private Setting<Integer> ticksThrow = addSetting(new IntSettingBuilder()
            .name("ticks-throw")
            .description("Ticks to wait before throwing the bobber.")
            .defaultValue(14)
            .min(0)
            .build()
    );

    private boolean ticksEnabled;
    private int ticksToRightClick;
    private int ticksData;

    public AutoFish() {
        super(Category.Player, "auto-fish", "Automatically fishes.");
    }

    @Override
    public void onActivate() {
        ticksEnabled = false;
        if (autoCast.value() && mc.player.getMainHandStack().getItem() instanceof FishingRodItem) Utils.rightClick();
    }

    @EventHandler
    private Listener<PlaySoundPacketEvent> onPlaySoundPacket = new Listener<>(event -> {
        if (event.packet.getSound().getId().getPath().equals("entity.fishing_bobber.splash")) {
            ticksEnabled = true;
            ticksToRightClick = ticksCatch.value();
            ticksData = 0;
        }
    });

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (ticksEnabled && ticksToRightClick <= 0) {
            if (ticksData == 0) {
                Utils.rightClick();
                ticksToRightClick = ticksThrow.value();
                ticksData = 1;
            }
            else if (ticksData == 1) {
                Utils.rightClick();
                ticksEnabled = false;
            }
        }

        ticksToRightClick--;
    });

    @EventHandler
    private Listener<KeyEvent> onKey = new Listener<>(event -> {
        if (mc.options.keyUse.isPressed()) ticksEnabled = false;
    });
}
