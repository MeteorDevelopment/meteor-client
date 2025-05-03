import * as github from '@actions/github';
import * as core from "@actions/core";

const otherModTerms = [
    "feather",
    "labymod",
    "lunar",
    "optifabric",
    "optifine",
    "origins",
    "pojav",
    "tlauncher",
    "wurst"
];

const anticheatTerms = [
    "aac",
    "anti xray",
    "anti aura",
    "anticheataddition",
    "bypass",
    "disabler",
    "donut",
    "godseye",
    "grim",
    "matrix",
    "ncp",
    "nocheat plus",
    "spartan",
    "vulcan",
    "watchdog",
    "wraith"
];

const featureTerms = [
    ".panic",
    "anti vanish",
    "dupe",
    "force op",
    "god mode",
    "infinite reach",
    "ping bypass",
    "portal godmode",
    "tp aura"
]

const token = process.env.GITHUB_TOKEN;
const octokit = github.getOctokit(token);
const context = github.context

const title = context.payload.issue.title;
const body = context.payload.issue.body;

async function run() {
    const issueText = `${title} ${body}`.toLowerCase();

    for (const term of otherModTerms) {
        if (!checkTerm(issueText, term)) continue;

        const otherModMessage =
            'Meteor does not offer support for "legit clients" like Lunar or Feather, or for launchers that ' +
            'support cracked accounts, or work on non-desktop devices. The mod likely won\'t run in tandem with ' +
            'them, and if you experience any issues while doing so you must troubleshoot any issues yourself.'

        await closeIssue(otherModMessage, octokit, context, term);
        return;
    }

    for (const term of anticheatTerms) {
        if (!checkTerm(issueText, term)) continue;

        const anticheatMessage =
            'Meteor is intended to be used as a utility client on servers that explicitly allow its use. ' +
            'We do not intend to add workarounds for specific anticheats unless it falls within that scope.'

        await closeIssue(anticheatMessage, octokit, context, term);
        return;
    }

    for (const term of featureTerms) {
        if (!checkTerm(issueText, term)) continue;

        const featureMessage =
            'This feature is either impossible to make, associated with cheating/griefing, or falls outside ' +
            'the scope of this project. We do not plan on adding this feature.'

        await closeIssue(featureMessage, octokit, context, term);
        return;
    }

    if (checkTerm(issueText, "old version")) {
        const oldVersionMessage =
            'Our [archive page](https://www.meteorclient.com/archive) stores major Meteor versions for Minecraft ' +
            'versions starting at 1.21.4. If you wish to use older builds on older versions of Minecraft, you will ' +
            'need to build them yourself. **You will not receive support for issues with old versions of Meteor!**'

        await closeIssue(oldVersionMessage, octokit, context, "old version");
        return;
    }

    if (checkTerm(issueText, "forge")) {
        const forgeMessage =
            'Porting Meteor to Forge would take a great amount of time and effort, and would require rewriting major ' +
            'parts of the client to accommodate different APIs. Meteor does not plan to port to Forge in the future.'

        await closeIssue(forgeMessage, octokit, context, "forge");
    }
}

function checkTerm(text, term) {
    if (text.includes(term)) return true;
    if (text.includes(term.replaceAll(' ', '-'))) return true;
    return text.includes(term.replaceAll(' ', ''));
}

async function closeIssue(message, octokit, context, foundTerm) {
    const issueNumber = context.payload.issue.number;
    const owner = context.repo.owner;
    const repo = context.repo.repo;

    let closeMessage = '### This issue is being automatically closed.\n' +
        `${message}`

    if (foundTerm !== '') closeMessage +=
        '\n' +
        '\n' +
        `_This issue was closed because you used the term "${foundTerm}". ` +
        'If you believe your issue is not associated with the given reason ' +
        'you may reopen it._'

    // IDE for whatever reason can't find the rest property yippee

    try {
        await octokit['rest'].issues.createComment({
            owner, repo, issue_number: issueNumber, body: closeMessage
        });

        core.info('Comment added successfully.');
    } catch (error) {
        core.error(`Failed to add comment: ${error.message}`);
    }

    try {
        await octokit['rest'].issues.update({
            owner, repo, issue_number: issueNumber, state: 'closed', state_reason: 'not_planned'
        });

        core.info('Closed issue successfully.');
    } catch (error) {
        core.setFailed(`Failed to close issue: ${error.message}`);
    }
}

run();

