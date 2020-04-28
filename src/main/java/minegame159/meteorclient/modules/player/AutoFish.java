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
            .group("General")
            .defaultValue(true)
            .build()
    );

    private Setting<Integer> ticksAutoCast = addSetting(new IntSetting.Builder()
            .name("ticks-auto-cast")
            .description("Ticks to wait before auto casting.")
            .group("General")
            .defaultValue(10)
            .min(0)
            .sliderMax(60)
            .build()
    );

    private Setting<Integer> ticksCatch = addSetting(new IntSetting.Builder()
            .name("ticks-catch")
            .description("Ticks to wait before catching the fish")
            .group("General")
            .defaultValue(6)
            .min(0)
            .sliderMax(60)
            .build()
    );

    private Setting<Integer> ticksThrow = addSetting(new IntSetting.Builder()
            .name("ticks-throw")
            .description("Ticks to wait before throwing the bobber.")
            .group("General")
            .defaultValue(14)
            .min(0)
            .sliderMax(60)
            .build()
    );

    private Setting<Double> splashDetectionRange;
    private Setting<Boolean> splashRangeDetection = addSetting(new BoolSetting.Builder()
            .name("splash-range-detection")
            .description("Allows you to use multiple accoutns next to each other.")
            .group("Splash Sound Range Detection")
            .defaultValue(false)
            .onChanged(aBoolean -> splashDetectionRange.setVisible(aBoolean))
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

        splashDetectionRange = addSetting(new DoubleSetting.Builder()
                .name("splash-detection-range")
                .description("Detection range of splash sound. Low values will not work when TPS is low.")
                .group("Splash Sound Range Detection")
                .defaultValue(10)
                .min(0)
                .visible(false)
                .build()
        );
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

        if (p.getSound().getId().getPath().equals("entity.fishing_bobber.splash")) {
            if (!splashRangeDetection.get() || Utils.distance(b.getX(), b.getY(), b.getZ(), p.getX(), p.getY(), p.getZ()) <= splashDetectionRange.get()) {
                ticksEnabled = true;
                ticksToRightClick = ticksCatch.get();
                ticksData = 0;
            }
        }
    });

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        // Auto cast
        if (autoCastCheckTimer <= 0) {
            autoCastCheckTimer = 30;

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
