const fs = require("fs");
const path = require("path");

const branch = process.argv[2];
const compareUrl = process.argv[3];
const success = process.argv[4] === "true";

function send(version, number) {
    fetch(compareUrl)
        .then(res => res.json())
        .then(res => {
            let description = "";

            description += "**Branch:** " + branch;
            description += "\n**Status:** " + (success ? "success" : "failure");

            let changes = "\n\n**Changes:**";
            let hasChanges = false;
            for (let i in res.commits) {
                let commit = res.commits[i];

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

            fetch(process.env.DISCORD_WEBHOOK, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(webhook)
            });
        });
}

if (success) {
    let jar = "";
    fs.readdirSync("../../build/libs").forEach(file => {
        if (!file.endsWith("-all.jar") && !file.endsWith("-sources.jar")) jar = "../../build/libs/" + file;
    });

    let form = new FormData();
    form.set(
        "file",
        new Blob([fs.readFileSync(jar)], { type: "application/java-archive" }),
        path.basename(jar)
    );

    fetch("https://meteorclient.com/api/uploadDevBuild", {
        method: "POST",
        headers: {
            "Authorization": process.env.SERVER_TOKEN
        },
        body: form
    })
        .then(async res => {
            let data = await res.json();

            if (res.ok) {
                send(data.version, data.number);
            }
            else {
                console.log("Failed to upload dev build: " + data.error);
            }
        });
}
else {
    fetch("https://meteorclient.com/api/stats")
        .then(res => res.json())
        .then(res => send(res.dev_build_version, parseInt(res.devBuild) + 1));
}
