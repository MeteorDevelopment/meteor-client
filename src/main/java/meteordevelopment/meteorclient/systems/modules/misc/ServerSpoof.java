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
import org.apache.commons.lang3.Strings;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class ServerSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // ==================== 品牌伪装 ====================
    private final Setting<Boolean> spoofBrand = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-brand")
        .description("是否伪造客户端品牌。")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> brand = sgGeneral.add(new StringSetting.Builder()
        .name("brand")
        .description("告诉服务器你的客户端品牌。建议保持 'fabric' 以配合你的白名单模组列表。")
        .defaultValue("fabric") // 【修改】默认为 fabric，配合 Sodium 白名单最真实
        .visible(spoofBrand::get)
        .build()
    );

    // ==================== 资源包欺骗 ====================
    private final Setting<Boolean> resourcePack = sgGeneral.add(new BoolSetting.Builder()
        .name("resource-pack")
        .description("自动伪造接受服务器资源包的状态 (防止被检测是否安装反作弊材质)。")
        .defaultValue(true)
        .build()
    );

    // ==================== 通道拦截 (核心反检测) ====================
    private final Setting<Boolean> blockChannels = sgGeneral.add(new BoolSetting.Builder()
        .name("block-channels")
        .description("是否拦截特定的网络通信通道 (防止暴露 Mod)。")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<String>> channels = sgGeneral.add(new StringListSetting.Builder()
        .name("channels")
        .description("包含这些关键词的通道数据包将被拦截。")
        .defaultValue(
            // 【修改】优化后的黑名单，去掉了 minecraft:register，增加了其他特征
            "meteor",       // 必须拦截
            "baritone",     // 必须拦截
            "freecam",      // 必须拦截
            "schematica",   // 投影 Mod
            "litematica",   // 投影 Mod (Fabric版)
            "worldedit",    // 创世神 CUI
            "replaymod",    // 回放 Mod
            "fabric:structure" // 某些结构 Mod
        )
        .visible(blockChannels::get)
        .build()
    );

    private MutableText msg;
    public boolean silentAcceptResourcePack = false;

    public ServerSpoof() {
        super(Categories.Misc, "server-spoof", "伪造客户端品牌、资源包状态并拦截敏感通道。");
        // 确保进服握手阶段就能运行
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        silentAcceptResourcePack = false;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!isActive()) return;

        // 处理自定义负载包 (通道与品牌)
        if (event.packet instanceof CustomPayloadC2SPacket packet) {
            Identifier id = packet.payload().getId().id();
            String idString = id.toString().toLowerCase();

            // 1. 检查通道黑名单拦截
            if (blockChannels.get()) {
                for (String channel : channels.get()) {
                    if (Strings.CI.contains(id.toString(), channel)) {
                        event.cancel();
                        return;
                    }
                }
            }

            // 2. 品牌伪装
            // 如果发包是品牌包 (minecraft:brand) 且开启了伪装
            if (spoofBrand.get() && id.equals(BrandCustomPayload.ID.id())) {
                // 创建一个新的品牌包，内容是我们设定的 (例如 "fabric")
                CustomPayloadC2SPacket spoofedPacket = new CustomPayloadC2SPacket(new BrandCustomPayload(brand.get()));

                event.sendSilently(spoofedPacket);
                event.cancel();
            }
        }

        // 处理资源包状态
        if (silentAcceptResourcePack && event.packet instanceof ResourcePackStatusC2SPacket) {
            event.cancel();
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!isActive() || !resourcePack.get()) return;
        
        // 监听服务器发来的资源包请求
        if (!(event.packet instanceof ResourcePackSendS2CPacket packet)) return;

        // 拦截请求，不让 GUI 弹出
        event.cancel();

        // 伪造完整的接收流程
        event.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
        event.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.DOWNLOADED));
        event.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));

        // 聊天栏提示
        msg = Text.literal("[ServerSpoof] 已欺骗服务器接受资源包。");
        msg.append(packet.required() ? " (强制)" : " (可选)").append(" ");

        MutableText link = Text.literal("[获取链接]");
        link.setStyle(link.getStyle()
            .withColor(Formatting.BLUE)
            .withUnderline(true)
            .withClickEvent(new ClickEvent.OpenUrl(URI.create(packet.url())))
            .withHoverEvent(new HoverEvent.ShowText(Text.literal("点击在浏览器下载")))
        );

        MutableText acceptance = Text.literal("[真实加载]");
        acceptance.setStyle(acceptance.getStyle()
            .withColor(Formatting.DARK_GREEN)
            .withUnderline(true)
            .withClickEvent(new RunnableClickEvent(() -> {
                URL url = getParsedResourcePackUrl(packet.url());
                if (url == null) error("无效的资源包链接: " + packet.url());
                else {
                    silentAcceptResourcePack = true;
                    mc.getServerResourcePackProvider().addResourcePack(packet.id(), url, packet.hash());
                }
            }))
            .withHoverEvent(new HoverEvent.ShowText(Text.literal("点击让客户端真的加载这个包")))
        );

        msg.append(link).append(" - ");
        msg.append(acceptance);
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
        } catch (MalformedURLException | URISyntaxException var3) {
            return null;
        }
    }
}