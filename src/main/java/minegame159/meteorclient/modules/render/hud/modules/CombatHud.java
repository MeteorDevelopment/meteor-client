package minegame159.meteorclient.modules.render.hud.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudEditorScreen;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.text.TextRenderer;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.render.RenderUtils;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CombatHud extends HudModule {
    private final Color GREEN = new Color(15, 255, 15);
    private final Color ORANGE = new Color(255, 150, 15);
    private final Color RED = new Color(255, 15, 15);

    public CombatHud(HUD hud) {
        super(hud, "combat-info", "Displays information about your combat target.");
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(180 * hud.combatInfoScale.get(), 75 * hud.combatInfoScale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        renderer.addPostTask(() -> {
            int x = box.getX();
            int y = box.getY();

            //Background
            Renderer.NORMAL.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
            Renderer.NORMAL.quad(x, y, box.width, box.height, hud.combatInfoBackgroundColor.get());
            Renderer.NORMAL.end();

            //Player Model
            if (mc.player != null) InventoryScreen.drawEntity(
                    x + (int) (25 * hud.combatInfoScale.get()),
                    y + (int) (66 * hud.combatInfoScale.get()),
                    (int) (30 * hud.combatInfoScale.get()),
                    -MathHelper.wrapDegrees(mc.player.prevYaw + (mc.player.yaw - mc.player.prevYaw) * mc.getTickDelta()),
                    -mc.player.pitch, mc.player
            );

            //Moving pos to past player model
            x += 50 * hud.combatInfoScale.get();
            y += 5 * hud.combatInfoScale.get();

            //Name and ping texts
            String nameText = mc.player.getGameProfile().getName();
            Color nameColor = Friends.get().getFriendColor(mc.player);

            int ping;
            try {
                ping =  mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
            } catch (NullPointerException ignored) {
                ping = 0;
            }

            String breakText = " | ";
            String pingText = ping + "ms";
            Color pingColor;

            if (ping < 75) pingColor = GREEN;
            else if (ping > 75 && ping < 200) pingColor = ORANGE;
            else pingColor = RED;

            TextRenderer.get().begin(0.5 * hud.combatInfoScale.get(), false, true);
            double nameWidth = TextRenderer.get().getWidth(nameText);
            double breakWidth = TextRenderer.get().getWidth(breakText);
            TextRenderer.get().render(nameText, x, y, nameColor != null ? nameColor : hud.primaryColor.get());
            if (hud.combatInfoDisplayPing.get()) {
                TextRenderer.get().render(breakText, x + nameWidth, y, hud.secondaryColor.get());
                TextRenderer.get().render(pingText, x + nameWidth + breakWidth, y, pingColor);
            }
            TextRenderer.get().end();

            //Moving pos down for armor
            y += 10 * hud.combatInfoScale.get();

            double armorX;
            double armorY;
            int slot = 5;

            //Drawing armor
            RenderSystem.pushMatrix();
            RenderSystem.scaled(hud.combatInfoScale.get(), hud.combatInfoScale.get(), 1);

            x /= hud.combatInfoScale.get();
            y /= hud.combatInfoScale.get();

            TextRenderer.get().begin(0.35, false, true);

            for (int position = 0; position < 6; position++) {
                armorX = x + position * 20;
                armorY = y;

                ItemStack itemStack = getItem(slot);

                RenderUtils.drawItem(itemStack, (int) armorX, (int) armorY, true);

                armorY += 18;

                Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);
                Map<Enchantment, Integer> enchantmentsToShow = new HashMap<>();

                for (Enchantment enchantment : hud.combatInfoDisplayedEnchantments.get()) {
                    if (enchantments.containsKey(enchantment)) enchantmentsToShow.put(enchantment, enchantments.get(enchantment));
                }

                for (Enchantment enchantment : enchantmentsToShow.keySet()) {
                    String enchantName = Utils.getEnchantSimpleName(enchantment) + " " + enchantmentsToShow.get(enchantment);

                    double enchX = (armorX + 8) - (TextRenderer.get().getWidth(enchantName) / 2);

                    TextRenderer.get().render(enchantName, enchX, armorY, enchantment.isCursed() ? RED :  hud.combatInfoEnchantmentTextColor.get());
                    armorY += TextRenderer.get().getHeight();
                }
                slot--;
            }

            TextRenderer.get().end();
            RenderSystem.popMatrix();
        });
    }

    private ItemStack getItem(int i) {
        if (mc.player == null || mc.currentScreen instanceof HudEditorScreen) {
            switch (i) {
                case 0:  return Items.END_CRYSTAL.getDefaultStack();
                case 1:  return Items.NETHERITE_BOOTS.getDefaultStack();
                case 2:  return Items.NETHERITE_LEGGINGS.getDefaultStack();
                case 3:  return Items.NETHERITE_CHESTPLATE.getDefaultStack();
                case 4:  return Items.NETHERITE_HELMET.getDefaultStack();
                case 5:  return Items.TOTEM_OF_UNDYING.getDefaultStack();
            }
        }
        if (i == 5) return mc.player.getMainHandStack();
        else if (i == 4) return mc.player.getOffHandStack();
        else return mc.player.inventory.getArmorStack(i);
    }

    public static List<Enchantment> setDefualtList() {
        List<Enchantment> ench = new ArrayList<>();
        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            ench.add(enchantment);
        }
        return ench;
    }
}
