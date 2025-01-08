/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

const fs = require("fs")
const readline = require("readline")

export async function getMcVersion() {
    let lines = readline.createInterface({
        input: fs.createReadStream("../../gradle.properties"),
        crlfDelay: Infinity
    })

    let mcVersion = ""

    for await (const line of lines) {
        if (line.startsWith("minecraft_version")) {
            mcVersion = line.substring(0, line.indexOf("="))
            break
        }
    }

    if (mcVersion === "") {
        console.log("Failed to read minecraft_version")
        process.exit(1)
    }

    return mcVersion
}
