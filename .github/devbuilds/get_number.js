fetch("https://meteorclient.com/api/stats")
    .then(res => res.json())
    .then(res => console.log("number=" + (parseInt(res.devBuild) + 1)))
