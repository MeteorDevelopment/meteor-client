package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.item.map.MapState;
import net.minecraft.network.Packet;
import net.minecraft.client.MinecraftClient;

public class PlayerFinder extends Module {

    public PlayerFinder() {
        super(Categories.World, "player-finder", "Allows you to get player coordinates using a Boat + Nether Portal + Map.");
    }
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Integer> packetsPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("packets-per-tick")
            .description("The amount of packets to be sent per tick.")
            .defaultValue(1)
            .min(1)
            .sliderMax(50)
            .build()
    );
    
//    public void onUpdate() {
//        if (mc.player.inPortal && mc.player.getRidingEntity() instanceof EntityBoat) {
//            if (mc.player.inventory.getCurrentItem().getItem().equals(Items.MAP))
//                mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(mc.player.getPosition(), EnumFacing.UP, EnumHand.MAIN_HAND, 0, -1337.77f, 0));
//            for (int i = 0; i < amountPerTick.getValInt(); i++) {
//                mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, -1337.77D, mc.player.posZ, false));
//                mc.player.connection.sendPacket(new CPacketSteerBoat(false, true));
// 
// 
//            }
//        }
//        for (Entity entity : mc.world.playerEntities) {
//            if (!entity.getName().equalsIgnoreCase(mc.player.getName())) {
//                Command.sendClientSideMessage("Found A Player Kek " + entity.getPosition());
//            }
//        }
//    }
// 
//    @Listener
//    public void onUpdate(PacketEvent.Receive event) {
//        if (event.getPacket() instanceof SPacketMaps) {
//            ((SPacketMaps) event.getPacket()).setMapdataTo(new MapData("haha i get ur coords"));
//        }
//        if (event.getPacket() instanceof SPacketEntityVelocity || event.getPacket() instanceof SPacketEntityTeleport) {
//            event.setCanceled(true);
//        }
//    }
    
//    @EventHandler(priority = EventPriority.HIGHEST + 1)
//    private void onReceivePacket(PacketEvent.Receive event) {
//    	Packet<?> packet = event.packet;
//    	if (packet instanceof MapUpdateS2CPacket) {
//    		((MapUpdateS2CPacket) packet).apply(new MapState(0, 0, (byte) 0, true, true, false, null));;
//    	}
//    }
    
// 
//    @Listener
//    public void onUpdate(PacketEvent.Send event) {
//        if (event.getPacket() instanceof CPacketConfirmTeleport || event.getPacket() instanceof CPacketPlayerTryUseItem) {
//            event.setCanceled(true);
//        }
//    }
//}
	
}
