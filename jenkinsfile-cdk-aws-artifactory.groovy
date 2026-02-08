pipeline {
        agent {
            docker {
                image '***'
                registryUrl '***'
                registryCredentialsId '***'
                args '-u root -e http_proxy=*** -e https_proxy=*** -e HTTP_PROXY=*** -e HTTPS_PROXY=*** -e  no_proxy=***'
            }
        } 

        environment {
            HTTP_PROXY = ***
            HTTPS_PROXY = ***
        } 

        stages {
            stage('Build') {
                steps {
                    withCredentials([string(credentialsId: 'JENKINS_ROLE_ARN', variable: 'JENKINS_ROLE_ARN'), string(credentialsId: 'ROLE_EXTERNAL_ID', variable: 'ROLE_EXTERNAL_ID'),
                    usernamePassword(credentialsId: 'artifactory-jenkins-user-password', usernameVariable: 'ARTIFACTORY_JENKINS_USERNAME', passwordVariable: 'ARTIFACTORY_JENKINS_PASSWORD')]) {
                        sh 'apt update'
                        sh 'apt install -y jq'
                        sh 'curl https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o "awscliv2.zip"'
                        sh 'unzip -o awscliv2.zip'
                        sh './aws/install --update'
                        sh 'echo "Assuming an aws role.."'

                        script {
                            echo "Assuming an AWS role.."
                            def assumeRoleOutput = sh(script: "aws sts assume-role --role-arn ${JENKINS_ROLE_ARN} --role-session-name jenkins-workbench --region ${AWS_DEFAULT_REGION} --external-id ${ROLE_EXTERNAL_ID}", returnStdout: true).trim()
                            def credentials = readJSON(text: assumeRoleOutput).Credentials
                            def awsAccessKeyId = credentials.AccessKeyId
                            def awsSecretAccessKey = credentials.SecretAccessKey
                            def awsSessionToken = credentials.SessionToken
                            sh 'aws sts get-caller-identity'
                            def accountId = (JENKINS_ROLE_ARN =~ /arn:aws:iam::(\d+):/)[0][1]

                            env.AWS_ACCESS_KEY_ID = credentials.AccessKeyId
                            env.AWS_SECRET_ACCESS_KEY = credentials.SecretAccessKey
                            env.AWS_SESSION_TOKEN = credentials.SessionToken
                            env.AWS_DEFAULT_ACCOUNT = accountId
                        }

                        sh 'echo "The aws role was successfully assumed."'
                        sh "npm config set proxy ${HTTP_PROXY}"
                        sh "npm config set https-proxy ${HTTPS_PROXY}"
                        sh 'npm install'
                        sh 'npm install -g aws-cdk'
                        sh 'npm run build'
                    }
                }
            }

            stage('Test') {
                steps {
                    sh 'npm run test'
                }
            }

            stage('Deploy') {
                steps {
                    sh """
                        export AWS_ACCESS_KEY_ID=\$AWS_ACCESS_KEY_ID
                        export AWS_SECRET_ACCESS_KEY=\$AWS_SECRET_ACCESS_KEY
                        export AWS_SESSION_TOKEN=\$AWS_SESSION_TOKEN
                        export AWS_DEFAULT_ACCOUNT=\$AWS_DEFAULT_ACCOUNT
                        cdk deploy -c env=${ENV} ${STACK_NAME} --require-approval never
                    """
                }
