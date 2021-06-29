freeStyleJob('dynamic-dsl-test') {
    properties {
        promotions {
            promotion {
                name('Development')
                conditions {
                    manual('tester')
                }
                actions {
                    jobDsl {
                        scriptText('println test')
                    }
                }
            }
        }
    }
}
