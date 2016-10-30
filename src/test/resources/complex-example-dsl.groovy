freeStyleJob('test-job-complex') {
    properties {
        promotions {
            promotion {
                name("Development")
                icon("star-red")
                restrict('slave1')
                conditions {
                    manual('testuser') {
                        parameters {
                            textParam("parameterName", "defaultValue", "description")
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
                                predefinedProp("ENVIRONMENT", "dev-server")
                                predefinedProp("APPLICATION_NAME", "\${PROMOTED_JOB_FULL_NAME}")
                                predefinedProp("BUILD_ID", "\${PROMOTED_NUMBER}")
                            }
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
                        trigger("deploy-application") {
                            block {
                                buildStepFailure('FAILURE')
                                failure('FAILURE')
                                unstable('UNSTABLE')
                            }
                            parameters {
                                predefinedProp("ENVIRONMENT", "test-server")
                                predefinedProp("APPLICATION_NAME", "\${PROMOTED_JOB_FULL_NAME}")
                                predefinedProp("BUILD_ID", "\${PROMOTED_NUMBER}")
                            }
                        }
                    }
                }
            }
            promotion {
                name('selfPromotion')
                conditions {
                    selfPromotion(false)
                }
            }
            promotion {
                name('parameterizedSelfPromotion')
                conditions {
                    parameterizedSelfPromotion(false, 'PARAM', 'value')
                }
            }
            promotion {
                name('releaseBuild')
                conditions {
                    releaseBuild()
                }
            }
            promotion {
                name('downstream')
                conditions {
                    downstream(false, 'jobName')
                }
            }
            promotion {
                name('upstream')
                conditions {
                    upstream('jobName')
                }
            }
            promotion {
                name('parameters deprecation')
                conditions {
                    // This is not expected to achieve anything but was wrongly exposed at that level, we just test it
                    // to make sure we can properly keep compatibility while also displaying a deprecation message to
                    // the user
                    parameters {}
                }
            }
        }
    }
}
