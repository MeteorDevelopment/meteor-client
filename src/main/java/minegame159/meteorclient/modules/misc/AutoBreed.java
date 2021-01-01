package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class AutoBreed extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to breed.")
            .defaultValue(new ArrayList<>(0))
            .onlyAttackable()
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("How far away the animals can be to be bred.")
            .min(0)
            .defaultValue(4.5)
            .build()
    );

    private final Setting<Hand> hand = sgGeneral.add(new EnumSetting.Builder<Hand>()
            .name("hand")
            .description("The hand to use for breeding.")
            .defaultValue(Hand.MAIN_HAND)
            .build()
    );

    private final Setting<Boolean> ignoreBabies = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-babies")
            .description("Whether or not to ignore the baby variants of the specified entity.")
            .defaultValue(true)
            .build()
    );

    private final List<Entity> animalsFed = new ArrayList<>();

    public AutoBreed() {
        super(Category.Misc, "auto-breed", "Automatically breeds specified animals.");
    }

    @Override
    public void onActivate() {
        animalsFed.clear();
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        for (Entity entity : mc.world.getEntities()) {
            AnimalEntity animal;

            if (!(entity instanceof AnimalEntity)) continue;
            else animal = (AnimalEntity) entity;

            if (!entities.get().contains(animal.getType())
                    || (animal.isBaby() && !ignoreBabies.get())
                    || animalsFed.contains(animal)
                    || mc.player.distanceTo(animal) > range.get()
                    || !animal.isBreedingItem(hand.get() == Hand.MAIN_HAND ? mc.player.getMainHandStack() : mc.player.getOffHandStack())) continue;

            Vec3d animalPos = new Vec3d(animal.getX(), animal.getY() + animal.getHeight() / 2, animal.getZ());
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(Utils.getNeededYaw(animalPos), Utils.getNeededPitch(animalPos), mc.player.isOnGround()));
            mc.interactionManager.interactEntity(mc.player, animal, hand.get());
            mc.player.swingHand(hand.get());
            animalsFed.add(animal);
            return;
        }
    });
}
