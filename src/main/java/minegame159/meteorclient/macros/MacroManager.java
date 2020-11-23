/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.macros;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.utils.NbtUtils;
import minegame159.meteorclient.utils.Savable;
import net.minecraft.nbt.CompoundTag;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MacroManager extends Savable<MacroManager> implements Iterable<Macro> {
    public static final MacroManager INSTANCE = new MacroManager();

    private List<Macro> macros = new ArrayList<>();

    private MacroManager() {
        super(new File(MeteorClient.FOLDER, "macros.nbt"));
    }

    public void add(Macro macro) {
        macros.add(macro);
        MeteorClient.EVENT_BUS.subscribe(macro);
        MeteorClient.EVENT_BUS.post(EventStore.macroListChangedEvent());
        save();
    }

    public List<Macro> getAll() {
        return macros;
    }

    public void remove(Macro macro) {
        if (macros.remove(macro)) {
            MeteorClient.EVENT_BUS.unsubscribe(macro);
            MeteorClient.EVENT_BUS.post(EventStore.macroListChangedEvent());
            save();
        }
    }

    @Override
    public Iterator<Macro> iterator() {
        return macros.iterator();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("macros", NbtUtils.listToTag(macros));
        return tag;
    }

    @Override
    public MacroManager fromTag(CompoundTag tag) {
        macros = NbtUtils.listFromTag(tag.getList("macros", 10), tag1 -> new Macro().fromTag((CompoundTag) tag1));

        for (Macro macro : macros) MeteorClient.EVENT_BUS.subscribe(macro);
        return this;
    }
}
