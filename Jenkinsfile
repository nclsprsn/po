node {
  def gradleHome

  stage('Clean') {
    deleteDir()
  }

  stage('Preparation') {
    gradleHome = tool 'gradle'
  }

  stage('Checkout') {

    checkout([$class: 'GitSCM',
      branches: scm.branches,
      submoduleCfg: [],
      userRemoteConfigs: [[credentialsId: 'bb625c49-e00c-4b89-a409-d06f732b6929', url: 'https://nclsprsn@gitlab.com/nclsprsn/po.git']]
    ])

  }

  stage('Verify') {
      sh "'${gradleHome}/bin/gradle' test"
  }
}