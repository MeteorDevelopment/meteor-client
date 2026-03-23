/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

import * as fs from "fs"
import * as path from "path"
import {fileURLToPath} from "url"

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export async function getMcVersion() {
    const filePath = path.resolve(__dirname, "../../gradle/libs.versions.toml")

    if (!fs.existsSync(filePath)) {
        throw new Error(`File not found: ${filePath}`)
    }

    const content = await fs.promises.readFile(filePath, "utf-8")

    const match = content.match(/^\s*minecraft\s*=\s*["']([^"']+)["']\s*$/m)
    if (match) return match[1].trim()

    throw new Error(`Failed to find minecraft version in ${filePath}`)
}
