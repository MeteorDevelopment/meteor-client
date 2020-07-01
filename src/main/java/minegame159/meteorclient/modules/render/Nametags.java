package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.mixininterface.IBakedQuad;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeableArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.Map;

public class Nametags extends ToggleModule {
    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);
    private static final Color GREEN = new Color(25, 225, 25);
    private static final Color ORANGE = new Color(225, 105, 25);
    private static final Color RED = new Color(225, 25, 25);
    private static final Color WHITE = new Color(255, 255, 255);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> displayArmor = sgGeneral.add(new BoolSetting.Builder()
            .name("display-armor")
            .description("Display armor.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayArmorEnchants = sgGeneral.add(new BoolSetting.Builder()
            .name("display-armor-enchants")
            .description("Display armor enchantments.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale.")
            .defaultValue(1)
            .min(0.1)
            .build()
    );

    private final Setting<Double> enchantTextScale = sgGeneral.add(new DoubleSetting.Builder()
            .name("enchant-text-scale")
            .description("Enchantment text scale.")
            .defaultValue(0.6)
            .min(0.1)
            .max(1)
            .sliderMin(0.1)
            .sliderMax(1)
            .build()
    );

    public Nametags() {
        super(Category.Render, "nametags", "Displays nametags above players.");
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity) || entity == mc.player) continue;

            renderNametag(event, (PlayerEntity) entity);
        }
    });

    private void renderNametag(RenderEvent event, PlayerEntity entity) {
        Camera camera = mc.gameRenderer.getCamera();

        // Compute scale
        double dist = Utils.distanceToCamera(entity);
        double scale = 0.04 * this.scale.get();

        // Compute health things
        float absorption = entity.getAbsorptionAmount();
        int health = Math.round(entity.getHealth() + absorption);
        double healthPercentage = health / (entity.getMaxHealth() + absorption);

        String name = entity.getGameProfile().getName();
        String healthText = " " + health;

        // Setup the rotation
        Matrices.push();
        double x = entity.prevX + (entity.getX() - entity.prevX) * event.tickDelta;
        double y = entity.prevY + (entity.getY() - entity.prevY) * event.tickDelta + entity.getHeight() + 0.5;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * event.tickDelta;
        Matrices.translate(x - event.offsetX, y - event.offsetY, z - event.offsetZ);
        Matrices.rotate(-camera.getYaw(), 0, 1, 0);
        Matrices.rotate(camera.getPitch(), 1, 0, 0);
        Matrices.scale(-scale, -scale, scale);

        // Get armor info
        double[] armorWidths = new double[4];
        boolean hasArmor = false;
        int maxEnchantCount = 0;
        if (displayArmor.get() || displayArmorEnchants.get()) {
            MeteorClient.FONT.scale = enchantTextScale.get();
            for (int i = 0; i < 4; i++) {
                ItemStack itemStack = entity.inventory.armor.get(i);
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);

                if (armorWidths[i] == 0) armorWidths[i] = 16;
                if (!itemStack.isEmpty() && displayArmor.get()) hasArmor = true;

                if (displayArmorEnchants.get()) {
                    for (Enchantment enchantment : enchantments.keySet()) {
                        String enchantName = Utils.getEnchantShortName(enchantment) + " " + enchantments.get(enchantment);
                        armorWidths[i] = Math.max(armorWidths[i], MeteorClient.FONT.getStringWidth(enchantName));
                    }

                    maxEnchantCount = Math.max(maxEnchantCount, enchantments.size());
                }
            }
            MeteorClient.FONT.scale = 1;
        }

        // Setup size
        double nameWidth = MeteorClient.FONT.getStringWidth(name);
        double healthWidth = MeteorClient.FONT.getStringWidth(healthText);
        double width = nameWidth + healthWidth;
        double armorWidth = 0;
        for (double v : armorWidths) armorWidth += v;
        width = Math.max(width, armorWidth);
        double widthHalf = width / 2;

        double heightDown = MeteorClient.FONT.getHeight();
        double armorHeight = (hasArmor ? 16 : 0);
        armorHeight = Math.max(armorHeight, maxEnchantCount * MeteorClient.FONT.getHeight() * enchantTextScale.get());
        double heightUp = armorHeight;

        // Render background
        ShapeBuilder.begin(null, GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        ShapeBuilder.quad(-widthHalf - 1, -1, 0, -widthHalf - 1, heightDown, 0, widthHalf + 1, heightDown, 0, widthHalf + 1, -1, 0, BACKGROUND);
        ShapeBuilder.end();

        // Render armor
        double itemSpacing = (width - armorWidth) / 4;
        if (hasArmor) {
            double itemX = -widthHalf;
            ShapeBuilder.begin(null, GL11.GL_TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

            boolean isDamaged = false;

            for (int i = 0; i < 4; i++) {
                ItemStack itemStack = entity.inventory.armor.get(i);

                if (itemStack.isDamaged()) isDamaged = true;

                for (BakedQuad quad : mc.getItemRenderer().getModels().getModel(itemStack).getQuads(null, null, null)) {
                    Sprite sprite = ((IBakedQuad) quad).getSprite();

                    if (itemStack.getItem() instanceof DyeableArmorItem) {
                        int c = ((DyeableArmorItem) itemStack.getItem()).getColor(itemStack);

                        WHITE.r = Color.toRGBAR(c);
                        WHITE.g = Color.toRGBAG(c);
                        WHITE.b = Color.toRGBAB(c);
                    }

                    double preItemX = itemX;
                    itemX += (armorWidths[i] - 16) / 2;
                    double addY = (armorHeight - 16) / 2;

                    ShapeBuilder.texQuad(itemX, -heightUp + addY, 16, 16, sprite.getMinU(), sprite.getMinV(), sprite.getMaxU() - sprite.getMinU(), sprite.getMaxV() - sprite.getMinV(), WHITE, WHITE, WHITE, WHITE);

                    itemX = preItemX;
                    WHITE.r = WHITE.g = WHITE.b = 255;
                }

                itemX += armorWidths[i] + itemSpacing;
            }
            mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            ShapeBuilder.end(true);

            // Durability
            if (isDamaged) {
                itemX = -widthHalf;
                ShapeBuilder.begin(null, GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);

                for (int i = 0; i < 4; i++) {
                    ItemStack itemStack = entity.inventory.armor.get(i);

                    double damage = itemStack.getDamage();
                    double maxDamage = itemStack.getMaxDamage();
                    double percentage = Math.max(0.0F, (maxDamage - damage) / maxDamage);

                    double j = Math.round(13.0F - damage * 13.0F / maxDamage);
                    int k = MathHelper.hsvToRgb((float) (percentage / 3.0), 1, 1);

                    double preItemX = itemX;
                    itemX += (armorWidths[i] - 17) / 2;
                    double addY = (armorHeight - 16) / 2;

                    WHITE.r = WHITE.g = WHITE.b = 0;
                    ShapeBuilder.quad(itemX + 2, -heightUp + 13 + addY, 0, itemX + 2 + 13, -heightUp + 13 + addY, 0, itemX + 2 + 13, -heightUp + 2 + 13 + addY, 0, itemX + 2, -heightUp + 2 + 13 + addY, 0, WHITE);

                    WHITE.r = k >> 16 & 255;
                    WHITE.g = k >> 8 & 255;
                    WHITE.b = k & 255;
                    ShapeBuilder.quad(itemX + 2, -heightUp + 13 + addY, 0, itemX + 2 + j, -heightUp + 13 + addY, 0, itemX + 2 + j, -heightUp + 1 + 13 + addY, 0, itemX + 2, -heightUp + 1 + 13 + addY, 0, WHITE);

                    WHITE.r = WHITE.g = WHITE.b = 255;
                    itemX = preItemX;

                    itemX += armorWidths[i] + itemSpacing;
                }

                ShapeBuilder.end();
            }
        }

        // Get health color
        Color healthColor;
        if (healthPercentage <= 0.333) healthColor = RED;
        else if (healthPercentage <= 0.666) healthColor = ORANGE;
        else healthColor = GREEN;

        // Render name, health enchant and texts
        MeteorClient.FONT.begin();
        double hX = MeteorClient.FONT.renderStringWithShadow(name, -widthHalf, 0, TEXT);
        MeteorClient.FONT.renderStringWithShadow(healthText, hX + (width - nameWidth - healthWidth), 0, healthColor);
        double itemX = -widthHalf;

        if (maxEnchantCount > 0) {
            MeteorClient.FONT.scale = enchantTextScale.get();

            for (int i = 0; i < 4; i++) {
                ItemStack itemStack = entity.inventory.armor.get(i);
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);

                double aW = armorWidths[i];
                double enchantY = 0;
                double addY = (armorHeight - enchantments.size() * MeteorClient.FONT.getHeight()) / 2;

                for (Enchantment enchantment : enchantments.keySet()) {
                    String enchantName = Utils.getEnchantShortName(enchantment) + " " + enchantments.get(enchantment);
                    MeteorClient.FONT.renderStringWithShadow(enchantName, itemX + ((aW - MeteorClient.FONT.getStringWidth(enchantName)) / 2), -heightUp + enchantY + addY, TEXT);

                    enchantY += MeteorClient.FONT.getHeight();
                }

                itemX += armorWidths[i] + itemSpacing;
            }

            MeteorClient.FONT.scale = 1;
        }
        MeteorClient.FONT.end();

        Matrices.pop();
    }
}
