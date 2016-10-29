freeStyleJob('configure-block-test') {
    properties {
        promotions {
            promotion('PromotionName') {
                configure { node ->
                    node / buildSteps / 'foo.bar.CustomAction' {
                        customAttribute 'customValue'
                    }
                }
            }
        }
    }
}
