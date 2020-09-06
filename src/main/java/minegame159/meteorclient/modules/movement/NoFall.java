package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends ToggleModule {
    public NoFall() {
        super(Category.Movement, "no-fall", "Protects you from fall damage.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> elytra = sgGeneral.add(new BoolSetting.Builder()
            .name("elytra compatibility")
            .description("Stops this from working when using elytra.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
            .name("height")
            .description("How high you have to be off the ground for this to toggle.")
            .defaultValue(0.5)
            .min(0.1)
            .sliderMax(1)
            .build()
    );

    @EventHandler
    private final Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            if (elytra.get() && (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && mc.options.keyJump.isPressed() || mc.player.isFallFlying())) {
                for (int i = 0; i <= Math.ceil(height.get()); i++) {
                    if (!mc.world.getBlockState(mc.player.getBlockPos().add(0, -i, 0)).getMaterial().isReplaceable()) {
                        if (mc.player.getBlockPos().add(0, -i, 0).getY() + 1 + height.get() >= mc.player.getPos().getY()) {
                            ((IPlayerMoveC2SPacket) event.packet).setOnGround(true);
                            return;
                        }
                    }
                }
            } else {
                ((IPlayerMoveC2SPacket) event.packet).setOnGround(true);
            }
        }
    });
}
