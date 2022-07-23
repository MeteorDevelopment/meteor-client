/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.nbs;

import java.util.HashMap;

public class Layer {

    private HashMap<Integer, Note> hashMap = new HashMap<Integer, Note>();
    private byte volume = 100;
    private String name = "";

    public HashMap<Integer, Note> getHashMap() {
        return hashMap;
    }

    public void setHashMap(HashMap<Integer, Note> hashMap) {
        this.hashMap = hashMap;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Note getNote(int tick) {
        return hashMap.get(tick);
    }

    public void setNote(int tick, Note note) {
        hashMap.put(tick, note);
    }

    public byte getVolume() {
        return volume;
    }

    public void setVolume(byte volume) {
        this.volume = volume;
    }
}
