#!groovy

def slackChannel = "#team-slackchannel"

properties([
    parameters([
        string(name: 'releaseType', description: "major, minor, or patch", defaultValue: 'minor')
   ])
])

node('slave-medium') {
    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm', 'defaultFg': 1, 'defaultBg': 2]) {
        wrap([$class: 'TimestamperBuildWrapper']) {
            env.RELEASE_TYPE = releaseType
            try {   
                stage('checkout') {
                    checkout scm
                }
		stage('npm install') {
                    sh "yarn install"
                }               
		stage('unit tests'){
                    sh "yarn test"
                }			   
                if (env.BRANCH_NAME == 'master') {           
                    stage('push to artifactory') {
                        sh "yarn run precompile"
                        sh "npm version ${env.RELEASE_TYPE}"
                        sh "git push origin HEAD:master --follow-tags"
                        sh "npm publish"
                    }   
                    stage('notify slack') {
                        slackSend channel: slackChannel, color: '#2ECC71', message: "Push ${JOB_NAME} (release type: ${env.RELEASE_TYPE}) to Artifactory - Complete - <${BUILD_URL}|See the build>"
                    }
                }
            }
            catch(error) {
                slackSend channel: slackChannel, color: '#CC3D2E', message: "Push ${JOB_NAME} (release type: ${env.RELEASE_TYPE}) to Artifactory - Failed - <${BUILD_URL}|See the build>"
                currentBuild.result = "FAILURE"
                throw error
            }
            stage('clean workspace') {
                cleanWs()
            }
        }
    }
}
