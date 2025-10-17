/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTab;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TabHud extends HudElement {
    public static final HudElementInfo<TabHud> INFO = new HudElementInfo<>(Hud.GROUP, "players", "Displays customizable list of players.", TabHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");
    private final SettingGroup sgTesting = settings.createGroup("Testing", false);

    private enum LATENCY_DISPLAY {
        LATENCY_DISPLAY_NONE,
        LATENCY_DISPLAY_BARS,
        LATENCY_DISPLAY_NUMBER,
    }

    private final Setting<Boolean> renderHeader = sgGeneral.add(new BoolSetting.Builder()
        .name("render-header")
        .description("Render tab header?")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> headerSpacing = sgGeneral.add(new IntSetting.Builder()
        .name("header-spacing")
        .description("Space between header and players table, in pixels.")
        .defaultValue(mc.textRenderer.fontHeight)
        .min(0)
        .sliderRange(0, 18)
        .build()
    );

    private final Setting<Boolean> renderPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("render-players")
        .description("Render player list/table?")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> columnLimit = sgGeneral.add(new IntSetting.Builder()
        .name("column-limit")
        .description("The max number of players in each column. (0 means single column)")
        .defaultValue(20)
        .min(1)
        .sliderRange(0, 40)
        .build()
    );

    private final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
        .name("limit")
        .description("The max number of players to show.")
        .defaultValue(80)
        .min(1)
        .sliderRange(1, 180)
        .build()
    );

    private final Setting<Boolean> applyBetterTabNames = sgGeneral.add(new BoolSetting.Builder()
        .name("apply-better-tab-names")
        .description("Ask BetterTab for player names?")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> drawPlayerHeads = sgGeneral.add(new BoolSetting.Builder()
        .name("draw-player-heads")
        .description("Render player heads?")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderFooter = sgGeneral.add(new BoolSetting.Builder()
        .name("render-footer")
        .description("Render tab footer?")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> footerSpacing = sgGeneral.add(new IntSetting.Builder()
        .name("footer-spacing")
        .description("Space between players table and footer, in pixels.")
        .defaultValue(mc.textRenderer.fontHeight)
        .min(0)
        .sliderRange(0, 18)
        .build()
    );

    private final Setting<Integer> wrapWidth = sgGeneral.add(new IntSetting.Builder()
        .name("wrap-width")
        .description("When to wrap header and footer.")
        .defaultValue(250)
        .sliderRange(20, 800)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders shadow behind text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
        .name("border")
        .description("How much space to add around the element.")
        .defaultValue(0)
        .build()
    );

    // Scale

    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies custom text scale rather than the global one.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    // Background

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    // Editor

    private final Setting<Boolean> editorUseFakeNames = sgTesting.add(new BoolSetting.Builder()
        .name("use-fake-names")
        .description("Generate random names to test hud in editor?")
        .defaultValue(true)
        .onChanged(this::testingGenList)
        .build()
    );

    private final Setting<Integer> editorNumberOfFakeNames = sgTesting.add(new IntSetting.Builder()
        .name("number-of-fake-names")
        .description("How many fakse names to generate in editor.")
        .defaultValue(mc.textRenderer.fontHeight)
        .min(1)
        .sliderRange(0, 120)
        .visible(editorUseFakeNames::get)
        .onChanged(integer -> testingGenList(true))
        .build()
    );

    private final Setting<Boolean> editorUseFakeHeaderFooter = sgTesting.add(new BoolSetting.Builder()
        .name("use-fake-header-footer")
        .description("Use specified fake headers and footers in editor?")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> editorFakeHeader = sgTesting.add(new StringSetting.Builder()
        .name("fake-header")
        .description("Fake header text.")
        .defaultValue("test header")
        .visible(editorUseFakeHeaderFooter::get)
        .build()
    );

    private final Setting<String> editorFakeFooter = sgTesting.add(new StringSetting.Builder()
        .name("fake-footer")
        .description("Fake footer text.")
        .defaultValue("test footer")
        .visible(editorUseFakeHeaderFooter::get)
        .build()
    );

    public TabHud() {
        super(INFO);
        testingGenList(true);
    }

    @Override
    public void setSize(double width, double height) {
        super.setSize(width + border.get() * 2, height + border.get() * 2);
    }

    @Override
    protected double alignX(double width, Alignment alignment) {
        return box.alignX(getWidth() - border.get() * 2, width, alignment);
    }

    private List<PlayerListEntry> fakePlayers = new ArrayList<>();

    @Override
    public void render(HudRenderer rend) {
        float scale = getScale();
        double width = 0;
        double height = 0;
        var header = getHeader();
        if (renderHeader.get()) {
            int maxHeaderWidth = 0;
            for (var hl : header) {
                maxHeaderWidth = Math.max(maxHeaderWidth, mc.textRenderer.getWidth(hl));
            }
            width = Math.max(width, maxHeaderWidth);
            height += mc.textRenderer.fontHeight * header.size();
            height += headerSpacing.get();
        }
        double playersColumnWidth = 0;
        int playersColumnCount = 0;
        double playersRowHeight = 0;
        int playersRowCount = 0;
        var players = getPlayers();
        int playersCount = players.size();
        var minLatency = players.stream().max(Comparator.comparingInt(PlayerListEntry::getLatency));
        var latencyWidth = mc.textRenderer.getWidth("0ms");
        if (minLatency.isPresent()) {
            latencyWidth = mc.textRenderer.getWidth(minLatency.get().getLatency() + "ms");
        }
        var playerHeadWidth = 0;
        if (drawPlayerHeads.get()) {
            playerHeadWidth = mc.textRenderer.fontHeight;
        }
        if (renderPlayers.get()) {
            playersRowCount = playersCount;
            playersColumnCount = 1;
            for (; playersRowCount > columnLimit.get(); playersRowCount = (playersCount + playersColumnCount - 1) / playersColumnCount) {
                playersColumnCount++;
            }
            double maxPlayerWidth = 1;
            double maxPlayerHeight = mc.textRenderer.fontHeight;
            for (var player : players) {
                var name = getPlayerName(player);
                maxPlayerWidth = Math.max(maxPlayerWidth, mc.textRenderer.getWidth(name)+latencyWidth+playerHeadWidth);
            }
            playersColumnWidth = maxPlayerWidth;
            playersRowHeight = maxPlayerHeight;
            width = Math.max(width, maxPlayerWidth*playersColumnCount);
            height += playersRowHeight*playersRowCount;
        }
        var footer = getFooter();
        if (renderFooter.get()) {
            int maxFooterWidth = 0;
            for (var fl : footer) {
                maxFooterWidth = Math.max(maxFooterWidth, mc.textRenderer.getWidth(fl));
            }
            width = Math.max(width, maxFooterWidth);
            height += mc.textRenderer.fontHeight * footer.size();
            height += footerSpacing.get();
        }
        if (width == 0 && height == 0) {
            setSize(40, 40);
            return;
        }
        setSize(width*scale, height*scale);

        var mats = rend.drawContext.getMatrices();
        if (background.get()) {
            rend.quad(this.x, this.y, width*scale, height*scale, backgroundColor.get());
        }
        mats.push();
        mats.scale(scale, scale, 1);
        mats.translate(0, 0, 401); // Thanks Mojang
        double y = this.y + border.get();
        double x = this.x + border.get();
        x /= scale;
        y /= scale;
        scale = 1;
        if (renderHeader.get()) {
            for (var hl : header) {
                int tx = (int)((x + width/2 - (double) mc.textRenderer.getWidth(hl) /2)/scale);
                int ty = (int)(y/scale);
                rend.drawContext.drawText(mc.textRenderer, hl, tx, ty, 0xFFFFFFFF, shadow.get());
                y += mc.textRenderer.fontHeight*scale;
            }
            y += headerSpacing.get()*scale;
        }
        if (renderPlayers.get()) {
            for (int col = 0; col < playersColumnCount; col++) {
                for (int row = 0; row < playersRowCount; row++) {
                    var i = col*playersRowCount + row;
                    if (i >= playersCount) {
                        continue;
                    }
                    var player = players.get(i);
                    var name = getPlayerName(player);
                    double tx = x + width / 2 - (playersColumnCount * playersColumnWidth) / 2 + col*playersColumnWidth;
                    int ty = (int)((y + row*playersRowHeight)/scale);
                    rend.drawContext.drawText(mc.textRenderer, name, (int)((tx+playerHeadWidth)/scale), ty, 0xFFFFFFFF, shadow.get());
                    mc.inGameHud.getPlayerListHud().renderLatencyIcon(rend.drawContext, latencyWidth, (int)((tx+playersColumnWidth-latencyWidth)/scale), ty, player);
                    if (mc.world != null && drawPlayerHeads.get()) {
                        PlayerEntity playerEntity = mc.world.getPlayerByUuid(player.getProfile().getId());
                        boolean bl2 = playerEntity != null && LivingEntityRenderer.shouldFlipUpsideDown(playerEntity);
                        PlayerSkinDrawer.draw(rend.drawContext, player.getSkinTextures().texture(), (int)(tx/scale), ty, 8, player.shouldShowHat(), bl2, -1);
                    }
                }
            }
            y += playersRowCount*playersRowHeight;
        }
        if (renderFooter.get() && footer != null) {
            y += footerSpacing.get()*scale;
            for (var fl : footer) {
                int tx = (int)((x + width/2 - mc.textRenderer.getWidth(fl)/2)/scale);
                int ty = (int)(y/scale);
                rend.drawContext.drawText(mc.textRenderer, fl, tx, ty, 0xFFFFFFFF, shadow.get());
                y += mc.textRenderer.fontHeight*scale;
            }
        }
        mats.pop();
    }

    private List<PlayerListEntry> genFakePlayers() {
        List<PlayerListEntry> ret = new ArrayList<>();
        for (int i = 0; i < editorNumberOfFakeNames.get(); i++) {
            var pickChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
            var pickLen = 3 + Math.round(Math.random()*13);
            StringBuilder a = new StringBuilder();
            for (int i2 = 0; i2 < pickLen; i2++) {
                a.append(pickChars.charAt((int) (Math.random() * pickChars.length()-1)));
            }
            var e = new PlayerListEntry(new GameProfile(UUID.randomUUID(), a.toString()), false);
            e.setDisplayName(Text.of(a.toString()));
            ret.add(e);
        }
        return ret;
    }

    private void testingGenList(boolean a) {
        fakePlayers = a ? genFakePlayers() : Collections.emptyList();
    }

    private Text getPlayerName(PlayerListEntry e) {
        if (applyBetterTabNames.get()) {
            return Modules.get().get(BetterTab.class).getPlayerName(e);
        }
        return mc.inGameHud.getPlayerListHud().getPlayerName(e);
    }

    private List<PlayerListEntry> getPlayers() {
        if (isInEditor() && editorUseFakeNames.get()) {
            return fakePlayers;
        }
        if (mc.player == null) {
            return Collections.emptyList();
        }
        return mc.player.networkHandler.getListedPlayerListEntries().stream().sorted(PlayerListHud.ENTRY_ORDERING).limit(limit.get()).toList();
    }

    private List<OrderedText> getHeader() {
        var t = mc.inGameHud.getPlayerListHud().header;
        if (isInEditor() && editorUseFakeHeaderFooter.get()) {
            t = Text.of(editorFakeHeader.get());
        }
        if (t == null) {
            return Collections.emptyList();
        }
        return mc.textRenderer.wrapLines(t, wrapWidth.get());
    }

    private List<OrderedText> getFooter() {
        var t = mc.inGameHud.getPlayerListHud().footer;
        if (isInEditor() && editorUseFakeHeaderFooter.get()) {
            t = Text.of(editorFakeFooter.get());
        }
        if (t == null) {
            return Collections.emptyList();
        }
        return mc.textRenderer.wrapLines(t, wrapWidth.get());
    }

    private float getScale() {
        return customScale.get() ? scale.get().floatValue() : 1;
    }
}
