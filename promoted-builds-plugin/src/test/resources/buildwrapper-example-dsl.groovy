freeStyleJob('build-wrapper-test') {
    properties {
        promotions {
            promotion {
                name('build-wrapper-promotion')
                conditions {
                    manual('tester')
                }
                wrappers {
                    timestamps()
                }
                actions {
                    actions {
                        shell('echo hello;')
                    }
                }
            }
        }
    }
}