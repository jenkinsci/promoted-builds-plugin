freeStyleJob('test-job-complex') {
    properties{
      promotions {
            promotion {
                name("Development")
                icon("star-red")
                restrict('slave1')
                conditions {
                    manual('testuser'){
                      parameters{
                          textParam("parameterName","defaultValue","description")
                      }
                     }
                }
                actions {
                    downstreamParameterized {
                        trigger("deploy-application","SUCCESS",false,["buildStepFailure": "FAILURE","failure":"FAILURE","unstable":"UNSTABLE"]) {
                            predefinedProp("ENVIRONMENT","dev-server")
                            predefinedProp("APPLICATION_NAME", "\${PROMOTED_JOB_FULL_NAME}")
                            predefinedProp("BUILD_ID","\${PROMOTED_NUMBER}")
                        }
                    }
                }
            }
            promotion {
                name("Test")
                icon("star-yellow")
                restrict('slave2')
                conditions {
                    manual('testuser')
                    upstream("Development")
                }
                actions {
                    downstreamParameterized {
                        trigger("deploy-application","SUCCESS",false,["buildStepFailure": "FAILURE","failure":"FAILURE","unstable":"UNSTABLE"]) {
                            predefinedProp("ENVIRONMENT","test-server")
                            predefinedProp("APPLICATION_NAME", "\${PROMOTED_JOB_FULL_NAME}")
                            predefinedProp("BUILD_ID","\${PROMOTED_NUMBER}")
                        }
                    }
                }
            }
        }
    }
}
