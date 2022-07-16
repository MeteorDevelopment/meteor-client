const axios = require("axios").default;

axios.get("https://meteorclient.com/api/stats").then(res => {
    console.log("::set-output name=number::" + (parseInt(res.data.devBuild) + 1));
});