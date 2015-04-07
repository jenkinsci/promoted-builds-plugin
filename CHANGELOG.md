
## Changelog

##### Version 2.21 (Apr 7, 2015)

*   [issue #24782]((http://issues.jenkins-ci.org/browse/JENKINS-24782) Prevent phantom builds from being scheduled when PromotionProcesses are built directly. 

##### Version 2.20 (Feb 16, 2015)

*   [issue #20492](http://issues.jenkins-ci.org/browse/JENKINS-20492) partial fix, show re-execute button unconditionally
*   fix an NPE in `getBuilds()` when projectName is incorrect
*   added support for rebuild plugin

##### Version 2.19 (Oct 10, 2104)

*   Prevent log file being cluttered with permission exceptions when users have Item.EXTENDED_READ but not Item.CONFIGURE

##### Version 2.18 (Aug 25, 2014)

*   [issue #8963](http://issues.jenkins-ci.org/browse/JENKINS-8963) CRUD for promotion processes using REST API.
*   [issue #23565](http://issues.jenkins-ci.org/browse/JENKINS-23565) Export build display name to promotion job.

##### Version 2.17 (Mar 5, 2014)

*   Repeating/Duplicating promotion parameters&nbsp;([issue #22005](https://issues.jenkins-ci.org/browse/JENKINS-22005))

##### Version 2.16 (Mar 5, 2014)

*   Added `PROMOTED_USER_NAME` environment variable ([issue #16063](https://issues.jenkins-ci.org/browse/JENKINS-16063))
*   Fixed a couple of typos
*   Fixed repeated exception being thrown when installed with literate plugin.

##### Version 2.15 (Jan 28, 2014)

*   Fixed NPE when no Manual Promotion was configured ([issue #20166](http://issues.jenkins-ci.org/browse/JENKINS-20166)

##### Version 2.14

*   Enable editing parameters for re-executions ([issue #8962](http://issues.jenkins-ci.org/browse/JENKINS-8962))

##### Version 2.13

*   Added `PROMOTED_JOB_FULL_NAME` environment variable ([issue #18958](http://issues.jenkins-ci.org/browse/JENKINS-18958))

##### Version 2.12 (Aug 20, 2013)

*   Expose promotion information via the REST API
*   Prevent duplication of promotion processes when creating promotion processes from other plugins

##### Version 2.11 (Jun 09, 2013)

*   Fix for an NPE, and diagnosis for another.
*   Reduced plugin size by eliminating unnecessarily bundled library.

##### Version 2.10 (Mar 30, 2013)

*   NPE when used in 1.507+ ([issue #17341](http://issues.jenkins-ci.org/browse/JENKINS-17341))
*   Test failures, and possibly visible bugs, when used with recent Jenkins ([issue #15156](http://issues.jenkins-ci.org/browse/JENKINS-15156))

##### Version 2.9 (Mar 25, 2013)

*   New promoted build parameter that can be used to select builds that have been promoted
*   Fixed file parameter on ManualApproval not correctly uploading the file

##### Version 2.8 (Oct 30, 2012)

*   Build shouldn't be promoted by a deleted promotion process ([issue #12799](http://issues.jenkins-ci.org/browse/JENKINS-12799))
*   Honor annotated console output ([issue #15328](http://issues.jenkins-ci.org/browse/JENKINS-15328))

##### Version 2.7 (Sep 26, 2012)

*   Added a trigger that allows projects to listen to promotions happening in other projects
*   PROMOTE permission can be used in project matrix-based security ([issue #14890](http://issues.jenkins-ci.org/browse/JENKINS-14890))
*   Downstream jobs textbox is now auto-complete-capable ([issue #14560](http://issues.jenkins-ci.org/browse/JENKINS-14560))

##### Version 2.6.2 (Aug 6, 2012)

*   Fix manual promotion of maven project and matrix project ([issue #13631](http://issues.jenkins-ci.org/browse/JENKINS-13631), [issue #13472](http://issues.jenkins-ci.org/browse/JENKINS-13472))
*   Fix 404 when clicking the promotion's progress bar to view console output ([pull-20](https://github.com/jenkinsci/promoted-builds-plugin/pull/20))

##### Version 2.6.1 (Jul 2, 2012)

*   Fix preventing setting "Restrict where this project can be run" ([issue #14197](http://issues.jenkins-ci.org/browse/JENKINS-14197))

##### Version 2.6 (Jun 21, 2012)

*   Fixed incorrect link from the executor status ([https://github.com/jenkinsci/promoted-builds-plugin/pull/17](https://github.com/jenkinsci/promoted-builds-plugin/pull/17))
*   Added a new promotion process that triggers right after a build is completed when it's parameterized. ([https://github.com/jenkinsci/promoted-builds-plugin/pull/16](https://github.com/jenkinsci/promoted-builds-plugin/pull/16))
*   Fixed a 500 error when viewing promotion on a failed build ([issue #12386](http://issues.jenkins-ci.org/browse/JENKINS-12386))
*   Image files are now in the PNG format.

##### Version 2.5 (Apr 12, 2012)

*   Improved hierarchical project support

##### Version 2.4 (Nov 3, 2011)

*   Fixed a possible NPE that fails the promotion ([issue #11609](http://issues.jenkins-ci.org/browse/JENKINS-11609))
*   Added Promotion History per Promotion Process at Project's Promotion Status page ([issue #10448](http://issues.jenkins-ci.org/browse/JENKINS-10448))

##### Version 2.3.1 (Oct 14, 2011)

*   Don't run promotionProcess that are disabled ([issue #10423](http://issues.jenkins-ci.org/browse/JENKINS-10423))
*   Manual approvement causes an NPE if project name or promotion name contains URI-unsafe chars ([issue #11122](http://issues.jenkins-ci.org/browse/JENKINS-11122))

##### Version 2.3 (Aug 9, 2011)

*   Modified the self-promotion condition so that it does not trigger for builds which are a failure. Also it is now configurable whether to self-promote for unstable builds. ([issue #10250](http://issues.jenkins-ci.org/browse/JENKINS-10250))

##### Version 2.2 (Jul 8, 2011)

*   Added a new promotion condition that immediately promotes itself.

##### Version 2.1 (May 4, 2011)

*   failed to Re-execute promotion if promotion builds plugin is disabled ([issue #9588](http://issues.jenkins-ci.org/browse/JENKINS-9588)).
*   promote plugin should provide ability to select slave node to run ([issue #9260](http://issues.jenkins-ci.org/browse/JENKINS-9260)).
*   Fix for NPE when promoting a build with a custom workspace ([issue #9254](http://issues.jenkins-ci.org/browse/JENKINS-9254)).
*   Fixed a problem where removing a promotion process leaves broken image links in the build history.
*   Fixed a problem where deleting and recreating the promotion process with different case (abc vs ABC) can result in weird behavior on Windows.

##### Version 2.0 (Mar 5 2011)

*   If a promotion criteria is met but the promotion fails, change the icon to represent that.
*   Exposed the job name and the build number of the promotion target to the promotion process (`PROMOTED_JOB_NAME` and `PROMOTED_NUMBER`.)
*   If the build is parameterized, expose that to the promotion process as well
*   Added a new promotion criteria where a promotion in upstream promotes a downstream build.
*   Fully implemented manual approval with user / group permissions and display Approve button on promotion page (no longer need to allow force promotion to all)
*   New promotion action to mark the promoted build as keep forever

##### Version 1.11 (Jan 31 2011)

*   Promote a build even if downstream build is unstable. ([issue #8626](http://issues.jenkins-ci.org/browse/JENKINS-8626))
*   Promote Builds Plugin can use custom workspace. ([issue #8547](http://issues.jenkins-ci.org/browse/JENKINS-8547))
*   Invalid characters in Promotion name causes error. ([issue #7972](http://issues.jenkins-ci.org/browse/JENKINS-7972))
*   Fix promotion permlinks. ([issue #8367](http://issues.jenkins-ci.org/browse/JENKINS-8367))
*   Allow promotion actions to be reordered. ([issue #8548](http://issues.jenkins-ci.org/browse/JENKINS-8548))

##### Version 1.10 (Sep 28 2010)

*   Promotion processes are now recognized as permalinks.

##### Version 1.9 (Jun 9 2010)

*   If fingerprints are not available, use the upstream/downstream triggering information to determine the relationship as a fallback.

##### Version 1.8 (Jun 5 2010)

*   Add the possibility to choose a different color for the star icon, to be able to differenciate the various promotion processes

##### Version 1.7 (Mar 29 2010)

*   Use JDK configured for project when running promotions ([issue #3526](http://issues.jenkins-ci.org/browse/JENKINS-3526))
*   Select node for running promotions from label configured for project ([issue #4089](http://issues.jenkins-ci.org/browse/JENKINS-4089)) (does not yet run on exact node where promoted build ran, unless project is tied to a single node)
*   Show most-recent first in promotion history tables ([issue #6073](http://issues.jenkins-ci.org/browse/JENKINS-6073))

##### Version 1.6 (Dec 30 2009)

*   Fix for running on slave node ([issue #4635](http://issues.jenkins-ci.org/browse/JENKINS-4635))
*   Copy promotions when a job is copied ([issue #3489](http://issues.jenkins-ci.org/browse/JENKINS-3489))
*   Fix broken "Back to Promotion Status" link ([issue #3562](http://issues.jenkins-ci.org/browse/JENKINS-3562))

##### Version 1.5 (Aug 17 2009)

*   Updated to work with Jenkins 1.320

##### Version 1.4 (May 21 2009)

*   Re-doing a release as 1.3 had never shown up in the update center.

##### Version 1.3 (May 11 2009)

*   Expose environment variable `PROMOTED_URL` that points to the URL of the build that's being promoted ([report](http://www.nabble.com/Obtaining-artifacts-from-original-build-during-promotion-td23080743.html))
*   Internal modernization.

##### Version 1.2 (Mar 25 2009)

*   Updated to work with the current Jenkins

##### Version 1.1 (Feb 20 2009)

*   Fixed a problem where Jenkins may issue the same warning multiple times for the same configuration problem ([report](http://www.nabble.com/Promotion-Plugin-tt18464387.html))
*   SVN Tagging is now a permitted promotion step
*   Promotion now fails if any actions are not performed ([issue #2597](http://issues.jenkins-ci.org/browse/JENKINS-2597))
*   Improved logging of promotion build process so users can see what succeeded and what failed
*   Promotion action to build another project no longer does nothing ([issue #1765](http://issues.jenkins-ci.org/browse/JENKINS-1765))
*   Added "Promotion History" pane to the PromotedBuildAction page ([issue #2794](http://issues.jenkins-ci.org/browse/JENKINS-2794))
*   Fixed 404 for last failed link while promotion build occuring ([issue #2578](http://issues.jenkins-ci.org/browse/JENKINS-2578))
