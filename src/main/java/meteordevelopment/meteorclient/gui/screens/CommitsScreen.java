/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.util.Util;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class CommitsScreen extends WindowScreen {
    private final MeteorAddon addon;
    private Commit[] commits;

    public CommitsScreen(GuiTheme theme, MeteorAddon addon) {
        super(theme, "Commits for " + addon.name);

        this.addon = addon;

        locked = true;
        lockedAllowClose = true;

        MeteorExecutor.execute(() -> {
            GithubRepo repo = addon.getRepo();
            Response res = Http.get(String.format("https://api.github.com/repos/%s/compare/%s...%s", repo.getOwnerName(), addon.getCommit(), repo.branch())).sendJson(Response.class);

            if (res != null) {
                commits = res.commits;
                taskAfterRender = this::populateWidgets;
            }
            else locked = false;
        });
    }

    @Override
    public void initWidgets() {
        // Only initialize widgets after data arrives
    }

    private void populateWidgets() {
        // Top
        WHorizontalList l = add(theme.horizontalList()).expandX().widget();

        String text = "There are %d new commits";
        if (commits.length == 1) text = "There is %d new commit";
        l.add(theme.label(String.format(text, commits.length))).expandX();

        String website = addon.getWebsite();
        if (website != null) l.add(theme.button("Website")).widget().action = () -> Util.getOperatingSystem().open(website);

        l.add(theme.button("GitHub")).widget().action = () -> {
            GithubRepo repo = addon.getRepo();
            Util.getOperatingSystem().open(String.format("https://github.com/%s/tree/%s", repo.getOwnerName(), repo.branch()));
        };

        // Commits
        if (commits.length > 0) {
            add(theme.horizontalSeparator()).padVertical(theme.scale(8)).expandX();

            WTable t = add(theme.table()).expandX().widget();
            t.horizontalSpacing = 0;

            for (Commit commit : commits) {
                String date = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(commit.commit.committer.date));
                t.add(theme.label(date)).top().right().widget().color = theme.textSecondaryColor();

                t.add(theme.label(getMessage(commit))).widget().action = () -> Util.getOperatingSystem().open(String.format("https://github.com/%s/commit/%s", addon.getRepo().getOwnerName(), commit.sha));
                t.row();
            }
        }

        locked = false;
    }

    private static String getMessage(Commit commit) {
        StringBuilder sb = new StringBuilder(" - ");
        String message = commit.commit.message;

        for (int i = 0; i < message.length(); i++) {
            if (i >= 80) {
                sb.append("...");
                break;
            }

            char c = message.charAt(i);

            if (c == '\n') {
                sb.append("...");
                break;
            }

            sb.append(c);
        }

        return sb.toString();
    }

    private static class Response {
        public Commit[] commits;
    }

    private static class Commit {
        public String sha;
        public CommitInner commit;
    }

    private static class CommitInner {
        public Committer committer;
        public String message;
    }

    private static class Committer {
        public String date;
    }
}
