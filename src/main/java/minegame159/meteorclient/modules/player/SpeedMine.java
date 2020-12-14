package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.entity.player.BlockBreakingProgressEvent;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.mixin.ClientPlayerInteractionManagerInvoker;
import minegame159.meteorclient.mixininterface.IClientPlayerInteractionManager;
import minegame159.meteorclient.mixininterface.IStatusEffectInstance;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Formatting;

import static net.minecraft.entity.effect.StatusEffects.HASTE;

public class SpeedMine extends ToggleModule {

    public enum Mode {
        Normal,
        Haste_1,
        Haste_2,
        Packet
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .defaultValue(Mode.Normal)
            .onChanged(mode -> {
                if(mode != Mode.Packet)
                    return;
                Chat.warning(this, "While in PACKET mode, hitting a block %sonce%s will eventually break it.", Formatting.ITALIC, Formatting.YELLOW);
            })
            .build()
    );
    public final Setting<Double> modifier = sgGeneral.add(new DoubleSetting.Builder()
            .name("modifier")
            .description("Speed modifier (only normal mode!). An additional value of 0.2 is equivalent to one haste level (1.2 = haste 1).")
            .defaultValue(1.6D)
            .min(0D)
            .sliderMin(1D)
            .sliderMax(10D)
            .build()
    );

    public SpeedMine() {
        super(Category.Player, "speed-mine", "Lets you break blocks faster.");
    }

    @EventHandler
    public final Listener<PostTickEvent> onTick = new Listener<>(e -> {
        Mode mode = this.mode.get();

        if (mode == Mode.Haste_1 || mode == Mode.Haste_2) {
            int amplifier = mode == Mode.Haste_2 ? 1 : 0;
            if (mc.player.hasStatusEffect(HASTE)) {
                StatusEffectInstance effect = mc.player.getStatusEffect(HASTE);
                ((IStatusEffectInstance) effect).setAmplifier(amplifier);
                if (effect.getDuration() < 20) {
                    ((IStatusEffectInstance) effect).setDuration(20);
                }
            } else {
                mc.player.addStatusEffect(new StatusEffectInstance(HASTE, 20, amplifier, false, false, false));
            }
        }
    });

    @EventHandler
    public final Listener<BlockBreakingProgressEvent> onBlockBreakProgress = new Listener<>(e -> {
        if (mode.get() != Mode.Packet)
            return;

        ClientPlayerInteractionManager man = mc.interactionManager;
        if (!man.isBreakingBlock())
            return;

        if (((IClientPlayerInteractionManager) man).getBreakingProgress() >= 1)
            return;

        PlayerActionC2SPacket.Action action = PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK;
        ((ClientPlayerInteractionManagerInvoker) man).invokeSendPlayerAction(action, e.blockPos, e.direction);
    });
}
