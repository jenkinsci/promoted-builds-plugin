
See the wiki at: <http://wiki.jenkins-ci.org/display/JENKINS/Promoted+Builds+Plugin>

This plugin allows you to distinguish good builds from bad builds by
introducing the notion of 'promotion'.Put simply, a promoted build is a
successful build that passed additional criteria (such as more comprehensive
tests that are set up as downstream jobs.) The typical situation in which you
use promotion is where you have multiple 'test' jobs hooked up as downstream
jobs of a 'build' job. You'll then configure the build job so that the build
gets promoted when all the test jobs passed successfully. This allows you to
keep the build job run fast (so that developers get faster feedback when a  
build fails), and you can still distinguish builds that are good from builds
that compiled but had runtime problems.

Another variation of this usage is to manually promote builds (based on
instinct or something else that runs outside Jenkins.) Promoted builds will
get a star in the build history view, and it can be then picked up by other
teams, deployed to the staging area, etc., as those builds have passed
additional quality criteria. In more complicated scenarios, one can set up
multiple levels of promotions. This fits nicely in an environment where there
are multiple stages of testings (for example, QA testing, acceptance testing,
staging, and production.)

<a name="PromotedBuildsPlugin-PromotionAction"></a>
# Promotion Action

When a build is promoted, you can have Jenkins perform some actions (such as
running a shell script, triggering other jobs, etc. — or in Jenkins lingo, you
can run build steps.) This is useful for example to copy the promoted build to
somewhere else, deploy it to your QA server. You can also define it as a
separate job and then have the promotion action trigger that job.

> **Do not rely on files in the workspace**
The promotion action uses the workspace of the job as the current directory
(and as such the execution of the promotion action is mutually exclusive from
any on-going builds of the job.) But by the time promotion runs, this
workspace can contain files from builds that are totally unrelated from the
build being promoted.

To access the artifacts, use the [Copy Artifact Plugin][1] and choose
the permalink.


<a name="PromotedBuildsPlugin-Usage"></a>
# Usage

To use this plugin, look for the "Promote builds when..." checkbox, on the
Job-configuration page. Define one or a series of promotion processes for
the job.

How might you use promoted builds in your environment? Here are a few
use cases.

Artifact storage -- you may not want to push an artifact to your main artifact
repository on each build. With build promotions, you can push only when an
artifact meets certain criteria. For example, you might want to push it only
after an integration test is run.

Manual Promotions - You can choose a group of people who can run a promotion
manually. This gives a way of having a "sign off" within the build system. For
example, a developer might validate a build and approve it for QA testing only
when a work product is completed entirely. Then another promotion can be added
for the QA hand off to production.

Aggregation of artifacts - If you have a software release that consists of
several not directly related artifacts that are in separate jobs, you might
want to aggregate all the artifacts of a proven quality to a distribution
location. To do this, you can create a new job, adding a "Copy artifacts from
another job" (available through Copy Artifact plugin") for each item you want
to aggregate. To get a certain promotion, select "Use permalink" in the copy
artifact step, then your promoted build should show up in the
list of items to copy.

<a name="PromotedBuildsPlugin-Notes"></a>
# Notes

<a name="PromotedBuildsPlugin-OnDownstreamPromotionConditions"></a>
### On Downstream Promotion Conditions

One of the possible criteria for promoting a build is "When the following
downstream projects build successfully", which basically says if all the
specified jobs successfully built (say build BD of job JD), the build in the
upstream will be promoted (say build BU of job JU.)

This mechanism crucially relies on a "link" between BD and BU, for BU isn't
always the last successful build. We say "BD qualifies BU" if there's this
link, and the qualification is established by one of the following
means:

1.  If BD records fingerprints and one of the fingerprints match some files
that are produced by BU (which is determined from the fingerprint records of
BU), then BD qualifies BU. Intuitively speaking, this indicates that BD uses
artifacts from BU, and thus BD helped verify BU's quality.
2.  If BU triggers BD through a build trigger, then BD qualifies BU. This is
somewhat weak and potentially incorrect, as there's no machine-readable
guarantee that BD actually used anything from BU, but nonetheless this
condition is considered as qualification for those who don't
configure fingerprints.

Note that in the case #1 above, JU and JD doesn't necessarily have to have any
triggering relationship. All it takes is for BD to use some fingerprinted
artifacts from BU, and records those fingerprints in BD. It doesn't matter how
those artifacts are retrieved either — it could be via
[Copy Artifact Plugin][1], it could be through a maven repository, etc. This
also means that you can have a single test job (perhaps parameterized), that
can promote a large number of different upstream jobs.

<a name="PromotedBuildsPlugin-AvailableEnvironmentVariables"></a>
### Available Environment Variables

The following environment variables are added for use in scripts, etc.
These were retrieved from github [here][2].

*   `PROMOTED_URL` - URL of the job being promoted
    *   ex: [http://jenkins/job/job_name_being_promoted/77/](http://jenkins/job/job_name_being_promoted/77/)
*   `PROMOTED_JOB_NAME` - Promoted job name
    *   ex: job_name_being_promoted
*   `PROMOTED_NUMBER` - Build number of the promoted job
    *   ex: 77
*   `PROMOTED_ID` - ID of the build being promoted
    *   ex: 2012-04-12_17-13-03
*   `PROMOTED_USER_NAME` - the user who triggered the promotion
*   `PROMOTED_JOB_FULL_NAME` - the full name of the promoted job

## Job DSL support

```groovy  
freeStyleJob(String jobname) {
  properties{
    promotions {
      promotion {
        name(String promotionName)
        icon(String iconName)
        conditions {
          selfPromotion(boolean evenIfUnstable = true)
          parameterizedSelfPromotion(boolean evenIfUnstable = true, String parameterName, String parameterValue)
          releaseBuild()
          downstream(boolean evenIfUnstable = true, String jobs)
          upstream(String promotionNames)
          manual(String user){
            parameters{
              textParam(String parameterName, String defaultValue, String description)
          }
        }
        actions {
          shell(String command)
        }
      }
    }
  }
}
```

See [StepContext](https://jenkinsci.github.io/job-dsl-plugin/#path/job-steps) in the API Viewer for full documentation about the possible actions.

### Example

```groovy
freeStyleJob('test-job') {
  properties{
    promotions {
      promotion {
        name('Development')
        conditions {
          manual('testuser')
        }
        actions {
          shell('echo hello;')
        }
      }
    }
  }
}
```

## Contributing

* Making new releases is covered in [the "Hosting Plugins" wiki page](https://wiki.jenkins-ci.org/display/JENKINS/Hosting+Plugins).
* If you want to send pull requests or work on some fixes, check [this document](http://jenkins-ci.org/pull-request-greeting).
* There are a lot a issues and features that need attention, the JIRA bug tracker is listed on [the wiki page](https://wiki.jenkins-ci.org/display/JENKINS/Promoted+Builds+Plugin).

[1]: https://wiki.jenkins-ci.org/display/JENKINS/Copy+Artifact+Plugin
[2]: https://github.com/jenkinsci/promoted-builds-plugin/blob/master/src/main/java/hudson/plugins/promoted_builds/Promotion.java
