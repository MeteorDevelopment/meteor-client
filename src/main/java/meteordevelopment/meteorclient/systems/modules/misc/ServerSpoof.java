/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.text.RunnableClickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ServerSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> spoofBrand = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-brand")
        .description("Whether or not to spoof the brand.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> brand = sgGeneral.add(new StringSetting.Builder()
        .name("brand")
        .description("Specify the brand that will be send to the server.")
        .defaultValue("vanilla")
        .visible(spoofBrand::get)
        .build()
    );

    private final Setting<Boolean> resourcePack = sgGeneral.add(new BoolSetting.Builder()
        .name("resource-pack")
        .description("Spoof accepting server resource pack.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> blockChannels = sgGeneral.add(new BoolSetting.Builder()
        .name("block-channels")
        .description("Whether or not to block some channels.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<String>> channels = sgGeneral.add(new StringListSetting.Builder()
        .name("channels")
        .description("If the channel contains the keyword, this outgoing channel will be blocked.")
        .defaultValue("fabric", "minecraft:register")
        .visible(blockChannels::get)
        .build()
    );

    public ServerSpoof() {
        super(Categories.Misc, "server-spoof", "Spoof client brand, resource pack and channels.");

        runInMainMenu = true;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!isActive() || !(event.packet instanceof CustomPayloadC2SPacket)) return;
        Identifier id = ((CustomPayloadC2SPacket) event.packet).payload().getId().id();

        if (blockChannels.get()) {
            for (String channel : channels.get()) {
                if (StringUtils.containsIgnoreCase(id.toString(), channel)) {
                    event.cancel();
                    return;
                }
            }
        }

        if (spoofBrand.get() && id.equals(BrandCustomPayload.ID.id())) {
            CustomPayloadC2SPacket spoofedPacket = new CustomPayloadC2SPacket(new BrandCustomPayload(brand.get()));

            // PacketEvent.Send doesn't trigger if we send the packet like this
            event.connection.send(spoofedPacket, null, true);
            event.cancel();
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!isActive()) return;

        if (resourcePack.get()) {
            if (!(event.packet instanceof ResourcePackSendS2CPacket packet)) return;
            event.cancel();

            MutableText msg = Text.literal("This server has ");
            msg.append(packet.required() ? "a required " : "an optional ").append("resource pack. ");

            MutableText link = Text.literal("[Download]");
            link.setStyle(link.getStyle()
                .withColor(Formatting.BLUE)
                .withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, packet.url()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to download")))
            );

            MutableText acceptance = Text.literal("[Spoof Acceptance]");
            acceptance.setStyle(acceptance.getStyle()
                .withColor(Formatting.DARK_GREEN)
                .withUnderline(true)
                .withClickEvent(new RunnableClickEvent(() -> {
                    event.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
                    event.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
                }))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to spoof accepting the recourse pack.")))
            );

            msg.append(link).append(" ");
            msg.append(acceptance).append(".");
            info(msg);
        }
    }
}
