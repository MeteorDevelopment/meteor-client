pipeline {
  agent {
    docker 'gradle:latest'
  }
  environment {
        VERSION = '0.3.3'
  }
  stages {
    stage('Gradle Build') {
      steps {
        withGradle {
            sh './gradlew build'
            sh "mv build/libs/meteor-client-${VERSION}.jar meteor-client-${VERSION}-${env.BUILD_NUMBER}.jar"
        }
      }
    }
  }
  post {
    always {
      archiveArtifacts artifacts: "meteor-client-${VERSION}-${env.BUILD_NUMBER}.jar", fingerprint: true, followSymlinks: false, onlyIfSuccessful: true
      script {
        def artifactUrl = env.BUILD_URL + "artifact/"
	def msg = "**Branch:** " + env.BRANCH_NAME + "\n"
        msg += "**Status:** " + currentBuild.currentResult.toLowerCase() + "\n"
        if (!currentBuild.changeSets.isEmpty()) {
            msg += "**Changes:** \n"
            currentBuild.changeSets.first().getLogs().each {
                msg += "- `" + it.getCommitId().substring(0, 8) + "` *" + it.getComment().substring(0, it.getComment().length()-1) + "*\n"
            }
        } else {
            msg += "no changes for this run\n"
        }

        if (msg.length() > 1024) msg.take(msg.length() - 1024)

        def filename
        if (!currentBuild.rawBuild.getArtifacts().isEmpty()) { 
        msg += "\n **Artifacts:**\n"
        currentBuild.rawBuild.getArtifacts().each {
            filename = it.getFileName()
            msg += "- [${filename}](${artifactUrl}${it.getFileName()})\n"
        }
        }

        withCredentials([string(credentialsId: 'meteor-discord-release', variable: 'discordWebhook')]) {
            discordSend thumbnail: "https://meteorclient.com/icon.png", successful: currentBuild.resultIsBetterOrEqualTo('SUCCESS'), description: "${msg}", link: env.BUILD_URL, title: "meteor-client v${VERSION} build #${BUILD_NUMBER}", webhookURL: "${discordWebhook}"
        }
      }
    }
  }
}
