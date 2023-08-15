/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.interaction;


import net.minecraft.entity.Entity;

public class EntityAction implements InteractionManager.Action {
    private final int priority;
    private State state;
    private final Entity entity;
    private final InteractionManager.EntityInteractType type;

    public EntityAction(Entity entity, InteractionManager.EntityInteractType type, int priority) {
        this.entity = entity;
        this.priority = priority;
        this.type = type;

        this.state = State.Pending;
    }

    public Entity getEntity() {
        return entity;
    }

    public InteractionManager.EntityInteractType getType() {
        return type;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }
}
