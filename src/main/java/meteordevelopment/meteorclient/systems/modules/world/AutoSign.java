/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.StandardLib;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.network.packet.c2s.play.*;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.starscript.Script;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import java.util.*;

public class AutoSign extends Module {

    private boolean applyingBack = false;

    private final SettingGroup front = settings.createGroup("Front");
    private final SettingGroup back = settings.createGroup("Back");
    private final SettingGroup other = settings.createGroup("Other");

    private final List<Script> frontScripts = new ArrayList<>();
    private final List<Script> backScripts = new ArrayList<>();

    // 4 lines at front of sign
    public final Setting<Boolean> frontEnabled = front.add(new BoolSetting.Builder()
        .name("front-enabled")
        .description("Enable auto writing front of sign.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> line1front = front.add(new StringSetting.Builder()
        .name("front-line-1")
        .description("Text in first line.")
        .defaultValue("{player}")
        .onChanged(strings -> recompile(front, frontScripts, 1))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(frontEnabled::get)
        .build()
    );

    private final Setting<String> line2front = front.add(new StringSetting.Builder()
        .name("front-line-2")
        .description("Text in second line.")
        .defaultValue("was here")
        .onChanged(strings -> recompile(front, frontScripts, 2))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(frontEnabled::get)
        .build()
    );

    private final Setting<String> line3front = front.add(new StringSetting.Builder()
        .name("front-line-3")
        .description("Text in third line.")
        .defaultValue("{date}")
        .onChanged(strings -> recompile(front, frontScripts, 3))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(frontEnabled::get)
        .build()
    );

    private final Setting<String> line4front = front.add(new StringSetting.Builder()
        .name("front-line-4")
        .description("Text in fourth line.")
        .defaultValue("{time}")
        .onChanged(strings -> recompile(front, frontScripts, 4))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(frontEnabled::get)
        .build()
    );

    // 4 lines at back of sign
    public final Setting<Boolean> backEnabled = back.add(new BoolSetting.Builder()
        .name("back-enabled")
        .description("Enable auto writing back of sign.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> line1back = back.add(new StringSetting.Builder()
        .name("back-line-1")
        .description("Text in first line.")
        .onChanged(strings -> recompile(back, backScripts, 1))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(backEnabled::get)
        .build()
    );

    // It's MuffinTime.
    private final Setting<String> line2back = back.add(new StringSetting.Builder()
        .name("back-line-2")
        .description("Text in second line.")
        .defaultValue("It's MeteorTime.")
        .onChanged(strings -> recompile(back, backScripts, 2))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(backEnabled::get)
        .build()
    );

    private final Setting<String> line3back = back.add(new StringSetting.Builder()
        .name("back-line-3")
        .description("Text in third line.")
        .onChanged(strings -> recompile(back, backScripts, 3))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(backEnabled::get)
        .build()
    );

    private final Setting<String> line4back = back.add(new StringSetting.Builder()
        .name("back-line-4")
        .description("Text in fourth line.")
        .onChanged(strings -> recompile(back, backScripts, 4))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(backEnabled::get)
        .build()
    );

    // Other settings
    public final Setting<Boolean> overwrite = other.add(new BoolSetting.Builder()
        .name("overwrite-existing")
        .description("Overwrite existing text on signs, when possible.")
        .defaultValue(false)
        .build());

    private final Setting<String> date = other.add(new StringSetting.Builder()
        .name("date-format")
        .description("Format string for {date} object.")
        .defaultValue("yyyy-MM-dd")
        .build()
    );

    private final Setting<String> time = other.add(new StringSetting.Builder()
        .name("time-format")
        .description("Format string for {time} object.")
        .defaultValue("HH:mm z")
        .build()
    );

    private final Setting<Boolean> utcTimezone = other.add(new BoolSetting.Builder()
        .name("UTC-timezone")
        .description("Whether to use UTC or your local timezone.")
        .defaultValue(true)
        .build()
    );

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WHorizontalList list = theme.horizontalList();

        WButton starscriptDoc = theme.button("Open Starscript documentation.");
        starscriptDoc.action = () -> Util.getOperatingSystem().open("https://github.com/MeteorDevelopment/meteor-client/wiki/Starscript");
        list.add(starscriptDoc);

        WButton simpleDateFormatDoc = theme.button("Open SimpleDateFormat documentation.");
        simpleDateFormatDoc.action = () -> Util.getOperatingSystem().open("https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html");
        list.add(simpleDateFormatDoc);

        return list;
    }

    public AutoSign() {
        super(Categories.World, "auto-sign", "Automatically writes signs. Configure what to write dynamically using Starscript placeholders.");
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) throws InterruptedException {
        if (!(event.screen instanceof AbstractSignEditScreen)) return;
        if (applyingBack) {
            applyingBack = false;
            event.cancel();
            return;
        }

        // apply custom format and timezone, not the nicest (temporary overwriting global date and time format)
        try {
            StandardLib.dateFormat.applyPattern(date.get());
            StandardLib.timeFormat.applyPattern(time.get());
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        }
        if (utcTimezone.get()) {
            StandardLib.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            StandardLib.timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        boolean updated = false;
        SignBlockEntity sign = ((AbstractSignEditScreenAccessor) event.screen).getSign();

        if (frontEnabled.get()) {
            boolean written = false;
            for (Text text : sign.getFrontText().getMessages(false))
                if (!Objects.equals(text.getString(), "") && !written)
                    written = true;
            if (overwrite.get() || !written) {
                mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(sign.getPos(), true,
                    frontScripts.get(0) != null ? MeteorStarscript.run(frontScripts.get(0)) : "error",
                    frontScripts.get(1) != null ? MeteorStarscript.run(frontScripts.get(1)) : "error",
                    frontScripts.get(2) != null ? MeteorStarscript.run(frontScripts.get(2)) : "error",
                    frontScripts.get(3) != null ? MeteorStarscript.run(frontScripts.get(3)) : "error"));
                updated = true;
            }
        }

        // Start a second sign interaction (if applicable) to apply second side of sign
        if (updated && backEnabled.get()) {
            applyingBack = true;

            // If sign was placed with offhand
            boolean offhand_swapped = false;
            if (!mc.player.getMainHandStack().getTranslationKey().endsWith("sign") && !mc.player.getMainHandStack().getTranslationKey().endsWith("air")) {
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, mc.player.getBlockPos(), mc.player.getFacing()));
                offhand_swapped = true;
            }

            boolean wasSneaking = mc.player.isSneaking();
            if (wasSneaking) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));

            BlockHitResult bhr = new BlockHitResult(sign.getPos().toCenterPos(), mc.player.getFacing().getOpposite(), sign.getPos(), false);
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, bhr, 0));

            if (wasSneaking) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));

            if (offhand_swapped) mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, mc.player.getBlockPos(), mc.player.getFacing()));
        }

        if (backEnabled.get()) {
            boolean written = false;
            for (Text text : sign.getBackText().getMessages(false))
                if (!Objects.equals(text.getString(), "") && !written)
                    written = true;
            if (overwrite.get() || !written) {
                mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(sign.getPos(), false,
                    backScripts.get(0) != null ? MeteorStarscript.run(backScripts.get(0)) : "error",
                    backScripts.get(1) != null ? MeteorStarscript.run(backScripts.get(1)) : "error",
                    backScripts.get(2) != null ? MeteorStarscript.run(backScripts.get(2)) : "error",
                    backScripts.get(3) != null ? MeteorStarscript.run(backScripts.get(3)) : "error"));
                updated = true;
            }
        }

        // reset formating and timezones
        StandardLib.dateFormat.applyPattern("dd. MM. yyyy");
        StandardLib.timeFormat.applyPattern("HH:mm");
        if (utcTimezone.get()) {
            StandardLib.dateFormat.setTimeZone(TimeZone.getDefault());
            StandardLib.timeFormat.setTimeZone(TimeZone.getDefault());
        }

        // When no sign side was updated open gui normally
        if (updated)
            event.cancel();
    }

    private void recompile(SettingGroup side, List<Script> sideScripts, int line) {
        String compileLine = (String) side.getByIndex(line).get();

        Script script = MeteorStarscript.compile(compileLine);

        if (sideScripts.size() <= line - 1)
            sideScripts.add(script);
        else
            sideScripts.set(line - 1, script);
    }
}
