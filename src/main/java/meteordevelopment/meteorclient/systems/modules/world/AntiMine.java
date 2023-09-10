/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import java.util.List;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.text.Text;

public class AntiMine extends Module {
    private static final long MIN_NOTIFY_DELAY = 1000;
    private long lastNotify = 0;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEnchantments = settings.createGroup("Enchantments");

    private final Setting<List<Block>> selectedBlocks = sgGeneral.add(new BlockListSetting.Builder()
          .name("blocks")
          .description("Which blocks to disallow mining")
          .defaultValue(Blocks.BUDDING_AMETHYST, Blocks.SPAWNER)
          .build()
    );

    private final Setting<ListMode> mode = sgGeneral.add(new EnumSetting.Builder<ListMode>()
           .name("mode")
           .description("Selection mode.")
           .defaultValue(ListMode.Blacklist)
           .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
            .name("Notify")
            .description("Inform in chat when this module prevented you from mining a block")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> checkSilkTouch = sgEnchantments.add(new BoolSetting.Builder()
          .name("Silk Touch only")
          .description("Allow selected blocks to only be mined when using a tool enchanted with Silk Touch")
          .defaultValue(true)
          .build()
    );

    private final Setting<List<Block>> blocksSilkTouch = sgEnchantments.add(new BlockListSetting.Builder()
          .name("Silk touch only blocks")
          .description("Blocks to only mine when using silk touch")
          .build()
    );

    private final Setting<Boolean> checkFortune = sgEnchantments.add(new BoolSetting.Builder()
          .name("Fortune only")
          .description("Allow selected blocks to only be mined when using a tool enchanted with Fortune")
          .defaultValue(true)
          .build()
    );

    private final Setting<List<Block>> blocksFortune = sgEnchantments.add(new BlockListSetting.Builder()
         .name("Fortune only blocks")
         .description("Blocks to only mine when using fortune")
         .build()
    );

    public AntiMine() {
        super(Categories.World, "anti-mine", "Prevent accidentally breaking blocks you want to keep.");
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        Block block = mc.world.getBlockState(event.blockPos).getBlock();

        if (mode.get() == ListMode.Whitelist && !selectedBlocks.get().contains(block)) {
            event.setCancelled(true);
            notifyMiningPrevented(block);
        }
        if (mode.get() == ListMode.Blacklist && selectedBlocks.get().contains(block)) {
            event.setCancelled(true);
            notifyMiningPrevented(block);
        }

        if (checkSilkTouch.get() && blocksSilkTouch.get().contains(block) && !hasEnchantment(Enchantments.SILK_TOUCH)) {
            event.setCancelled(true);
            notifyMiningPrevented(block, Enchantments.SILK_TOUCH);
        }

        if (checkFortune.get() && blocksFortune.get().contains(block) && !hasEnchantment(Enchantments.FORTUNE)) {
            event.setCancelled(true);
            notifyMiningPrevented(block, Enchantments.FORTUNE);
        }
    }

    private boolean hasEnchantment(Enchantment e) {
        return EnchantmentHelper.getLevel(e, mc.player.getMainHandStack()) > 0;
    }

    private void notifyMiningPrevented(Block block, Enchantment enchantment) {
        if (!notify.get()) {
            return;
        }

        // minimum time between messages so holding mouse1 on a block won't send them every tick
        long timeCurrent = System.currentTimeMillis();
        if (timeCurrent - lastNotify < MIN_NOTIFY_DELAY) {
            return;
        }
        lastNotify = timeCurrent;

        String msg = String.format("Not breaking %s", block.getName().getString().toLowerCase());
        if (enchantment != null) {
            msg += ", missing " + Text.translatable(enchantment.getTranslationKey()).getString().toLowerCase();
        }

        info(msg);
    }

    private void notifyMiningPrevented(Block block) {
        notifyMiningPrevented(block, null);
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
