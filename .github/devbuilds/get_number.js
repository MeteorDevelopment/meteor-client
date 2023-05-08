const axios = require("axios").default;

axios.get("https://meteorclient.com/api/stats").then(res => {
    console.log("number=" + (parseInt(res.data.devBuild) + 1));
});
