/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

const axios = require("axios").default;
const FormData = require("form-data");
const fs = require("fs");

const branch = process.argv[2];
const compareUrl = process[3];

// Upload
let jar = "";

fs.readdirSync("../../build/libs").forEach(file => {
    if (!file.endsWith("-all.jar") && !file.endsWith("-sources.jar")) jar = file;
});

let form = new FormData();
form.append("file", fs.createReadStream(jar));

axios.post("https://meteorclient.com/api/uploadDevBuild", form, {
    headers: {
        ...form.getHeaders(),
        "Authorization": process.env.SERVER_TOKEN
    }
}).then(res => {
    let version = res.version;
    let number = res.number;

    // Discord webhook
    axios.get(compareUrl).then(res => {
        let success = true;
        let description = "";

        description += "**Branch:** " + branch;
        description += "\n**Status:** " + (success ? "success" : "failure");

        let changes = "\n\n**Changes:**";
        let hasChanges = false;
        for (let i in res.data.commits) {
            let commit = res.data.commits[i];

            changes += "\n- [`" + commit.sha.substring(0, 7) + "`](https://github.com/MeteorDevelopment/meteor-client/commit/" + commit.sha + ") *" + commit.commit.message + "*";
            hasChanges = true;
        }
        if (hasChanges) description += changes;

        if (success) {
            description += "\n\n**Download:** [meteor-client-" + version + "-" + number + "](https://meteorclient.com/download?devBuild=" + number + ")";
        }

        const webhook = {
            username: "Dev Builds",
            avatar_url: "https://meteorclient.com/icon.png",
            embeds: [
                {
                    title: "meteor client v" + version + " build #" + build,
                    description: description,
                    url: "https://meteorclient.com",
                    color: success ? 3066993 : 15158332
                }
            ]
        };

        axios.post(process.env.DISCORD_WEBHOOK, webhook);
    });
});