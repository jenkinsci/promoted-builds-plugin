freeStyleJob('Definition with automatically generated DSL') {
    properties {
        promotions {
            promotion {
                name = 'Foo Promotion'
                conditions {
                    groovy {
                        script {
                            script 'false'
                            sandbox false
                        }
                        unmetQualificationLabel ''
                        metQualificationLabel ''
                    }
                }
                actions {
                    downstreamParameterized {
                        trigger("deploy-application") {
                            block {
                                buildStepFailure('FAILURE')
                                failure('FAILURE')
                                unstable('UNSTABLE')
                            }
                            parameters {
                                predefinedProp("ENVIRONMENT","dev-server")
                                predefinedProp("APPLICATION_NAME", "\${PROMOTED_JOB_FULL_NAME}")
                                predefinedProp("BUILD_ID","\${PROMOTED_NUMBER}")
                            }
                        }
                    }
                }
            }
        }
    }
}