/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.text.RunnableClickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.Strings;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

    private MutableComponent msg;
    public boolean silentAcceptResourcePack = false;

    public ServerSpoof() {
        super(Categories.Misc, "server-spoof", "Spoof client brand, resource pack and channels.");

        runInMainMenu = true;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!isActive()) return;

        if (event.packet instanceof ServerboundCustomPayloadPacket customPayloadPacket) {
            Identifier id = customPayloadPacket.payload().type().id();

            if (blockChannels.get()) {
                for (String channel : channels.get()) {
                    if (Strings.CI.contains(id.toString(), channel)) {
                        event.cancel();
                        return;
                    }
                }
            }

            if (spoofBrand.get() && id.equals(BrandPayload.TYPE.id())) {
                ServerboundCustomPayloadPacket spoofedPacket = new ServerboundCustomPayloadPacket(new BrandPayload(brand.get()));

                event.sendSilently(spoofedPacket);
                event.cancel();
            }
        }

        // we want to accept the pack silently to prevent the server detecting you bypassed it when logging in
        if (silentAcceptResourcePack && event.packet instanceof ServerboundResourcePackPacket) event.cancel();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!isActive() || !resourcePack.get()) return;
        if (!(event.packet instanceof ClientboundResourcePackPushPacket packet)) return;

        event.cancel();
        event.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
        event.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.DOWNLOADED));
        event.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));

        msg = Component.literal("This server has ");
        msg.append(packet.required() ? "a required " : "an optional ").append("resource pack. ");

        MutableComponent link = Component.literal("[Open URL]");
        link.setStyle(link.getStyle()
            .withColor(ChatFormatting.BLUE)
            .withUnderlined(true)
            .withClickEvent(new ClickEvent.OpenUrl(URI.create(packet.url())))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open the pack url")))
        );

        MutableComponent acceptance = Component.literal("[Accept Pack]");
        acceptance.setStyle(acceptance.getStyle()
            .withColor(ChatFormatting.DARK_GREEN)
            .withUnderlined(true)
            .withClickEvent(new RunnableClickEvent(() -> {
                URL url = getParsedResourcePackUrl(packet.url());
                if (url == null) error("Invalid resource pack URL: " + packet.url());
                else {
                    silentAcceptResourcePack = true;
                    mc.getDownloadedPackSource().pushPack(packet.id(), url, packet.hash());
                }
            }))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to accept and apply the pack.")))
        );

        msg.append(link).append(" ");
        msg.append(acceptance).append(".");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isActive() || !Utils.canUpdate() || msg == null) return;

        info(msg);
        msg = null;
    }

    private static URL getParsedResourcePackUrl(String url) {
        try {
            URL uRL = new URI(url).toURL();
            String string = uRL.getProtocol();
            return !"http".equals(string) && !"https".equals(string) ? null : uRL;
        } catch (MalformedURLException | URISyntaxException _) {
            return null;
        }
    }
}
