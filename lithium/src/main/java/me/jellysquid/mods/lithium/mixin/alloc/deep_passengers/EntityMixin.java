package me.jellysquid.mods.lithium.mixin.alloc.deep_passengers;


import com.google.common.collect.ImmutableList;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Stream;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    private ImmutableList<Entity> passengerList;

    /**
     * @author 2No2Name
     * @reason Avoid stream code
     */
    @Overwrite
    public Iterable<Entity> getPassengersDeep() {
        if (this.passengerList.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<Entity> passengers = new ArrayList<>();
        this.addPassengersDeep(passengers);

        return passengers;
    }

    /**
     * @author 2No2Name
     * @reason Avoid stream allocations
     */
    @Overwrite
    private Stream<Entity> streamIntoPassengers() {
        if (this.passengerList.isEmpty()) {
            return Stream.empty();
        }
        ArrayList<Entity> passengers = new ArrayList<>();
        this.addPassengersDeep(passengers);

        return passengers.stream();
    }

    /**
     * @author 2No2Name
     * @reason Avoid stream allocations
     */
    @Overwrite
    public Stream<Entity> streamSelfAndPassengers() {
        if (this.passengerList.isEmpty()) {
            return Stream.of((Entity) (Object) this);
        }
        ArrayList<Entity> passengers = new ArrayList<>();
        passengers.add((Entity) (Object) this);
        this.addPassengersDeep(passengers);

        return passengers.stream();
    }

    /**
     * @author 2No2Name
     * @reason Avoid stream allocations
     */
    @Overwrite
    public Stream<Entity> streamPassengersAndSelf() {
        if (this.passengerList.isEmpty()) {
            return Stream.of((Entity) (Object) this);
        }
        ArrayList<Entity> passengers = new ArrayList<>();
        this.addPassengersDeepFirst(passengers);
        passengers.add((Entity) (Object) this);
        return passengers.stream();
    }

    private void addPassengersDeep(ArrayList<Entity> passengers) {
        ImmutableList<Entity> list = this.passengerList;
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            Entity passenger = list.get(i);
            passengers.add(passenger);
            //noinspection ConstantConditions
            ((EntityMixin) (Object) passenger).addPassengersDeep(passengers);
        }
    }

    private void addPassengersDeepFirst(ArrayList<Entity> passengers) {
        ImmutableList<Entity> list = this.passengerList;
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            Entity passenger = list.get(i);
            //noinspection ConstantConditions
            ((EntityMixin) (Object) passenger).addPassengersDeep(passengers);
            passengers.add(passenger);
        }
    }
}
