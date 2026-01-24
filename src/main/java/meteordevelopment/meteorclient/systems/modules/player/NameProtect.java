/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

public class NameProtect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> nameProtect = sgGeneral.add(new BoolSetting.Builder()
        .name("name-protect")
        .description("Hides your name client-side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("Name to be replaced with.")
        .defaultValue("seasnail")
        .visible(nameProtect::get)
        .build()
    );

    private final Setting<Boolean> nickAll = sgGeneral.add(new BoolSetting.Builder()
        .name("nick-all")
        .description("Gives all players random names and skins.")
        .defaultValue(false)
        .build()
    );

    // 【关键修改1】默认关闭皮肤保护，这样才能看到大家的真实头像/Team Icon
    private final Setting<Boolean> skinProtect = sgGeneral.add(new BoolSetting.Builder()
        .name("skin-protect")
        .description("Make players become Steves.")
        .defaultValue(false) 
        .build()
    );

    private final Setting<Boolean> nickOthers = sgGeneral.add(new BoolSetting.Builder()
        .name("nick-others")
        .description("Only apply skin protection to other players, not yourself.")
        .defaultValue(false)
        .visible(() -> nickAll.get() || skinProtect.get())
        .build()
    );

    private String username = "If you see this, something is wrong.";
    private final Map<String, String> fakeNames = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public NameProtect() {
        super(Categories.Player, "name-protect", "Hide player names and skins.");
    }

    @Override
    public void onActivate() {
        username = mc.getSession().getUsername();
        fakeNames.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (nickAll.get() && mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                String name = entry.getProfile().name(); // 注意: 旧版本可能是 getProfile().name()
                if (!fakeNames.containsKey(name)) {
                    Team team = entry.getScoreboardTeam();
                    fakeNames.put(name, generateShortName(team));
                }
            }
        }
    }

    // 基本的字符串替换，用于聊天栏等纯文本地方
    public String replaceName(String string) {
        if (string == null || !isActive()) return string;

        String result = string;
        if (nameProtect.get()) {
            result = result.replace(username, name.get());
        }
        
        if (nickAll.get()) {
            if (fakeNames.size() < 1000) {
                 for (Map.Entry<String, String> entry : fakeNames.entrySet()) {
                     result = result.replace(entry.getKey(), entry.getValue());
                 }
            }
        }
        return result;
    }

    public String getName(String original) {
        if (!isActive()) return original;

        if (original.equals(username) && nameProtect.get()) {
            return name.get();
        }

        if (nickAll.get()) {
            // 在这里我们无法直接获取PlayerListEntry，所以暂时保留随机生成
            // 在getDisplayName中会根据队伍生成一致的名字
            return fakeNames.computeIfAbsent(original, k -> generateShortName(null));
        }

        return original;
    }

    // 生成简短的名字格式: X-Y-Z (如: A-1-B, C-2-D)
    private String generateShortName(Team team) {
        StringBuilder sb = new StringBuilder();
        
        // 生成第一个字母 (A-Z)
        char firstLetter;
        if (team != null) {
            // 根据队伍名称生成一致的第一个字母
            int teamHash = team.getName().hashCode();
            firstLetter = (char) ('A' + Math.abs(teamHash) % 26);
        } else {
            // 随机生成第一个字母
            firstLetter = (char) ('A' + random.nextInt(26));
        }
        sb.append(firstLetter);
        sb.append("-");
        
        // 生成数字 (1-9)
        int number = random.nextInt(9) + 1;
        sb.append(number);
        sb.append("-");
        
        // 生成第二个字母 (A-Z)
        char secondLetter;
        if (team != null) {
            // 根据队伍名称生成一致的第二个字母（与第一个字母不同）
            int teamHash = team.getName().hashCode() + 13; // 加一个偏移确保与第一个字母不同
            secondLetter = (char) ('A' + Math.abs(teamHash) % 26);
        } else {
            // 随机生成第二个字母
            secondLetter = (char) ('A' + random.nextInt(26));
        }
        sb.append(secondLetter);
        
        return sb.toString();
    }


    public Text getDisplayName(PlayerListEntry entry) {
        if (!isActive()) return null;

        String originalName = entry.getProfile().name();
        String newName = getName(originalName);

        // 如果名字没变，返回null，让MC使用默认逻辑
        if (originalName.equals(newName)) return null;

        MutableText text = Text.literal(newName);

        // 获取该玩家当前的队伍数据
        Team team = entry.getScoreboardTeam();
        if (team != null) {
            return team.decorateName(text);
        }

        return text;
    }

    public String getNameForDisplay(String original) {
        if (!isActive()) return original;
        if (original.equals(username) && nameProtect.get()) {
            return name.get();
        }
        return original;
    }

    public boolean skinProtect() {
        return isActive() && skinProtect.get();
    }
    
    public boolean nickAll() {
        return isActive() && nickAll.get();
    }
    
    public boolean nickOthers() {
        return isActive() && nickOthers.get();
    }
}