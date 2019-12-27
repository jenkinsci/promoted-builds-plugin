freeStyleJob('copy-artifacts-test') {
    properties {
        promotions {
            promotion {
                name('Development')
                conditions {
                    manual('tester')
                }
                actions {
                    actions {
                        copyArtifacts('source-job') {
                            includePatterns('lib/artifact.jar')
                            buildSelector {
                                buildNumber('${PROMOTED_NUMBER}')
                            }
                        }
                    }
                }
            }
        }
    }
}