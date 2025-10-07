/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.settings.groups.GroupedList;
import meteordevelopment.meteorclient.settings.groups.ListGroup;
import meteordevelopment.meteorclient.settings.groups.ListGroupTracker;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class GroupedListSetting<T> extends Setting<GroupedList<T, GroupedListSetting.Groups<T>.Group>> {

    protected Predicate<T> filter;

    public GroupedListSetting(String name, String description, GroupedList<T, Groups<T>.Group> defaultValue, Predicate<T> filter, Consumer<GroupedList<T, Groups<T>.Group>> onChanged, Consumer<Setting<GroupedList<T, Groups<T>.Group>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        this.filter = filter;
    }

    abstract public T parseItem(String str);

    abstract public NbtElement itemToNbt(T t);
    abstract public T itemFromNbt(NbtElement e);

    abstract protected Groups<T> groups();

    @Override
    public GroupedList<T, Groups<T>.Group> get() {
        return value;
    }

    public Predicate<T> getFilter() {
        return filter;
    }

    @Override
    public boolean set(GroupedList<T, Groups<T>.Group> o) {
        if (value == null) {
            value = new GroupedList<>();
            value.tracker = groups();
            value.setFilter(filter);
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
    protected GroupedList<T, Groups<T>.Group> parseImpl(String str) {

        GroupedList<T, Groups<T>.Group> list = new GroupedList<>();
        list.tracker = groups();

        String[] values = str.split(",");

        try {
            for (String value : values) {
                value = value.trim();
                Groups<T>.Group group = null;

                if (value.startsWith("@")) {
                    group = groups().get(value.substring(1));
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
    protected boolean isValueValid(GroupedList<T, Groups<T>.Group> value) {
        return true;
    }

    @Override
    final protected NbtCompound save(NbtCompound tag) {
        NbtList groupsTag = new NbtList();

        // this does duplicate the contents of all groups into the config of every setting, however this is
        // necessary for copy/paste to be able to share the complete config
        for (Groups<T>.Group g : groups().GROUPS.values()) if (!g.builtin) groupsTag.add(g.toTag(this::itemToNbt));
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
    final protected GroupedList<T, Groups<T>.Group> load(NbtCompound tag) {
        tag.getListOrEmpty("groups").forEach(el -> el.asCompound().ifPresent(t -> groups().fromTag(t, this::itemFromNbt)));

        value.clear();

        value.addAll(tag.getListOrEmpty("direct").stream().map(this::itemFromNbt).filter(Objects::nonNull).toList());
        value.addAllGroups(tag.getListOrEmpty("include").stream().map(NbtElement::asString).filter(Optional::isPresent).map(o -> groups().get(o.get())).toList());

        return value;
    }

    static final public class Groups<T> extends ListGroupTracker {

        Map<String, Group> GROUPS = new HashMap<>();

        public Groups<T>.Group get(String name) {
            return GROUPS.get(name.toUpperCase());
        }

        public Collection<Group> getAll() {
            return GROUPS.values();
        }

        public class GroupBuilder {

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
                GROUPS.put(g.internalName.toUpperCase(), g);
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

        public GroupBuilder builder(String name) {
            return new GroupBuilder(name);
        }

        public GroupBuilder builder(String name, Item icon) {
            return new GroupBuilder(name, icon);
        }

        public Group fromTag(NbtCompound tag, Function<NbtElement, T> itemFromNbt) {
            return new Group(tag, itemFromNbt);
        }

        public class Group extends ListGroup<T, Group> {
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
                Group other = GROUPS.get(v.toUpperCase());
                return other == this || other == null;
            }

            private void changeName(String v) {
                if (v.equals(internalName)) return;

                Group other = GROUPS.get(v.toUpperCase());
                if (other == this) {
                    internalName = v;
                    return;
                }
                if (other != null) {
                    name.set(internalName);
                }
                else {
                    GROUPS.remove(internalName.toUpperCase());
                    GROUPS.put(v.toUpperCase(), this);
                }
            }


            private Group(String name)
            {
                super(Groups.this);

                internalName = name;
                this.name.set(name);
            }

            private Group(NbtCompound tag, Function<NbtElement, T> itemFromNbt)
            {
                super(Groups.this);
                fromTag(tag, itemFromNbt);
            }

            public NbtCompound toTag(Function<T, NbtElement> itemToNbt) {
                NbtCompound tag = new NbtCompound();
                tag.putString("name", internalName);
                tag.put("icon", Identifier.CODEC, Registries.ITEM.getId(icon.get()));

                NbtList d = new NbtList();
                direct.forEach((t) -> d.add(itemToNbt.apply(t)));

                NbtList i = new NbtList();
                include.forEach((g) -> i.add(NbtString.of(g.internalName)));

                tag.put("direct", d);
                tag.put("include", i);
                return tag;
            }

            public Group fromTag(NbtCompound tag, Function<NbtElement, T> itemFromNbt) {
                String name = tag.getString("name", null);
                if (name == null) return null;

                this.name.set(name);
                this.icon.set(tag.get("item", Identifier.CODEC).map(Registries.ITEM::get).orElse(null));

                this.direct = tag.getListOrEmpty("direct").stream().map(itemFromNbt).toList();
                this.include = tag.getListOrEmpty("include").stream().map(NbtElement::asString)
                    .filter(Optional::isPresent).map(Optional::get).map(String::toUpperCase).map((s) -> {
                        if (GROUPS.containsKey(s)) return GROUPS.get(s);
                        return GROUPS.put(s, builder(s).get());
                    }).toList();

                if (internalName == null) this.name.set(String.format("group%d", hashCode()));

                return this;
            }
        }
    }


}
