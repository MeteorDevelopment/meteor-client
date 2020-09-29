const axios = require("axios")

const branch = process.argv[2]
const version = process.argv[3]
const build = parseInt(process.argv[4]) + 1
const compareUrl = process.argv[5]

console.log(compareUrl)

let success = true
let description = ""

description += "**Branch:** " + branch
description += "\n**Status:** " + (success ? "success" : "failure")

description += "\n\n**Changes:**"
description += "\n- and what here"

if (success) {
  description += "\n\n**Download:** [meteor-client-" + version + "](https://" + build + "-256699023-gh.circle-artifacts.com/0/build/libs/meteor-client-" + version + ".jar)"
}

axios.post("https://discordapp.com/api/webhooks/760506437348229151/PDbacrTK-dHeYtRVb4YPj-bzb_bj4Bs_Q6Bga8iA4SLXFeKS6prj13uqQs0St5FLKWHF", {
  username: "Dev Builds",
  avatar_url: "https://meteorclient.com/icon.png",
  embeds: [
    {
      title: "meteor client v" + version + " build #" + build,
      description: description,
      url: "https://meteorclient.com",
      color: success ? 3066993 : 15158332,
      thumbnail: {
        url: "https://meteorclient.com/icon.png"
      }
    }
  ]
})