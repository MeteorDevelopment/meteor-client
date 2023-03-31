package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.hit.EntityHitResult;

public class VehicleOneHit extends Module {
    boolean ignorePIEPacket = false;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
        .name("amount")
        .description("The amount of packets to send")
        .defaultValue(15)
        .range(1, 100)
        .sliderRange(1, 20)
        .build()
    );
    public VehicleOneHit() {
        super(Categories.Misc, "VehicleOneHit", "Destroy boats and minecarts with one hit");
    }


    @EventHandler
    public void onPacketSend(PacketEvent.Send event){
        if(event.packet instanceof PlayerInteractEntityC2SPacket && !ignorePIEPacket){
            ignorePIEPacket = true;
            assert mc.crosshairTarget != null && !event.isCancelled();
            Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if(entity instanceof BoatEntity || entity instanceof MinecartEntity) for (int i = 0; i < amount.get(); i++) mc.player.networkHandler.sendPacket(event.packet);
            ignorePIEPacket = false;
        }
    }
}
