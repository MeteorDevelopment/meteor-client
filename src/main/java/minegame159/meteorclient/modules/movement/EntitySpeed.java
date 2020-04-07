package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PlayerMoveEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;

public class EntitySpeed extends ToggleModule {
    private Setting<Double> speed = addSetting(new DoubleSetting.Builder()
            .name("speed")
            .description("Speed.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private Setting<Boolean> dontGoIntoUnloadedChunks = addSetting(new BoolSetting.Builder()
            .name("dont-go-into-unloaded-chunks")
            .description("Dont go into unloaded chunks.")
            .defaultValue(true)
            .build()
    );

    public EntitySpeed() {
        super(Category.Movement, "entity-speed", "Entities go brrrm.");
    }

    @EventHandler
    private Listener<PlayerMoveEvent> onPlayerMove = new Listener<>(event -> {
        if (mc.player.getVehicle() == null) return;

        Entity vehicle = mc.player.getVehicle();
        if (vehicle instanceof PigEntity || vehicle instanceof HorseEntity) steerEntity(vehicle);
        else if (vehicle instanceof BoatEntity) steerBoat((BoatEntity) vehicle);
    });

    private void steerEntity(Entity entity) {
        //entity.motionY = -0.4D;

        moveForward(entity, speed.get() * 3.8);

        if (entity instanceof HorseEntity) {
            entity.yaw = mc.player.yaw;
        }
    }

    private void steerBoat(BoatEntity boat) {
        int angle;

        boolean forward = mc.options.keyForward.isPressed();
        boolean left = mc.options.keyLeft.isPressed();
        boolean right = mc.options.keyRight.isPressed();
        boolean back = mc.options.keyBack.isPressed();

        double velX;
        double velY = boat.getVelocity().y;
        double velZ;

        if (!(forward && back)) velY = 0;
        if (mc.options.keyJump.isPressed()) velY += speed.get() / 2;

        if (!forward && !left && !right && !back) return;
        if (left && right) angle = forward ? 0 : back ? 180 : -1;
        else if (forward && back) angle = left ? -90 : (right ? 90 : -1);
        else {
            angle = left ? -90 : (right ? 90 : 0);
            if (forward) angle /= 2;
            else if (back) angle = 180 - (angle / 2);
        }

        if (angle == -1) return;
        float yaw = mc.player.yaw + angle;

        velX = Math.sin(-yaw * 0.017453292) * speed.get();
        velZ = Math.cos(yaw * 0.017453292) * speed.get();

        if (isGoingIntoUnloadedChunk(boat, velX, velZ) && dontGoIntoUnloadedChunks.get()) {
            velX = 0;
            velZ = 0;
        }

        setEntitySpeed(boat, velX, velY, velZ);
    }

    private void moveForward(Entity entity, double speed) {
        double forward = mc.player.input.movementForward;
        double sideways = mc.player.input.movementSideways;
        boolean movingForward = forward != 0;
        boolean movingSideways = sideways != 0;
        float yaw = mc.player.yaw;

        if (!movingForward && !movingSideways) {
            setEntitySpeed(entity, 0, entity.getVelocity().y, 0);
        } else {
            if (forward != 0.0) {
                if (sideways > 0.0) {
                    yaw += (forward > 0.0 ? -45 : 45);
                } else if (sideways < 0.0) {
                    yaw += (forward > 0.0 ? 45 : -45);
                }
                sideways = 0.0D;
                if (forward > 0.0) {
                    forward = 1.0;
                } else {
                    forward = -1.0D;
                }
            }

            double cos = Math.cos(Math.toRadians(yaw + 90.0f));
            double sin = Math.sin(Math.toRadians(yaw + 90.0f));
            double velX = (forward * speed * cos + sideways * speed * sin);
            double velZ = (forward * speed * sin - sideways * speed * cos);

            //System.out.println(isGoingIntoUnloadedChunk(entity, velX, velZ));
            if (isGoingIntoUnloadedChunk(entity, velX, velZ) && dontGoIntoUnloadedChunks.get()) {
                velX = 0;
                velZ = 0;
            }

            setEntitySpeed(entity, velX, entity.getVelocity().y, velZ);
        }
    }

    private boolean isGoingIntoUnloadedChunk(Entity entity, double velX, double velZ) {
        int chunkX = (int) ((entity.x + velX) / 16);
        int chunkZ = (int) ((entity.z + velZ) / 16);

        System.out.println(!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ));
        return !mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ);
    }

    private void setEntitySpeed(Entity entity, double velX, double velY, double velZ) {
        Vec3d velocity = entity.getVelocity();
        ((IVec3d) velocity).set(velX, velY, velZ);
    }
}
