/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PotionTimersHud extends HudElement {
    public static final HudElementInfo<PotionTimersHud> INFO = new HudElementInfo<>(Hud.GROUP, "potion-timers", "Displays active potion effects with timers.", PotionTimersHud::new);
    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    // General
    private final Setting<FilterMode> filterMode = sgGeneral.add(new EnumSetting.Builder<FilterMode>()
        .name("filter-mode")
        .description("Whether or not to use a whitelist or a blacklist.")
        .defaultValue(FilterMode.Whitelist)
        .build()
    );

    private final Setting<List<StatusEffect>> potions = sgGeneral.add(new StatusEffectListSetting.Builder()
        .name("effects")
        .description("Which effects to whitelist/blacklist.")
        .build()
    );

    private final Setting<Boolean> roman = sgGeneral.add(new BoolSetting.Builder()
        .name("roman-numerals")
        .description("Display the effect amplifier as Roman numerals rather than numbers.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> vanilla = sgGeneral.add(new BoolSetting.Builder()
        .name("vanilla-colors")
        .description("Use the vanilla potion colors instead of the normal HUD colors.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders shadow behind text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
        .name("sort-mode")
        .description("How to sort the displayed potions.")
        .defaultValue(SortMode.Biggest)
        .build()
    );

    private final Setting<Alignment> alignment = sgGeneral.add(new EnumSetting.Builder<Alignment>()
        .name("alignment")
        .description("Horizontal alignment.")
        .defaultValue(Alignment.Auto)
        .build()
    );


    public PotionTimersHud() {
        super(INFO);
    }

    private final List<CountedEffect> effects = new ArrayList<>();
    private final Pool<CountedEffect> effectPool = new Pool<>(CountedEffect::new);
    private final int[] numbers = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private final String[] letters = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    @Override
    public void tick(HudRenderer renderer) {
        if (!Utils.canUpdate()) return;

        updateEffects(renderer);

        double width = 0;
        double height = 0;

        if (effects.isEmpty() && isInEditor()) {
            width = Math.max(width, renderer.textWidth("Potion Timers (0:00)"));
            height += renderer.textHeight(shadow.get());
        } else {
            double i = 0;
            for (CountedEffect effect : effects) {
                width = Math.max(width, effect.width);
                height += renderer.textHeight(shadow.get());
                if (i > 0) height += 2;
                i++;
            }
        }

        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (!Utils.canUpdate()) return;

        updateEffects(renderer);

        if (effects.isEmpty() && isInEditor()) {
            renderer.text("Potion Timers (0:00)", x, this.y, TextHud.getSectionColor(0), shadow.get());
        } else {
            double y = this.y;
            double i = 0;

            for (CountedEffect effect : effects) {
                double lineWidth = renderer.textWidth(effect.name + effect.time);

                double x = this.x + alignX(lineWidth, alignment.get());
                x = renderer.text(effect.name, x, y, (vanilla.get()) ? effect.vanillaColor : TextHud.getSectionColor(0), shadow.get());
                renderer.text(effect.time, x, y, (vanilla.get()) ? effect.vanillaColor : TextHud.getSectionColor(1), shadow.get());

                y += renderer.textHeight(shadow.get());
                if (i > 0) y += 2;
                i++;
            }
        }
    }

    private void updateEffects(HudRenderer renderer) {
        for (CountedEffect effect : effects) effectPool.free(effect);
        effects.clear();

        for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
            if (potions.get().contains(effect.getEffectType()) && filterMode.get() == FilterMode.Blacklist) continue;
            else if (!potions.get().contains(effect.getEffectType()) && filterMode.get() == FilterMode.Whitelist) continue;

            effects.add(effectPool.get().set(renderer, effect));
        }

        if (sortMode.get() == SortMode.Smallest) {
            effects.sort(Comparator.comparing(effect -> effect.width));
        } else effects.sort(Comparator.comparing(effect -> -effect.width));
    }


    private class CountedEffect {
        public String name;
        public String time;
        public double width;
        public Color vanillaColor = new Color();

        public CountedEffect set(HudRenderer renderer, StatusEffectInstance effect) {
            name = Names.get(effect.getEffectType()) + " ";
            if (roman.get()) {
                int a = effect.getAmplifier() + 1;
                StringBuilder roman = new StringBuilder();
                for (int i = 0; i < numbers.length; i++) {
                    while (a >= numbers[i]) {
                        a -= numbers[i];
                        roman.append(letters[i]);
                    }
                }
                name += roman + " ";
            } else name += effect.getAmplifier() + 1 + " ";

            time = "(" + StatusEffectUtil.durationToString(effect, 1) + ")";
            width = renderer.textWidth(name + time);

            if (vanilla.get()) {
                int c = effect.getEffectType().getColor();
                vanillaColor.r = Color.toRGBAR(c);
                vanillaColor.g = Color.toRGBAG(c);
                vanillaColor.b = Color.toRGBAB(c);
            }

            return this;
        }
    }

    public enum FilterMode {
        Whitelist,
        Blacklist
    }

    public enum SortMode {
        Biggest,
        Smallest
    }
}
