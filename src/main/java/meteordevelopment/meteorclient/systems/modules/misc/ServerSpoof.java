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
import org.apache.commons.lang3.StringUtils;

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
        .description("是否伪造客户端品牌 (Brand).")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> brand = sgGeneral.add(new StringSetting.Builder()
        .name("brand")
        .description("告诉服务器你使用的客户端品牌 (建议 vanilla).")
        .defaultValue("fabric") 
        .visible(spoofBrand::get)
        .build()
    );

    // ==================== 资源包欺骗 ====================
    private final Setting<Boolean> resourcePack = sgGeneral.add(new BoolSetting.Builder()
        .name("resource-pack")
        .description("伪造接受服务器资源包的状态 (防止被检测是否透视/未安装反作弊材质).")
        .defaultValue(true) // 建议默认开启
        .build()
    );

    // ==================== 通道拦截 (核心反检测) ====================
    private final Setting<Boolean> blockChannels = sgGeneral.add(new BoolSetting.Builder()
        .name("block-channels")
        .description("是否拦截特定的网络通信通道 (防止暴露 Mod).")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<String>> channels = sgGeneral.add(new StringListSetting.Builder()
        .name("channels")
        .description("包含这些关键词的通道数据包将被直接拦截.")
        .defaultValue(
            // 必须拦截的 (Meteor 和 Baritone 的特征)
            "meteor",
            "baritone",     
            "forge",
            "freecam",    
            "minecraft:register" 
        )
        .visible(blockChannels::get)
        .build()
    );

    private MutableText msg;
    public boolean silentAcceptResourcePack = false;

    public ServerSpoof() {
        super(Categories.Misc, "server-spoof", "伪造客户端品牌、资源包状态并拦截敏感通道。");
        // 确保在主界面也能运行，以便进服瞬间生效
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        // 开启模块时重置状态，防止逻辑错误
        silentAcceptResourcePack = false;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!isActive()) return;

        // 拦截自定义通道包 (CustomPayload)
        if (event.packet instanceof CustomPayloadC2SPacket packet) {
            // 获取通道 ID (例如 "meteor-client:main")
            Identifier id = packet.payload().getId().id();
            String idString = id.toString().toLowerCase(); // 转小写防止大小写绕过

            // 1. 检查通道拦截
            if (blockChannels.get()) {
                for (String channelKeyword : channels.get()) {
                    if (StringUtils.containsIgnoreCase(idString, channelKeyword)) {
                        // 命中黑名单，拦截发送
                        event.cancel();
                        return; // 直接返回，不再处理后续逻辑
                    }
                }
            }

            // 2. 伪造 Client Brand
            // 当游戏尝试发送 "fabric" 品牌包时，拦截它并发送一个新的 "vanilla" 包
            if (spoofBrand.get() && id.equals(BrandCustomPayload.ID.id())) {
                CustomPayloadC2SPacket spoofedPacket = new CustomPayloadC2SPacket(new BrandCustomPayload(brand.get()));

                // 直接通过网络连接发送伪造的包，绕过事件系统的再次检查
                event.connection.send(spoofedPacket, null, true);
                
                // 拦截原始的 "fabric" 包
                event.cancel();
            }
        }

        // 拦截资源包状态反馈 (用于静默接受)
        if (silentAcceptResourcePack && event.packet instanceof ResourcePackStatusC2SPacket) {
            event.cancel();
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!isActive() || !resourcePack.get()) return;
        
        // 监听服务器发送的“请下载资源包”请求
        if (!(event.packet instanceof ResourcePackSendS2CPacket packet)) return;

        // 1. 拦截请求 (不让客户端弹出原本的下载提示)
        event.cancel();

        // 2. 伪造一系列回复，让服务器以为我们下载并加载成功了
        event.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
        event.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.DOWNLOADED));
        event.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));

        // 3. 在聊天栏打印一个提示，方便你自己手动下载 (如果需要的话)
        msg = Text.literal("[ServerSpoof] 已自动欺骗服务器接受资源包。");
        msg.append(packet.required() ? " (强制)" : " (可选)").append(" ");

        MutableText link = Text.literal("[点击获取下载链接]");
        link.setStyle(link.getStyle()
            .withColor(Formatting.BLUE)
            .withUnderline(true)
            .withClickEvent(new ClickEvent.OpenUrl(URI.create(packet.url())))
            .withHoverEvent(new HoverEvent.ShowText(Text.literal("点击打开浏览器下载")))
        );

        MutableText acceptance = Text.literal("[真实安装]");
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
            .withHoverEvent(new HoverEvent.ShowText(Text.literal("点击让客户端真的去下载安装这个包")))
        );

        msg.append(link).append(" - ");
        msg.append(acceptance);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isActive() || !Utils.canUpdate() || msg == null) return;
        // 将提示信息发送到聊天栏
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