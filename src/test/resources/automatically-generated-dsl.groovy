freeStyleJob('Job-from-automatically-generated-DSL') {
    label('windows')
    properties {
        promotions {
            promotion('Development') {
                icon("star-red")
                label('slave1')
                conditions {
                    groovy {
                        script {
                            script('''return true == !false;''')
                            sandbox(false)
                        }
                    }
                    manual {
                        users('testuser')
                        parameterDefinitions {
                            stringParameterDefinition {
                                name('FOO')
                                defaultValue('BAR')
                                description('description')
                            }
                        }
                    }
                    selfPromotion {
                        evenIfUnstable(false) // required
                    }
                    parameterizedSelfPromotion {
                        evenIfUnstable(false) // required
                        parameterName('name') // required
                        parameterValue('value') // required
                    }
                    downstreamPass {
                        jobs('downstream') // required
                        evenIfUnstable(false) // required
                    }
                    upstream {
                        promotions('upstream') // required
                    }
                }
                actions {
                    shell {
                        command('echo foo')
                    }
                    triggerBuilder {
                        configs {
                            blockableBuildTriggerConfig {
                                projects('projects')
                                block {
                                    buildStepFailureThreshold('FAILURE')
                                    unstableThreshold('FAILURE')
                                    failureThreshold('FAILURE')
                                }
                                configFactories {}
                                configs {
                                    predefinedBuildParameters {
                                        properties('''\
                                            ENVIRONMENT=dev-server
                                            APPLICATION_NAME=${PROMOTED_JOB_FULL_NAME}
                                            BUILD_ID=${PROMOTED_NUMBER}
                                            '''.stripIndent())
                                    }
                                }
                            }
                        }
                    }
                }
            }
            promotion('test') {
                icon("star-yellow")
                label('slave2')
                conditions {
                    manual {
                        users('testuser')
                    }
                    upstream {
                        promotions("Development")
                    }
                }
                actions {
                    shell {
                        command('echo bar')
                    }
                    triggerBuilder {
                        configs {
                            blockableBuildTriggerConfig {
                                projects('projects')
                                block {
                                    buildStepFailureThreshold('FAILURE')
                                    unstableThreshold('FAILURE')
                                    failureThreshold('FAILURE')
                                }
                                configFactories {}
                                configs {
                                    predefinedBuildParameters {
                                        properties('''\
                                            ENVIRONMENT=test-server
                                            APPLICATION_NAME=${PROMOTED_JOB_FULL_NAME}
                                            BUILD_ID=${PROMOTED_NUMBER}
                                            '''.stripIndent())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
