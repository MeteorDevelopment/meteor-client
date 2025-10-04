/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.groups.GroupedList;
import meteordevelopment.meteorclient.settings.groups.ListGroup;
import meteordevelopment.meteorclient.settings.groups.ListGroupTracker;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class GroupedListSetting<T> extends Setting<GroupedList<T, GroupedListSetting<T>.Group>> {

    public GroupedListSetting(String name, String description, GroupedList<T, Group> defaultValue, Consumer<GroupedList<T, Group>> onChanged, Consumer<Setting<GroupedList<T, Group>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    abstract public T parseItem(String str);

    abstract public NbtElement itemToNbt(T t);
    abstract public T itemFromNbt(NbtElement e);

    abstract public Map<String, Group> groups();
    abstract protected ListGroupTracker tracker();

    public Predicate<T> filter = null;

    protected Group getGroup(String name) {
        return groups().get(name.toUpperCase());
    }

    @Override
    public GroupedList<T, Group> get() {
        return value;
    }

    @Override
    public boolean set(GroupedList<T, Group> o) {
        if (value == null) {
            value = new GroupedList<>();
            value.tracker = tracker();
        }
        value.set(o);
        onChanged();
        return true;
    }

    @Override
    protected void resetImpl() {
        set(defaultValue);
    }

    @Override
    protected GroupedList<T, Group> parseImpl(String str) {

        GroupedList<T, Group> list = new GroupedList<>();
        list.tracker = tracker();

        String[] values = str.split(",");

        try {
            for (String value : values) {
                value = value.trim();
                Group group = null;

                if (value.startsWith("@")) {
                    group = getGroup(value.substring(1));
                }

                if (group != null) {
                    list.add(group);
                    continue;
                }

                T item = parseItem(value);

                if (item != null) list.add(item);
            }
        } catch (Exception ignored) {}

        return list;
    }

    @Override
    protected boolean isValueValid(GroupedList<T, Group> value) {
        return true;
    }

    @Override
    final protected NbtCompound save(NbtCompound tag) {
        NbtList groupsTag = new NbtList();

        for (Group g : groups().values()) if (!g.builtin) groupsTag.add(g.toTag());
        tag.put("groups", groupsTag);

        NbtList direct = new NbtList();
        value.getDirectlyIncludedItems().stream().map(this::itemToNbt).forEach(direct::add);

        tag.put("direct", direct);

        NbtList include = new NbtList();
        value.getIncludedGroups().stream().map((g) -> NbtString.of(g.internalName)).forEach(direct::add);

        tag.put("include", include);

        return tag;
    }

    @Override
    final protected GroupedList<T, Group> load(NbtCompound tag) {
        return null;
    }

    final public class GroupBuilder {

        private Group g;

        public GroupBuilder(String name) {
            g = new Group(name);
        }

        public GroupBuilder(String name, Item icon) {
            g = new Group(name);
            g.icon.set(icon);
            g.showIcon.set(true);
        }

        private GroupBuilder builtin() {
            g.builtin = true;
            return this;
        }

        @SafeVarargs
        public final GroupBuilder items(T... of) {
            g.addAll(Arrays.asList(of));
            return this;
        }

        @SafeVarargs
        public final GroupBuilder include(Group... of) {
            g.addAllGroups(Arrays.asList(of));
            return this;
        }

        public GroupBuilder items(Collection<T> of) {
            g.addAll(of);
            return this;
        }

        public GroupBuilder include(Collection<Group> of) {
            g.addAllGroups(of);
            return this;
        }

        public Group get() {
            groups().put(g.internalName.toUpperCase(), g);
            Group w = g;
            g = null;
            return w;
        }
    }

    protected GroupBuilder builtin(String name) {
        return new GroupBuilder(name).builtin();
    }

    protected GroupBuilder builtin(String name, Item icon) {
        return new GroupBuilder(name, icon).builtin();
    }

    public GroupBuilder newGroup(String name) {
        return new GroupBuilder(name);
    }

    public GroupBuilder newGroup(String name, Item icon) {
        return new GroupBuilder(name, icon);
    }


    public class Group extends ListGroup<T, Group> implements ISerializable<Group> {
        public boolean builtin;

        public Settings settings = new Settings();
        public SettingGroup sg = settings.getDefaultGroup();

        private String internalName;

        public Setting<String> name = sg.add(new StringSetting.Builder()
            .name("name")
            .description("the name of the group")
            .onChanged(this::changeName)
            .filter(Utils::nameFilter)
            .build()
        );

        public Setting<Boolean> showIcon = sg.add(new BoolSetting.Builder()
            .name("show-icon")
            .description("don't show an icon")
            .defaultValue(false)
            .build()
        );

        public Setting<Item> icon = sg.add(new ItemSetting.Builder()
            .name("icon")
            .description("item to use as icon in list")
            .defaultValue(Items.NETHER_STAR)
            .visible(showIcon::get)
            .build()
        );

        public boolean checkName(String v) {
            Group other = groups().get(v.toUpperCase());
            return other == this || other == null;
        }

        private void changeName(String v) {
            if (v.equals(internalName)) return;

            Group other = groups().get(v.toUpperCase());
            if (other == this) {
                internalName = v;
                return;
            }
            if (other != null) {
                name.set(internalName);
            }
            else {
                groups().remove(internalName.toUpperCase());
                groups().put(v.toUpperCase(), this);
            }
        }


        private Group(String name)
        {
            super(tracker());

            MeteorClient.LOG.info("GroupedListSetting<T>.Group@ \"{}\" in {}", internalName, tracker().hashCode());

            internalName = name;
            this.name.set(name);
        }

        @Override
        @Unmodifiable
        public List<T> get() {
            MeteorClient.LOG.info("GroupedListSetting<T>.get@ \"{}\" ({} before filter)", internalName, direct.size());
            if (filter == null) return direct;
            return direct.stream().filter(filter).toList();
        }

        @Override
        public NbtCompound toTag() {
            NbtCompound tag = new NbtCompound();
            tag.putString("name", internalName);
            tag.put("icon", Identifier.CODEC, Registries.ITEM.getId(icon.get()));

            NbtList d = new NbtList();
            direct.forEach((t) -> d.add(itemToNbt(t)));

            NbtList i = new NbtList();
            include.forEach((g) -> i.add(NbtString.of(g.internalName)));

            tag.put("direct", d);
            tag.put("include", i);
            return tag;
        }

        @Override
        public Group fromTag(NbtCompound tag) {
            String name = tag.getString("name", null);
            if (name == null) return null;

            Group g = groups().get(name.toUpperCase());
            if (g == null) g = this;

            g.internalName = name;
            g.name.set(name);
            g.icon.set(tag.get("item", Identifier.CODEC).map(Registries.ITEM::get).orElse(null));

            g.direct = tag.getListOrEmpty("direct").stream().map(GroupedListSetting.this::itemFromNbt).toList();
            g.include = tag.getListOrEmpty("include").stream().map(NbtElement::asString)
                .filter(Optional::isPresent).map(Optional::get).map(String::toUpperCase).map((s) -> {
                    if (groups().containsKey(s)) return groups().get(s);
                    return groups().put(s, newGroup(s).get());
                }).toList();

            return g;
        }
    }
}
