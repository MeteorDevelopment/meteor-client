/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import java.util.Arrays;
import java.util.List;

public class AutoSign extends Module {

    private final SettingGroup front = settings.createGroup("Front");
    private final SettingGroup back = settings.createGroup("Back");
    private final SettingGroup other = settings.createGroup("Other");

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
        .onChanged(strings -> recompile(strings, true, 0))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(frontEnabled::get)
        .build()
    );

    private final Setting<String> line2front = front.add(new StringSetting.Builder()
        .name("front-line-2")
        .description("Text in second line.")
        .defaultValue("was here")
        .onChanged(strings -> recompile(strings, true, 1))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(frontEnabled::get)
        .build()
    );

    private final Setting<String> line3front = front.add(new StringSetting.Builder()
        .name("front-line-3")
        .description("Text in third line.")
        .defaultValue("{date}")
        .onChanged(strings -> recompile(strings, true, 2))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(frontEnabled::get)
        .build()
    );

    private final Setting<String> line4front = front.add(new StringSetting.Builder()
        .name("front-line-4")
        .description("Text in fourth line.")
        .defaultValue("{time}")
        .onChanged(strings -> recompile(strings, true, 3))
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
        .onChanged(strings -> recompile(strings, false, 0))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(backEnabled::get)
        .build()
    );

    // It's MuffinTime.
    private final Setting<String> line2back = back.add(new StringSetting.Builder()
        .name("back-line-2")
        .description("Text in second line.")
        .defaultValue("It's MeteorTime.")
        .onChanged(strings -> recompile(strings, false, 1))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(backEnabled::get)
        .build()
    );

    private final Setting<String> line3back = back.add(new StringSetting.Builder()
        .name("back-line-3")
        .description("Text in third line.")
        .onChanged(strings -> recompile(strings, false, 2))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(backEnabled::get)
        .build()
    );

    private final Setting<String> line4back = back.add(new StringSetting.Builder()
        .name("back-line-4")
        .description("Text in fourth line.")
        .onChanged(strings -> recompile(strings, false, 3))
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

    private BlockPos signPlaced = null;
    private boolean signUpdated = false;
    private boolean frontToWrite;
    private boolean backToWrite;
    private int expectedPacket = 0;
    private final List<Script> frontScripts = Arrays.asList(new Script[4]);
    private final List<Script> backScripts = Arrays.asList(new Script[4]);

    public AutoSign() {
        super(Categories.World, "auto-sign", "Automatically writes signs. Configure what to write dynamically using Starscript placeholders.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerInteractBlockC2SPacket)) return;
        if (expectedPacket == 1) {
            // we send this packet in restartInteraction
            expectedPacket = 2;
        } else if (expectedPacket == 2) {
            // as we already received restartInteraction packet
            // but the expected packet was not reset by onOpenScreen
            // we cant apply back sign
            expectedPacket = 0;
            backToWrite = false;
            signPlaced = null;
            if (backEnabled.get()) {
                warning("It seems like that this server does not support sign editing, disabling back sign writing is recommended.");
            }
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) throws InterruptedException {
        if (!(event.screen instanceof AbstractSignEditScreen)) {return;}

        SignBlockEntity sign = ((AbstractSignEditScreenAccessor) event.screen).getSign();

        if (signPlaced == null) {
            signPlaced = sign.getPos();
            frontToWrite = frontEnabled.get();
            backToWrite = backEnabled.get();
        }

        if (frontToWrite) {
            boolean updated = writeSign(sign, true);
            if (updated) {
                signUpdated = true;
            }
            frontToWrite = false;
            if (backToWrite) {
                // onOpenScreen will be called a second time if server accepts interaction
                restartInteraction(sign);
            }
        } else if (backToWrite) {
            // check if second sign interaction is for the same sign initially placed
            if (!signPlaced.equals(sign.getPos())) {
                warning("It seems like that this server does not support sign editing, disabling back sign writing is recommended.");
            }
            boolean updated = writeSign(sign, false);
            if (updated) {
                signUpdated = true;
            }
            backToWrite = false;
            if (expectedPacket == 2) {
                // Server accepted sign edit
                expectedPacket = 0;
            }
        }

        if (!frontToWrite && !backToWrite) {
            signPlaced = null;
        }

        // Cancel GUI only when something was updated
        if (signUpdated || signPlaced != null) {
            signUpdated = false;
            event.cancel();
        }
    }

    private boolean writeSign(SignBlockEntity sign, boolean front) {
        List<Script> script;
        SignText signText;
        if (front) {
            script = frontScripts;
            signText = sign.getFrontText();
        } else {
            script = backScripts;
            signText = sign.getBackText();
        }

        // Check if side of sign is already written
        boolean written = false;
        for (Text text : signText.getMessages(false)) {
            if (!text.getString().isEmpty() && !written) {
                written = true;
            }
        }

        if (overwrite.get() || !written) {
            String line0 = script.get(0) != null ? MeteorStarscript.run(script.get(0)) : "error";
            String line1 = script.get(1) != null ? MeteorStarscript.run(script.get(1)) : "error";
            String line2 = script.get(2) != null ? MeteorStarscript.run(script.get(2)) : "error";
            String line3 = script.get(3) != null ? MeteorStarscript.run(script.get(3)) : "error";
            mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(sign.getPos(), front,
                line0 != null ? line0 : "error",
                line1 != null ? line1 : "error",
                line2 != null ? line2 : "error",
                line3 != null ? line3 : "error"));
            return true;
        }
        return false;
    }

    // Try a new sign interaction
    // We can only improve chances, that this interaction triggers sign edit
    // Since in the end the server decides, what to do with this interaction (as far as I know...)
    // (if it allows sign edit or as example instead places a block)
    private void restartInteraction(SignBlockEntity sign) {

        // If sign was placed with offhand
        boolean offhand_swapped = false;
        if (!mc.player.getMainHandStack().getTranslationKey().endsWith("sign") && !mc.player.getMainHandStack().getTranslationKey().endsWith("air")) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, mc.player.getBlockPos(), mc.player.getFacing()));
            offhand_swapped = true;
        }

        // Avoid sneaking during interaction
        boolean wasSneaking = mc.player.isSneaking();
        if (wasSneaking) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
        BlockHitResult bhr = new BlockHitResult(sign.getPos().toCenterPos(), mc.player.getFacing().getOpposite(), sign.getPos(), false);

        // expectedPacket shenanigans are used to identify if server accepted the sign edit
        expectedPacket = 1;
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, bhr, 0));

        if (wasSneaking) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        }

        if (offhand_swapped) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, mc.player.getBlockPos(), mc.player.getFacing()));
        }
    }

    private void recompile(String compileLine, boolean front, int line) {
        List<Script> sideScripts;
        if (front) {
            sideScripts = frontScripts;
        } else {
            sideScripts = backScripts;
        }

        Script script = MeteorStarscript.compile(compileLine);
        sideScripts.set(line, script);
    }
}
