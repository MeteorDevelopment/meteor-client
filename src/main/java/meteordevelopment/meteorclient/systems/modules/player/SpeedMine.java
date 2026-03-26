/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayerInteractionManagerAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Block;
import java.util.List;

import static net.minecraft.world.effect.MobEffects.HASTE;

public class SpeedMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .defaultValue(Mode.Damage)
        .onChanged(mode -> removeHaste())
        .build()
    );

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Selected blocks.")
        .filter(block -> block.defaultDestroyTime() > 0)
        .visible(() -> mode.get() != Mode.Haste)
        .build()
    );

    private final Setting<ListMode> blocksFilter = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("blocks-filter")
        .description("How to use the blocks setting.")
        .defaultValue(ListMode.Blacklist)
        .visible(() -> mode.get() != Mode.Haste)
        .build()
    );

    public final Setting<Double> modifier = sgGeneral.add(new DoubleSetting.Builder()
        .name("modifier")
        .description("Mining speed modifier. An additional value of 0.2 is equivalent to one haste level (1.2 = haste 1).")
        .defaultValue(1.4)
        .visible(() -> mode.get() == Mode.Normal)
        .min(0)
        .build()
    );

    private final Setting<Integer> hasteAmplifier = sgGeneral.add(new IntSetting.Builder()
        .name("haste-amplifier")
        .description("What value of haste to give you. Above 2 not recommended.")
        .defaultValue(2)
        .min(1)
        .visible(() -> mode.get() == Mode.Haste)
        .onChanged(i -> removeHaste())
        .build()
    );

    private final Setting<Boolean> instamine = sgGeneral.add(new BoolSetting.Builder()
        .name("instamine")
        .description("Whether or not to instantly mine blocks under certain conditions.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Damage)
        .build()
    );

    private final Setting<Boolean> grimBypass = sgGeneral.add(new BoolSetting.Builder()
        .name("grim-bypass")
        .description("Bypasses Grim's fastbreak check, working as of 2.3.58")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Damage)
        .build()
    );

    public SpeedMine() {
        super(Categories.Player, "speed-mine", "Allows you to quickly mine blocks.");
    }

    @Override
    public void onDeactivate() {
        removeHaste();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        if (mode.get() == Mode.Haste) {
            MobEffectInstance haste = mc.player.getEffect(HASTE);

            if (haste == null || haste.getAmplifier() <= hasteAmplifier.get() - 1) {
                mc.player.forceAddEffect(new MobEffectInstance(HASTE, -1, hasteAmplifier.get() - 1, false, false, false), null);
            }
        }
        else if (mode.get() == Mode.Damage) {
            ClientPlayerInteractionManagerAccessor im = (ClientPlayerInteractionManagerAccessor) mc.gameMode;
            float progress = im.meteor$getBreakingProgress();
            BlockPos pos = im.meteor$getCurrentBreakingBlockPos();

            if (pos == null || progress <= 0) return;
            if (progress + mc.level.getBlockState(pos).getDestroyProgress(mc.player, mc.level, pos) >= 0.7f)
                im.meteor$setCurrentBreakingProgress(1f);
        }
    }

    @EventHandler
    private void onPacket(PacketEvent.Send event) {
        if (!(mode.get() == Mode.Damage) || !grimBypass.get()) return;

        // https://github.com/GrimAnticheat/Grim/issues/1296
        if (event.packet instanceof ServerboundPlayerActionPacket packet && packet.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos().above(), packet.getDirection()));
        }
    }

    private void removeHaste() {
        if (!Utils.canUpdate()) return;

        MobEffectInstance haste = mc.player.getEffect(HASTE);
        if (haste != null && !haste.showIcon()) mc.player.removeEffect(HASTE);
    }

    public boolean filter(Block block) {
        if (blocksFilter.get() == ListMode.Blacklist && !blocks.get().contains(block)) return true;
        return blocksFilter.get() == ListMode.Whitelist && blocks.get().contains(block);
    }

    public boolean instamine() {
        return isActive() && mode.get() == Mode.Damage && instamine.get();
    }

    public enum Mode {
        Normal,
        Haste,
        Damage
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
