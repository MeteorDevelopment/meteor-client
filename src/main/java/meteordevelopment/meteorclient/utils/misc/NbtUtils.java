/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.utils.render.prompts.OkPrompt;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class NbtUtils {
    public static <T extends ISerializable<?>> NbtList listToTag(Iterable<T> list) {
        NbtList tag = new NbtList();
        for (T item : list) tag.add(item.toTag());
        return tag;
    }

    public static <T> List<T> listFromTag(NbtList tag, ToValue<T> toItem) {
        List<T> list = new ArrayList<>(tag.size());
        for (NbtElement itemTag : tag) {
            T value = toItem.toValue(itemTag);
            if (value != null) list.add(value);
        }
        return list;
    }

    public static <K, V extends ISerializable<?>> NbtCompound mapToTag(Map<K, V> map) {
        NbtCompound tag = new NbtCompound();
        for (K key : map.keySet()) tag.put(key.toString(), map.get(key).toTag());
        return tag;
    }

    public static <K, V> Map<K, V> mapFromTag(NbtCompound tag, ToKey<K> toKey, ToValue<V> toValue) {
        Map<K, V> map = new HashMap<>(tag.getSize());
        for (String key : tag.getKeys()) map.put(toKey.toKey(key), toValue.toValue(tag.get(key)));
        return map;
    }

    public static boolean toClipboard(System<?> system) {
        return toClipboard(system.getName(), system.toTag());
    }

    public static boolean toClipboard(String name, NbtCompound nbtCompound) {
        String preClipboard = mc.keyboard.getClipboard();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            NbtIo.writeCompressed(nbtCompound, byteArrayOutputStream);
            mc.keyboard.setClipboard(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
            return true;
        } catch (Exception e) {
            MeteorClient.LOG.error(String.format("Error copying %s NBT to clipboard!", name));

            OkPrompt.create()
                .title(String.format("Error copying %s NBT to clipboard!", name))
                .message("This shouldn't happen, please report it.")
                .id("nbt-copying")
                .show();

            mc.keyboard.setClipboard(preClipboard);
            return false;
        }
    }

    public static boolean fromClipboard(System<?> system) {
        NbtCompound clipboard = fromClipboard(system.toTag());

        if (clipboard != null) {
            system.fromTag(clipboard);
            return true;
        }

        return false;
    }

    public static NbtCompound fromClipboard(NbtCompound schema) {
        try {
            byte[] data = Base64.getDecoder().decode(mc.keyboard.getClipboard());
            ByteArrayInputStream bis = new ByteArrayInputStream(data);

            NbtCompound pasted = NbtIo.readCompressed(new DataInputStream(bis));
            for (String key : schema.getKeys()) if (!pasted.getKeys().contains(key)) return null;
            if (!pasted.getString("name").equals(schema.getString("name"))) return null;

            return pasted;
        } catch (Exception e) {
            MeteorClient.LOG.error("Invalid NBT data pasted!");

            OkPrompt.create()
                .title("Error pasting NBT data!")
                .message("Please check that the data you pasted is valid.")
                .id("nbt-pasting")
                .show();

            return null;
        }
    }

    public interface ToKey<T> {
        T toKey(String string);
    }

    public interface ToValue<T> {
        T toValue(NbtElement tag);
    }
}
