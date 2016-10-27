freeStyleJob('Definition with automatically generated DSL') {
    properties {
        promotions {
            promotion('Foo promotion') {
                conditions {
                    groovy {
                        script {
                            script('false')
                            sandbox(false)
                        }
                    }
                    manual {
                        users('authenticated')
                        parameterDefinitions {
                            stringParameterDefinition {
                                name('FOO')
                                defaultValue('BAR')
                                description('description')
                            }
                        }
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