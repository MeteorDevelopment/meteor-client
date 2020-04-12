package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.events.packets.PlaySoundPacketEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;

public class AutoFish extends ToggleModule {
    private Setting<Boolean> autoCast = addSetting(new BoolSetting.Builder()
            .name("auto-cast")
            .description("Automatically casts when not fishing.")
            .defaultValue(true)
            .build()
    );

    private Setting<Integer> ticksAutoCast = addSetting(new IntSetting.Builder()
            .name("ticks-auto-cast")
            .description("Ticks to wait before auto casting.")
            .defaultValue(10)
            .min(0)
            .build()
    );

    private Setting<Integer> ticksCatch = addSetting(new IntSetting.Builder()
            .name("ticks-catch")
            .description("Ticks to wait before catching the fish")
            .defaultValue(6)
            .min(0)
            .build()
    );

    private Setting<Integer> ticksThrow = addSetting(new IntSetting.Builder()
            .name("ticks-throw")
            .description("Ticks to wait before throwing the bobber.")
            .defaultValue(14)
            .min(0)
            .build()
    );

    private Setting<Double> splashDetectionRange = addSetting(new DoubleSetting.Builder()
            .name("splash-detection-range")
            .description("Detection range of splash sound. Lowe values will not work when TPS is low.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private boolean ticksEnabled;
    private int ticksToRightClick;
    private int ticksData;

    private int autoCastTimer;
    private boolean autoCastEnabled;

    private int autoCastCheckTimer;

    public AutoFish() {
        super(Category.Player, "auto-fish", "Automatically fishes.");
    }

    @Override
    public void onActivate() {
        ticksEnabled = false;
        autoCastEnabled = false;
        autoCastCheckTimer = 0;
    }

    @EventHandler
    private Listener<PlaySoundPacketEvent> onPlaySoundPacket = new Listener<>(event -> {
        PlaySoundS2CPacket p = event.packet;
        FishingBobberEntity b = mc.player.fishHook;

        if (p.getSound().getId().getPath().equals("entity.fishing_bobber.splash") && isIdk(p.getX(), b.x) && isIdk(p.getY(), b.y) && isIdk(p.getZ(), b.z)) {
            ticksEnabled = true;
            ticksToRightClick = ticksCatch.get();
            ticksData = 0;
        }
    });

    private boolean isIdk(double a1, double a2) {
        return a1 >= a2 - splashDetectionRange.get() && a1 <= a2 + splashDetectionRange.get();
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        // Auto cast
        if (autoCastCheckTimer <= 0) {
            autoCastCheckTimer = 20;

            if (autoCast.get() && !ticksEnabled && !autoCastEnabled && mc.player.fishHook == null && mc.player.getMainHandStack().getItem() instanceof FishingRodItem) {
                autoCastTimer = 0;
                autoCastEnabled = true;
            }
        } else {
            autoCastCheckTimer--;
        }

        // Check for auto cast timer
        if (autoCastEnabled) {
            autoCastTimer++;

            if (autoCastTimer > ticksAutoCast.get()) {
                autoCastEnabled = false;
                Utils.rightClick();
            }
        }

        // Handle logic
        if (ticksEnabled && ticksToRightClick <= 0) {
            if (ticksData == 0) {
                Utils.rightClick();
                ticksToRightClick = ticksThrow.get();
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
