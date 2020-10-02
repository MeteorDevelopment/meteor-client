package minegame159.meteorclient.modules.player;

import com.google.common.collect.Streams;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class EndermanLook extends ToggleModule {

	public EndermanLook() {
		super(Category.Player, "ender-man-look", "Prevents endermen from getting angry at you");
	}
	
	@EventHandler
  private final Listener<TickEvent> onTick = new Listener<>(event -> {
    if (mc.player.abilities.creativeMode || !shouldLook())
      return;

    PlayerMoveC2SPacket.LookOnly packet = new PlayerMoveC2SPacket.LookOnly(
    		mc.player.yaw,
    		90.0f,
    		mc.player.isOnGround());
    
    mc.player.networkHandler.sendPacket(packet);
  });
	
	private boolean shouldLook() {
    return Streams.stream(mc.world.getEntities())
			.filter(entity -> entity instanceof EndermanEntity)
			.filter(Entity::isAlive)
			.filter(this::angleCheck)
			.count() > 0;
	}
	
	private boolean angleCheck(Entity entity) {
		Vec3d vec3d = mc.player.getRotationVec(1.0F).normalize();
		Vec3d vec3d2 = new Vec3d(
				entity.getX() - mc.player.getX(),
				entity.getEyeY() - mc.player.getEyeY(),
				entity.getZ() - mc.player.getZ());
		
		double d = vec3d2.length();
		vec3d2 = vec3d2.normalize();
		double e = vec3d.dotProduct(vec3d2);
		
    return e > 1.0D - 0.025D / d ? mc.player.canSee(entity) : false;
  }
}
