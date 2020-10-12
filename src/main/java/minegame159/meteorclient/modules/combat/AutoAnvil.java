package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

// Created by Eureka

public class AutoAnvil extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
            .name("height")
            .description("How high to place the anvils.")
            .defaultValue(5)
            .min(0)
            .max(10)
            .sliderMin(0)
            .sliderMax(10)
            .build()
    );

    public AutoAnvil() {
        super(Category.Combat, "auto-anvil", "Automatically places anvils above players.");
    }

    private PlayerEntity target = null;

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        int obsidianSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStack(i).getItem() == Blocks.ANVIL.asItem()) {
                obsidianSlot = i;
                break;
            }
        }
        if (obsidianSlot == -1) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !FriendManager.INSTANCE.attack(player)) continue;

            if (target == null) {
                target = player;
            } else if (mc.player.distanceTo(target) > mc.player.distanceTo(player)) {
                target = player;
            }
        }

        if (mc.player.distanceTo(target) < 4) {
            int prevSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = obsidianSlot;
            BlockPos targetPos = target.getBlockPos().up();

            if (mc.world.getBlockState(targetPos.add(0, height.get(), 0)).isAir()) {
                mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos().add(0, height.get(), 0), Direction.UP, targetPos.add(0, height.get(), 0), false));
                mc.player.swingHand(Hand.MAIN_HAND);
            }

            mc.player.inventory.selectedSlot = prevSlot;
        }
    });
}
