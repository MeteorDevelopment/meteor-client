const axios = require("axios").default;
const FormData = require("form-data");
const fs = require("fs");

const branch = process.argv[2];
const compareUrl = process.argv[3];
const success = process.argv[4] === "true";

function send(version, number) {
    axios.get(compareUrl).then(res => {
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
                    title: "meteor client v" + version + " build #" + number,
                    description: description,
                    url: "https://meteorclient.com",
                        color: success ? 2672680 : 13117480
                }
            ]
        };

        axios.post(process.env.DISCORD_WEBHOOK, webhook);
    });
}

if (success) {
    let jar = "";
    fs.readdirSync("../../build/libs").forEach(file => {
        if (!file.endsWith("-all.jar") && !file.endsWith("-sources.jar")) jar = "../../build/libs/" + file;
    });

    let form = new FormData();
    form.append("file", fs.createReadStream(jar));

    axios.post("https://meteorclient.com/api/uploadDevBuild", form, {
        headers: {
            ...form.getHeaders(),
            "Authorization": process.env.SERVER_TOKEN
        }
    }).then(res => {
        send(res.data.version, res.data.number)
    });
}
else {
    axios.get("https://meteorclient.com/api/stats").then(res => {
        send(res.data.dev_build_version, parseInt(res.data.devBuild) + 1)
    });
}
