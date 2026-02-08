pipeline {

    agent {
        node {
            label 'master'
        }
    }

    parameters {
      string defaultValue: '', description: 'Project Name. ', name: 'PROJECT_NAME', trim: true
      string defaultValue: '', description: 'Bundle version. ', name: 'BUNDLE_VERSION', trim: true
      string defaultValue: '', description: 'Bundle name. ', name: 'BUNDLE_NAME', trim: true
    }

    options {
        buildDiscarder logRotator( 
                    daysToKeepStr: '16', 
                    numToKeepStr: '10'
            )
    }

    stages {
        
        stage('Preparation') {
            steps{
                git branch: "prod",
                url: 'https://github.com/chamilaadhi/poc-cicd-deployment-repo.git'
            }
        }
        
        stage('Download bundle') {
            steps {
                sh '''#!/bin/bash
                rm -f ${BUNDLE_NAME}".zip"
                wget https://${ARTIFACTORY_HOST}/artifactory/${ARTIFACTORY_REPO}/${PROJECT_NAME}/${BUNDLE_VERSION}/${BUNDLE_NAME}.zip
                ls
                '''
            }
        }

        stage('Setup Environment for APICTL') {
            steps {
                sh '''#!/bin/bash
                envs=$(apictl get envs --format "{{.Name}}")
                if [ -z "$envs" ]; 
                then 
                    echo "No environment configured. Setting prod environment.."
                    apictl add env prod --apim https://${APIM_PROD_HOST}:9443 
                else
                    echo "Environments :"$envs
                    if [[ $envs != *"prod"* ]]; then
                    echo "Prod environment is not configured. Setting prod environment.."
                    apictl add env prod --apim https://${APIM_PROD_HOST}:9443 
                    fi
                fi
                '''
            }
        }

         stage('Deploy to Production Environment') {
            steps {
                    sh '''#!/bin/bash
                    name=${BUNDLE_NAME}".zip"
                    # derive param content name 
                    fileName=$(echo $name | sed 's/\\(.*\\).zip/\\1 /')
                    deploymentName=$(echo $fileName | sed 's/\\(.*\\)_/\\1-/')
                    paramPath="DeploymentArtifacts_"$deploymentName
                    echo "Param path :"$paramPath
                    # login to the prod environment
                    apictl login prod -u admin -p admin -k
                    # import the artifact
                    message=$(apictl import api -f $name --params $paramPath -e prod --update -k)
                    if [ "$message" = "Successfully imported API." ]; then
                        echo "Successfully imported API."
                    else
                        echo $message
                    fi
                    rm $name
                    '''
            }
        }
    }   
}
@awsvpc
Comment
