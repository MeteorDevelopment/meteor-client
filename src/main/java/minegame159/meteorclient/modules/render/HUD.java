package minegame159.meteorclient.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.*;
import minegame159.meteorclient.mixininterface.IClientPlayerInteractionManager;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.combat.*;
import minegame159.meteorclient.modules.misc.Timer;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.stream.Collectors;

public class HUD extends ToggleModule {
    public enum DurabilityType{
        None,
        Default,
        Numbers,
        Percentage
    }
    private static final Color white = new Color(255, 255, 255);
    private static final Color gray = new Color(185, 185, 185);
    private static final Color green = new Color(45, 225, 45);
    private static final Color purple = new Color(120, 43, 153);
    private static final Color red = new Color(225, 45, 45);
    private static final Color amber = new Color(235, 158, 52);
    private static final Color yellow = new Color(255, 255, 5);

    private final SettingGroup sgArmor = settings.createGroup("Armor");
    private final SettingGroup sgTopCenter = settings.createGroup("Top Center");
    private final SettingGroup sgTopLeft = settings.createGroup("Top Left");
    private final SettingGroup sgMinimap = settings.createGroup("Minimap");
    private final SettingGroup sgTopRight = settings.createGroup("Top Right");
    private final SettingGroup sgBottomRight = settings.createGroup("Bottom Right");
    private final SettingGroup sgPlayerModel = settings.createGroup("Player Model");

    // Armor
    private final Setting<Boolean> armorEnabled = sgArmor.add(new BoolSetting.Builder()
            .name("armor-enabled")
            .description("Armor HUD.")
            .defaultValue(true)
            .build()
    );

    private final Setting<DurabilityType> armorDurability = sgArmor.add(new EnumSetting.Builder<DurabilityType>()
            .name("armor-durability")
            .description("Displays armor durability on top of hotbar")
            .defaultValue(DurabilityType.Default)
            .build()
    );

    private final Setting<Double> scale = sgArmor.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of the numbers over the armor")
            .min(0.5)
            .max(1)
            .sliderMax(1)
            .defaultValue(0.8)
            .build()
    );

    private final Setting<Boolean> armorWarning = sgArmor.add(new BoolSetting.Builder()
            .name("durability-warner")
            .description("Warns you if your armor is about to break.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> warningDurability = sgArmor.add(new IntSetting.Builder()
            .name("warn-durability")
            .description("The durability you are warned at")
            .defaultValue(30)
            .min(1)
            .max(360)
            .sliderMax(100)
            .build()
    );

    private final Setting<Boolean> notificationSettings = sgArmor.add(new BoolSetting.Builder()
            .name("chat-notifications")
            .description("Sends messages in chat rather than in the corner of the screen.")
            .defaultValue(false)
            .build()
    );

    // Top Center
    private final Setting<Boolean> serverLagNotifier = sgTopCenter.add(new BoolSetting.Builder()
            .name("lag-notifier")
            .description("Time since last tick.").defaultValue(true).build()
    );

    private final Setting<Boolean> userWelcome = sgTopCenter.add( new BoolSetting.Builder()
            .name("user-welcome")
            .description("Welcome message.").defaultValue(true).build());

    // Top Left
    private final Setting<Boolean> waterMark = sgTopLeft.add(new BoolSetting.Builder()
            .name("water-mark")
            .description("Water mark.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> fps = sgTopLeft.add(new BoolSetting.Builder()
            .name("fps")
            .description("Display fps.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ping = sgTopLeft.add(new BoolSetting.Builder()
            .name("ping")
            .description("Display ping.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> tps = sgTopLeft.add(new BoolSetting.Builder()
            .name("tps")
            .description("Display tps.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> speed = sgTopLeft.add(new BoolSetting.Builder()
            .name("speed")
            .description("Display speed in blocks per second.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> biome = sgTopLeft.add(new BoolSetting.Builder()
            .name("biome")
            .description("Displays biome you are in.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> time = sgTopLeft.add(new BoolSetting.Builder()
            .name("time")
            .description("Displays ingame time in ticks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> durability = sgTopLeft.add(new BoolSetting.Builder()
            .name("durability")
            .description("Durability of the time in your main hand.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> breakingBlock = sgTopLeft.add(new BoolSetting.Builder()
            .name("breaking-block")
            .description("Displays the percentage how much you have broken the block.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> lookingAt = sgTopLeft.add(new BoolSetting.Builder()
            .name("looking-at")
            .description("Displays block or entity you are looking at.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> entities = sgTopLeft.add(new BoolSetting.Builder()
            .name("entities")
            .description("Display number of entities.")
            .defaultValue(false)
            .onChanged(aBoolean -> updateEntities = true)
            .build()
    );

    private final Setting<Boolean> entityCustomNames = sgTopLeft.add(new BoolSetting.Builder()
            .name("entity-custom-names")
            .description("Use custom names.")
            .defaultValue(true)
            .onChanged(aBoolean -> updateEntities = true)
            .build()
    );

    private final Setting<Boolean> separateSheepsByColor = sgTopLeft.add(new BoolSetting.Builder()
            .name("separate-sheeps-by-color")
            .description("Separates sheeps by color in entity list.")
            .defaultValue(false)
            .onChanged(aBoolean -> updateEntities = true)
            .build()
    );

    private final Setting<Boolean> combatInfo = sgTopLeft.add(new BoolSetting.Builder()
            .name("combat-info")
            .description("Shows which combat modules you have on.")
            .defaultValue(false)
            .build()
    );

    // Minimap
    private final Setting<Boolean> minimapEnabled = sgMinimap.add(new BoolSetting.Builder()
            .name("minimap-enabled")
            .description("Minimap HUD.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> mmScale = sgMinimap.add(new DoubleSetting.Builder()
            .name("minimap-scale")
            .description("Scale.")
            .defaultValue(1)
            .min(0)
            .sliderMax(2)
            .build()
    );

    private final Setting<Color> mmBackground = sgMinimap.add(new ColorSetting.Builder()
            .name("minimap-background")
            .description("Minimap background color.")
            .defaultValue(new Color(25, 25, 25, 175))
            .build()
    );

    private final Setting<Color> mmPlayer = sgMinimap.add(new ColorSetting.Builder()
            .name("minimap-player")
            .description("Minimap player color.")
            .defaultValue(new Color(225, 225, 225, 225))
            .build()
    );

    private final Setting<Color> mmAnimal = sgMinimap.add(new ColorSetting.Builder()
            .name("minimap-animal")
            .description("Minimap animal color.")
            .defaultValue(new Color(25, 225, 25, 225))
            .build()
    );

    private final Setting<Color> mmMob = sgMinimap.add(new ColorSetting.Builder()
            .name("minimap-mob")
            .description("Minimap mob color.")
            .defaultValue(new Color(225, 25, 25, 225))
            .build()
    );

    // Top Right
    private final Setting<Boolean> activeModules = sgTopRight.add(new BoolSetting.Builder()
            .name("active-modules")
            .description("Display active modules.")
            .defaultValue(true)
            .build()
    );

    // Bottom Right
    public final Setting<Boolean> potionTimers = sgBottomRight.add(new BoolSetting.Builder()
            .name("potion-timers")
            .description("Display potion timers and hide minecraft default potion icons.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> position = sgBottomRight.add(new BoolSetting.Builder()
            .name("position")
            .description("Display your position.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotation = sgBottomRight.add(new BoolSetting.Builder()
            .name("rotation")
            .description("Display your rotation.")
            .defaultValue(true)
            .build()
    );

    // Player Model
    private final Setting<Boolean> playerModel = sgPlayerModel.add(new BoolSetting.Builder()
            .name("Player Model")
            .description("Render mini player model.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> background = sgPlayerModel.add(new BoolSetting.Builder()
            .name("render-background")
            .description("Render a background behind the player model.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Color> bgColor = sgPlayerModel.add(new ColorSetting.Builder()
            .name("background-color")
            .description("Background color.")
            .defaultValue(new Color(0, 0, 0, 30))
            .build()
    );

    private final Setting<AlignmentX> xAlignment = sgPlayerModel.add(new EnumSetting.Builder<AlignmentX>()
            .name("x-alignment")
            .description("X alignment.")
            .defaultValue(AlignmentX.Left)
            .build()
    );

    private final Setting<Integer> xOffset = sgPlayerModel.add(new IntSetting.Builder()
            .name("x-offset")
            .description("X offset.")
            .defaultValue(3)
            .sliderMax(200)
            .build()
    );

    private final Setting<AlignmentY> yAlignment = sgPlayerModel.add(new EnumSetting.Builder<AlignmentY>()
            .name("y-alignment")
            .description("Y alignment.")
            .defaultValue(AlignmentY.Bottom)
            .build()
    );

    private final Setting<Integer> yOffset = sgPlayerModel.add(new IntSetting.Builder()
            .name("y-offset")
            .description("Y offset.")
            .defaultValue(3)
            .sliderMax(200)
            .build()
    );

    private final HashMap<String, EntityInfo> entityCounts = new HashMap<>();
    private int maxLetterCount = 0;
    private boolean updateEntities;
    private int updateEntitiesTimer = 2;
    private Map<Integer, ItemStack> itemStackMap = new HashMap<>();

    private final List<ToggleModule> modules = new ArrayList<>();

    private final BlockPos.Mutable playerBlockPos = new BlockPos.Mutable();

    public HUD() {
        super(Category.Render, "HUD", "Displays various info to the screen.");
    }

    @Override
    public void onActivate() {
        updateEntities = true;

        recalculateActiveModules();
    }

    @EventHandler
    private final Listener<ActiveModulesChangedEvent> activeModulesChangedEventListener = new Listener<>(event -> recalculateActiveModules());

    @EventHandler
    private final Listener<ModuleVisibilityChangedEvent> onModuleVisibilityChanged = new Listener<>(event -> {
        if (event.module.isActive()) recalculateActiveModules();
    });

    @EventHandler
    private final Listener<EntityAddedEvent> onEntityAdded = new Listener<>(event -> {
        if (entities.get()) {
            if (!isValidEntity(event.entity)) return;
            updateEntities = true;
        }
    });

    @EventHandler
    private final Listener<EntityRemovedEvent> onEntityRemoved = new Listener<>(event -> {
        if (entities.get()) {
            if (!isValidEntity(event.entity)) return;
            updateEntities = true;
        }
    });

    private String getEntityName(Entity entity) {
        if (entity instanceof PlayerEntity) return "Player";
        if (entity instanceof ItemEntity) return "Item";
        String name = entityCustomNames.get() ? entity.getDisplayName().getString() : entity.getType().getName().getString();
        if (separateSheepsByColor.get() && entity instanceof SheepEntity) return StringUtils.capitalize(((SheepEntity) entity).getColor().getName().replace('_', ' ')) + " - " + name;
        return name;
    }

    private EntityInfo getEntityInfo(Entity entity) {
        return entityCounts.computeIfAbsent(getEntityName(entity), EntityInfo::new);
    }

    private boolean isValidEntity(Entity entity) {
        return entity != mc.player;
    }

    private void calculateMaxLetterCount() {
        maxLetterCount = 0;

        for (EntityInfo a : entityCounts.values()) {
            maxLetterCount = Math.max(maxLetterCount, a.countStr.length());
        }
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        updateEntitiesTimer--;

        if (entities.get()) {
            if (updateEntities && updateEntitiesTimer <= 0 && entities.get()) {
                for (EntityInfo entityInfo : entityCounts.values()) {
                    entityInfo.reset();
                }

                for (Entity entity : mc.world.getEntities()) {
                    if (!isValidEntity(entity)) continue;

                    getEntityInfo(entity).increment(entity);
                }

                updateEntities = false;
                updateEntitiesTimer = 2;
                calculateMaxLetterCount();
            }
        }
        if(armorWarning.get()){
            for (int i = mc.player.inventory.armor.size() - 1; i >= 0; i--){
                ItemStack itemStack = mc.player.inventory.armor.get(i);
                if(itemStack.isEmpty())continue;
                if((itemStack.getMaxDamage() - itemStack.getDamage()) <= warningDurability.get()){
                    if(!itemStackMap.containsKey(i)){
                        itemStackMap.put(i, itemStack);
                        sendNotification();
                    }else if(itemStackMap.containsKey(i) && (itemStackMap.get(i).getMaxDamage() - itemStackMap.get(i).getDamage()) < (itemStack.getMaxDamage() - itemStack.getDamage())){
                        itemStackMap.put(i, itemStack);
                        sendNotification();
                    }
                }else if ((itemStack.getMaxDamage() - itemStack.getDamage()) > warningDurability.get() && itemStackMap.containsKey(i)){
                    itemStackMap.remove(i);
                }
            }
        }
    });

    @EventHandler
    private final Listener<Render2DEvent> onRender2D = new Listener<>(event -> {
        MeteorClient.FONT.begin();
        renderPlayerModel(event);
        renderTopCenter(event);
        renderTopLeft(event);
        renderTopRight(event);
        renderBottomRight(event);
        MeteorClient.FONT.end();

        if (armorEnabled.get()) {
            int x = event.screenWidth / 2 + 12;
            int y = event.screenHeight - 38;

            if (!mc.player.abilities.creativeMode) y -= 18;

            if(armorDurability.get() == DurabilityType.Default) {
                for (int i = mc.player.inventory.armor.size() - 1; i >= 0; i--) {
                    ItemStack itemStack = mc.player.inventory.armor.get(i);

                    mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
                    mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y);

                    x += 20;
                }
            }else if(armorDurability.get() == DurabilityType.Numbers){
                for (int i = mc.player.inventory.armor.size() - 1; i >= 0; i--) {
                    ItemStack itemStack = mc.player.inventory.armor.get(i);

                    mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
                    if(!itemStack.isEmpty() && itemStack.isDamageable()) {
                        String message = Integer.toString(itemStack.getMaxDamage() - itemStack.getDamage());
                        MeteorClient.FONT.scale = scale.get();
                        MeteorClient.FONT.renderStringWithShadow(message, x + ((15 - (MeteorClient.FONT.getStringWidth(message))) / 2) + 1, y - 5, white);
                        MeteorClient.FONT.scale = 1;
                    }

                    x += 20;
                }
            }else if(armorDurability.get() == DurabilityType.Percentage){
                for (int i = mc.player.inventory.armor.size() - 1; i >= 0; i--) {
                    ItemStack itemStack = mc.player.inventory.armor.get(i);

                    mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
                    if(!itemStack.isEmpty() && itemStack.isDamageable()) {
                        String message = Integer.toString(Math.round(((itemStack.getMaxDamage() - itemStack.getDamage()) * 100f) / (float) itemStack.getMaxDamage()));
                        MeteorClient.FONT.scale = scale.get();
                        MeteorClient.FONT.renderStringWithShadow(message, x + ((15 - (MeteorClient.FONT.getStringWidth(message))) / 2) + 1, y - 5, white);
                        MeteorClient.FONT.scale = 1;
                    }

                    x += 20;
                }
            }else if(armorDurability.get() == DurabilityType.None){
                for (int i = mc.player.inventory.armor.size() - 1; i >= 0; i--) {
                    ItemStack itemStack = mc.player.inventory.armor.get(i);

                    mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);

                    x += 20;
                }
            }

            DiffuseLighting.disable();
        }

    });

    private void renderMMQuad(double x, double y, double width, double height, Color color) {
        double s = mmScale.get();
        ShapeBuilder.quad(2 + x * s, 2 + y * s, width * s, height * s, color);
    }

    private void renderMMTriangle(double x, double y, double size, double angle, Color color) {
        double s = mmScale.get();
        ShapeBuilder.triangle(2 + x * s, 2 + y * s, size * s, angle, color);
    }

    private void renderTopCenter(Render2DEvent event) { if
    (mc.options.debugEnabled) return;
        int y = 4;
        if (userWelcome.get()) {
            drawInfoCenter("Welcome to Meteor Client, ", mc.player.getGameProfile().getName() + "!", y, white, purple);
            y += MeteorClient.FONT.getHeight() + 4;
        }

        float timeSinceLastTick = TickRate.INSTANCE.getTimeSinceLastTick();



        Color warningColor = red;
        if (timeSinceLastTick > 10) warningColor = red;
        else if (timeSinceLastTick > 3) warningColor = amber;
        else warningColor = yellow;
        if (serverLagNotifier.get()) {
            if (timeSinceLastTick >= 1f) {
                drawInfoCenter("Since last tick: ", String.format("%.1f", timeSinceLastTick), y, white, warningColor);
                y += MeteorClient.FONT.getHeight() + 4;
            }
        }
    }

    public void renderPlayerModel(Render2DEvent event) {
        if (playerModel.get()) {
            int posX = getX(mc.getWindow().getScaledWidth());
            int posY = getY(mc.getWindow().getScaledHeight());
            if (background.get()) {
                ShapeBuilder.begin(null, GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
                ShapeBuilder.quad(posX, posY, WIDTH, HEIGHT, bgColor.get());
                ShapeBuilder.end();
            }
            InventoryScreen.drawEntity(posX + 25, posY + 66, 30, 0, 0, mc.player);

/* This is the other method of rendering the player which includes yeaw headYaw and pitch so ill leave it here for now
            EntityRenderDispatcher entityRenderDispatcher = mc.getEntityRenderDispatcher();
            MatrixStack matrixStack = new MatrixStack();
            RenderSystem.pushMatrix();
            boolean entityShadows = entityRenderDispatcher.gameOptions.entityShadows;
            float glY = (float) 10 + (20.0f + (float) 50 / 2.0f);
            float glX = (float) 10 + 20.0f;
            int scaleY = MathHelper.ceil((float) 50 / (mc.player.getHeight()));
            int scaleX = MathHelper.ceil((float) 25 / (mc.player.getWidth()));
            float scale = Math.min(scaleX, scaleY) * -1.0f;
            RenderSystem.translatef(glX, glY - ((mc.player.isSwimming() || mc.player.isFallFlying()) ? (float) 50 / 2.0f : 0.0f), 50.0f);
            matrixStack.scale(scale, scale, scale);
            entityRenderDispatcher.setRenderShadows(false);
            VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
            entityRenderDispatcher.render(mc.player, -posX, -posY, 0.0, 0.0f, 1.0f, matrixStack, immediate, 0xF000F0);
            immediate.draw();
            entityRenderDispatcher.setRenderShadows(entityShadows);
            RenderSystem.popMatrix();
*/
        }
    }

    private static final int WIDTH = 51;
    private static final int HEIGHT = 75;

    private int getX(int screenWidth) {
        switch (xAlignment.get()) {
            case Left:   return xOffset.get();
            case Center: return screenWidth / 2 - WIDTH / 2 + xOffset.get();
            case Right:  return screenWidth - WIDTH - xOffset.get();
            default:     return 0;
        }
    }

    private int getY(int screenHeight) {
        switch (yAlignment.get()) {
            case Top:    return yOffset.get();
            case Center: return screenHeight / 2 - HEIGHT / 2 + yOffset.get();
            case Bottom: return screenHeight - HEIGHT - yOffset.get();
            default:     return 0;
        }
    }


    private void renderTopLeft(Render2DEvent event) {
        if (mc.options.debugEnabled) return;
        int y = 2;

        if (minimapEnabled.get()) {
            ShapeBuilder.begin(null, GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
            renderMMQuad(0, 0, 100, 100, mmBackground.get());
            renderMMQuad(0, 0, 100, 1, mmBackground.get());
            renderMMQuad(0, 0, 1, 100, mmBackground.get());
            renderMMQuad(100 - 1, 0, 1, 100, mmBackground.get());
            renderMMQuad(0, 100 - 1, 100, 1, mmBackground.get());

            double radius = 32;
            if (mc.options.viewDistance > 4) radius += 16;

            double centerX = mc.player.getX();
            double centerZ = mc.player.getZ();

            for (Entity entity : mc.world.getEntities()) {
                double x = entity.getX() - centerX;
                double z = entity.getZ() - centerZ;
                if (Math.abs(x) > radius || Math.abs(z) > radius) continue;

                Color color;
                if (EntityUtils.isPlayer(entity)) color = mmPlayer.get();
                else if (EntityUtils.isAnimal(entity)) color = mmAnimal.get();
                else if (EntityUtils.isMob(entity)) color = mmMob.get();
                else continue;

                double x2 = (x / radius) * 50 + 50;
                double z2 = (z / radius) * 50 + 50;

                if (entity.getEntityId() == mc.player.getEntityId()) renderMMTriangle(x2 - 2.5, z2 - 2.5, 5, mc.player.yaw, color);
                else renderMMQuad(x2 - 1, z2 - 1, 2, 2, color);
            }

            double w = MeteorClient.FONT.getStringWidth("N");
            MeteorClient.FONT.renderStringWithShadow("N", 2 + 50 - w / 2, 4, white);
            w = MeteorClient.FONT.getStringWidth("S");
            MeteorClient.FONT.renderStringWithShadow("S", 2 + 50 - w / 2, 2 + 100 - MeteorClient.FONT.getHeight() - 2, white);
            MeteorClient.FONT.renderStringWithShadow("W", 4, 2 + 50 - MeteorClient.FONT.getHeight() / 2, white);
            w = MeteorClient.FONT.getStringWidth("E");
            MeteorClient.FONT.renderStringWithShadow("E", 2 + 100 - w - 2, 2 + 50 - MeteorClient.FONT.getHeight() / 2, white);

            ShapeBuilder.end();
            y += 100 * mmScale.get() + 2;
        }

        if (waterMark.get()) {
            drawInfo("Meteor Client ", Config.INSTANCE.version.getOriginalString(), y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (fps.get()) {
            drawInfo("FPS: ", ((IMinecraftClient) MinecraftClient.getInstance()).getFps() + "", y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (ping.get() && playerListEntry != null) {
            drawInfo("Ping: ", playerListEntry.getLatency() + "", y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (tps.get()) {
            drawInfo("TPS: ", String.format("%.1f", TickRate.INSTANCE.getTickRate()), y);
            y += MeteorClient.FONT.getHeight() + 2;
        }
      
        if (speed.get()) {
            double tX = Math.abs(mc.player.getX() - mc.player.prevX);
            double tZ = Math.abs(mc.player.getZ() - mc.player.prevZ);
            double length = Math.sqrt(tX * tX + tZ * tZ);
            if (ModuleManager.INSTANCE.get(Timer.class).isActive()){
                length *= ModuleManager.INSTANCE.get(Timer.class).getMultiplier();
            }

            drawInfo("Speed: ", String.format("%.1f", length * 20), y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (biome.get()) {
            playerBlockPos.set(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            drawInfo("Biome: ", Arrays.stream(mc.world.getBiome(playerBlockPos).getCategory().getName().split("_")).map(StringUtils::capitalize).collect(Collectors.joining(" ")), y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (time.get()) {
            int ticks = (int) (mc.world.getTimeOfDay() % 24000);
            ticks += 6000;
            if (ticks > 24000) ticks -= 24000;
            drawInfo("Time: ", String.format("%02d:%02d", ticks / 1000, (int) (ticks % 1000 / 1000.0 * 60)), y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (durability.get()) {
            Integer amount = null;
            if (!mc.player.getMainHandStack().isEmpty() && mc.player.getMainHandStack().isDamageable()) amount = mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage();

            drawInfo("Durability: ", amount == null ? "" : amount.toString(), y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (breakingBlock.get()) {
            drawInfo("Breaking block: ", String.format("%.0f%%", ((IClientPlayerInteractionManager) mc.interactionManager).getBreakingProgress() * 100), y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (lookingAt.get()) {
            String text = "";
            if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) text = mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos()).getBlock().getName().getString();
            else if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY) text = ((EntityHitResult) mc.crosshairTarget).getEntity().getDisplayName().getString();

            drawInfo("Looking At: ", text, y);
            y += MeteorClient.FONT.getHeight() + 2;
        }

        if (entities.get()) {
            for (EntityInfo renderInfo : entityCounts.values()) {
                if (!renderInfo.render) continue;

                drawEntityCount(renderInfo, y);
                y += MeteorClient.FONT.getHeight() + 2;
            }
        }

        if (combatInfo.get()) {
            y += MeteorClient.FONT.getHeight() + 2;
            drawCombatInfo(KillAura.class, "KA", y);
            y += MeteorClient.FONT.getHeight() + 2;
            drawCombatInfo(CrystalAura.class, "CA", y);
            y += MeteorClient.FONT.getHeight() + 2;
            drawCombatInfo(BedAura.class, "BA", y);
            y += MeteorClient.FONT.getHeight() + 2;
            drawCombatInfo(Surround.class, "SR", y);
            y += MeteorClient.FONT.getHeight() + 2;
            drawCombatInfo(AutoTrap.class, "AT", y);
            y += MeteorClient.FONT.getHeight() + 2;
        }
    }

    private void drawCombatInfo(Class<? extends ToggleModule> module, String name, double y) {
        boolean on = ModuleManager.INSTANCE.isActive(module);
        double x = MeteorClient.FONT.renderStringWithShadow(name, 2, y, white);
        MeteorClient.FONT.renderStringWithShadow(on ? " ON" : " OFF", x, y, on ? green : red);
    }

    private void drawInfo(String text1, String text2, double x, double y, Color text1Color) {
        MeteorClient.FONT.renderStringWithShadow(text1, x, y, text1Color);
        MeteorClient.FONT.renderStringWithShadow(text2, x + MeteorClient.FONT.getStringWidth(text1), y, gray);
    }
    private void drawInfo(String text1, String text2, double y, Color text1Color) {
        drawInfo(text1, text2, 2, y, text1Color);
    }
    private void drawInfo(String text1, String text2, double y) {
        drawInfo(text1, text2, y, white);
    }

    private void drawInfoCenter(String text1, String text2, double x, double y, Color text1Color, Color text2Color) {
        MeteorClient.FONT.renderStringWithShadow(text1, x, y, text1Color);
        MeteorClient.FONT.renderStringWithShadow(text2, x + MeteorClient.FONT.getStringWidth(text1), y, text2Color);
    }

    private void drawInfoCenter(String text1, String text2, double y, Color text1Color, Color text2Color) {
        drawInfoCenter(text1, text2, mc.getWindow().getScaledWidth() / 2 - (MeteorClient.FONT.getStringWidth(text1)+MeteorClient.FONT.getStringWidth(text2))  / 2, y, text1Color, text2Color);
    }
    private void drawInfoRight(String text1, String text2, double y, Color text1Color) {
        drawInfo(text1, text2, mc.getWindow().getScaledWidth() - MeteorClient.FONT.getStringWidth(text1) - MeteorClient.FONT.getStringWidth(text2) - 2, y, text1Color);
    }
    private void drawInfoRight(String text1, String text2, double y) {
        drawInfoRight(text1, text2, y, white);
    }

    private void drawEntityCount(EntityInfo entityInfo, double y) {
        MeteorClient.FONT.renderStringWithShadow(entityInfo.countStr, 2, y, gray);
        MeteorClient.FONT.renderStringWithShadow(entityInfo.name, 2 + (maxLetterCount - entityInfo.countStr.length()) * 4 + 4 + MeteorClient.FONT.getStringWidth(entityInfo.countStr), y, white);
    }

    private void renderTopRight(Render2DEvent event) {
        if (mc.options.debugEnabled) return;
        double y = 2;

        if (activeModules.get()) {
            for (ToggleModule module : modules) {
                String infoString = module.getInfoString();
                double x;
                if (infoString == null) {
                    x = event.screenWidth - MeteorClient.FONT.getStringWidth(module.title) - 2;
                    MeteorClient.FONT.renderStringWithShadow(module.title, x, y, module.color);
                } else {
                    x = event.screenWidth - MeteorClient.FONT.getStringWidth(module.title + " " + infoString) - 2;
                    MeteorClient.FONT.renderStringWithShadow(module.title, x, y, module.color);
                    MeteorClient.FONT.renderStringWithShadow(module.getInfoString(), x + MeteorClient.FONT.getStringWidth(module.title + " "), y, gray);
                }
                y += MeteorClient.FONT.getHeight() + 1;
            }
        }
    }

    private void recalculateActiveModules() {
        modules.clear();

        for (ToggleModule oldModule : ModuleManager.INSTANCE.getActive()) {
            if (oldModule.isVisible()) modules.add(oldModule);
        }

        modules.sort((o1, o2) -> {
            int a = Double.compare(o1.getInfoString() == null ? MeteorClient.FONT.getStringWidth(o1.title) : MeteorClient.FONT.getStringWidth(o1.title + " " + o1.getInfoString()), o2.getInfoString() == null ? MeteorClient.FONT.getStringWidth(o2.title) : MeteorClient.FONT.getStringWidth(o2.title + " " + o2.getInfoString()));
            if (a == 0) return 0;
            return a < 0 ? 1 : -1;
        });
    }

    private void renderBottomRight(Render2DEvent event) {
        double y = event.screenHeight - MeteorClient.FONT.getHeight() - 2;

        if (rotation.get()) {
            Direction direction = Direction.fromRotation(mc.gameRenderer.getCamera().getYaw());
            String axis = "invalid";
            switch (direction) {
                case NORTH: axis = "-Z"; break;
                case SOUTH: axis = "+Z"; break;
                case WEST:  axis = "-X"; break;
                case EAST:  axis = "+X"; break;
            }

            float yaw = mc.gameRenderer.getCamera().getYaw() % 360;
            if (yaw < 0) yaw += 360;
            if (yaw > 180) yaw -= 360;

            float pitch = mc.gameRenderer.getCamera().getPitch() % 360;
            if (pitch < 0) pitch += 360;
            if (pitch > 180) pitch -= 360;

            drawInfoRight(String.format("%s %s ", StringUtils.capitalize(direction.getName()), axis), String.format("(%.1f, %.1f)", yaw, pitch), y);
            y -= MeteorClient.FONT.getHeight() + 2;
        }

        if (position.get()) {
            double x1 = mc.gameRenderer.getCamera().getPos().x;
            double y1 = mc.gameRenderer.getCamera().getPos().y - mc.player.getEyeHeight(mc.player.getPose());
            double z1 = mc.gameRenderer.getCamera().getPos().z;

            switch (Utils.getDimension()) {
                case Overworld:
                    drawPosition(event.screenWidth, "Nether Pos: ", y, x1 / 8.0, y1, z1 / 8.0);
                    y -= MeteorClient.FONT.getHeight() + 2;
                    drawPosition(event.screenWidth, "Pos: ", y, x1, y1, z1);
                    y -= MeteorClient.FONT.getHeight() + 2;
                    break;

                case Nether:
                    drawPosition(event.screenWidth, "Overworld Pos: ", y, x1 * 8.0, y1, z1 * 8.0);
                    y -= MeteorClient.FONT.getHeight() + 2;
                    drawPosition(event.screenWidth, "Pos: ", y, x1, y1, z1);
                    y -= MeteorClient.FONT.getHeight() + 2;
                    break;

                case End:
                    drawPosition(event.screenWidth, "Pos: ", y, x1, y1, z1);
                    y -= MeteorClient.FONT.getHeight() + 2;
                    break;
            }
        }

        if (potionTimers.get()) {
            for (StatusEffectInstance statusEffectInstance : mc.player.getStatusEffects()) {
                StatusEffect statusEffect = statusEffectInstance.getEffectType();

                int c = statusEffect.getColor();
                white.r = Color.toRGBAR(c);
                white.g = Color.toRGBAG(c);
                white.b = Color.toRGBAB(c);

                drawInfoRight(statusEffect.getName().getString(), " " + (statusEffectInstance.getAmplifier() + 1) + " (" + StatusEffectUtil.durationToString(statusEffectInstance, 1) + ")", y, white);
                y -= MeteorClient.FONT.getHeight() + 2;

                white.r = white.g = white.b = 255;
            }
        }
    }

    private void drawPosition(int screenWidth, String text, double yy, double x, double y, double z) {
        String msg1 = String.format("%.1f %.1f %.1f", x, y, z);
        double x1 = screenWidth - MeteorClient.FONT.getStringWidth(msg1) - 2;
        double x2 = screenWidth - MeteorClient.FONT.getStringWidth(msg1) - MeteorClient.FONT.getStringWidth(text) - 2;
        MeteorClient.FONT.renderStringWithShadow(msg1, x1, yy, gray);
        MeteorClient.FONT.renderStringWithShadow(text, x2, yy, white);
    }

    private void sendNotification(){
        if (!notificationSettings.get()) {
            mc.getToastManager().add(new Toast() {
                private long timer;
                private long lastTime = -1;

                @Override
                public Visibility draw(MatrixStack matrices, ToastManager manager, long currentTime) {
                    if (lastTime == -1) lastTime = currentTime;
                    else timer += currentTime - lastTime;

                    manager.getGame().getTextureManager().bindTexture(new Identifier("textures/gui/toasts.png"));
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 255.0F);
                    manager.drawTexture(matrices, 0, 0, 0, 32, 160, 32);

                    manager.getGame().textRenderer.draw(matrices, "Armor Low.", 12.0F, 12.0F, -11534256);

                    return timer >= 32000 ? Visibility.HIDE : Visibility.SHOW;
                }
            });
        } else {
            Chat.warning(this, "One of your armor pieces is low.");
        }
    }

    private static class EntityInfo {
        public String name;
        public int count;
        public String countStr;
        public boolean render;

        public EntityInfo(String name) {
            this.name = name;
        }

        public void increment(Entity entity) {
            if (entity instanceof ItemEntity) count += ((ItemEntity) entity).getStack().getCount();
            else count++;
            countStr = Integer.toString(count);
            render = true;
        }

        public void reset() {
            count = 0;
            render = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntityInfo that = (EntityInfo) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
