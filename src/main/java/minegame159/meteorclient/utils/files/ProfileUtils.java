/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.files;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.accounts.AccountManager;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.macros.MacroManager;
import minegame159.meteorclient.modules.ModuleManager;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProfileUtils {
    private static final File FOLDER = new File(MeteorClient.FOLDER, "profiles");

    public static List<String> getProfiles() {
      String[] children = FOLDER.list();
      List<String> profiles = new ArrayList<>(0);

      if (children != null) {
          for (String child : children) {
              File file = new File(child);
              if (!child.contains(".")) profiles.add(file.getName());
          }
      }

      return profiles;
    }

    public static boolean isProfile(String profile) {
        String[] children = FOLDER.list();

        if (children != null) {
            for (String child : children) {
                if (child.equals(profile)) return true;
            }
        }

        return false;
    }

    public static boolean save(String profile) {
        if (profile.isEmpty() || profile.contains(".")) return false;
        File folder = new File(FOLDER, profile);

        Config.INSTANCE.save(new File(folder, Config.INSTANCE.getFile().getName()));
        ModuleManager.INSTANCE.save(new File(folder, ModuleManager.INSTANCE.getFile().getName()));
        FriendManager.INSTANCE.save(new File(folder, FriendManager.INSTANCE.getFile().getName()));
        MacroManager.INSTANCE.save(new File(folder, MacroManager.INSTANCE.getFile().getName()));
        AccountManager.INSTANCE.save(new File(folder, AccountManager.INSTANCE.getFile().getName()));

        return true;
    }

    public static void load(String profile) {
        MeteorClient.INSTANCE.save();
        ModuleManager.INSTANCE.disableAll();

        File folder = new File(FOLDER, profile);

        Config.INSTANCE.load(new File(folder, Config.INSTANCE.getFile().getName()));
        ModuleManager.INSTANCE.load(new File(folder, ModuleManager.INSTANCE.getFile().getName()));
        FriendManager.INSTANCE.load(new File(folder, FriendManager.INSTANCE.getFile().getName()));
        MacroManager.INSTANCE.load(new File(folder, MacroManager.INSTANCE.getFile().getName()));
        AccountManager.INSTANCE.load(new File(folder, AccountManager.INSTANCE.getFile().getName()));
    }

    public static void delete(String profile) {
        try {
            FileUtils.deleteDirectory(new File(FOLDER, profile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
