
pipeline {
    agent any
    options {
        skipDefaultCheckout()
    }
    environment {
        RELEASE_PATTERN = "develop-\\d+((\\.\\d\\d?)+)?(?!.)"
        TOKEN = credentials('mledot-git-token')
    }
    stages {
        stage('Clean the ws') {
      steps {
        cleanWs()
      }
        }
        stage('Clone repos') {
      steps {
        script {
          checkout scmGit(
                        branches: [[name: 'main']],
                        extensions: [submodule(parentCredentials: true, recursiveSubmodules: true, reference: '')],
                        userRemoteConfigs: [[
                            credentialsId: 'mledot-token',
                            url: 'https://github.com/BondR-Consulting/bmi-calculator-2.git'
                            ]]
                            )
        }
      }
        }

        stage('Install modules and run tests') {
      steps {
        script {
          nodejs(nodeJSInstallationName: 'Node16') {
            sh 'npm install -f'
            sh 'npm test'
          }
        }
      }
        }

    post {
        always {
            script {
          if (env.branch ==~ env.REGEX_RELEASE
                    && currentBuild.result ==~ 'SUCCESS') {
            steps {
              script {
                def accessToken = env.TOKEN
                def repo = 'BondR-Consulting/bmi-calculator-2'
                def baseBranch = 'main'
                def headBranch = ${ env.branch }
                def title = 'Merging to main'
                def assignee = 'mledot'
                def reviewer = 'smbondrnet'

                def pr = sh(script: """
                                    curl -s -X POST -H 'Authorization: token ${accessToken}' -d '{
                                        "title": "${title}",
                                        "head": "${headBranch}",
                                        "base": "${baseBranch}"
                                    }' https://api.github.com/repos/${repo}/pulls
                                """ returnStdout: true).trim()

                def prNumber = readJSON text: pr
                sh """
                                    curl -s -X POST -H 'Authorization: token ${accessToken}' -d '{
                                        "assignees": ["${assignee}"],
                                        "reviewers": ["${reviewer}"]
                                    }' https://api.github.com/repos/${repo}/pulls/${prNumber}/requested_reviewers
                                """
                                    }
                                    }
              }
            }
                    }
            }
        }
    }
