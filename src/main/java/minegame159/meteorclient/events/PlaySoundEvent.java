/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events;

import net.minecraft.client.sound.SoundInstance;

public class PlaySoundEvent extends Cancellable {
    public SoundInstance sound;
}
