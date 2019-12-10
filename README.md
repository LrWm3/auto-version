# auto-version

Automatically version [auto-version-target](https://github.com/WilliamTheMarsman/auto-version-target) when an image version is updated.

## See it in action

As an example, we can see many commits performed by this job in the [Github Network for auto-version-target](https://github.com/WilliamTheMarsman/auto-version-target/network), or in the [commit log](https://github.com/WilliamTheMarsman/auto-version-target/commits/release-0.1.0). Jenkins is responsible for any commits in the 'rc10' range.

## Interface

```groovy
//
// Automatically increase the version of an image in a project, committing and pushing to provided release
//
// release : The release branch to target for the SUPERSET project
// image   : The image to target in the SUPERSET project
// version : The new version to uprev to in the SUPERSET project
///
// Notes
//   If SUPERSET project is not present, it is cloned
//   If version supplied equals the current version in the SUPERSET project, nothing is done
//   If release branch doesn't exist, nothing is done
//   Script assumes all images are part of a larger group: e.g. 'main-project/image:version'
def autoVersion(release, image, version){
  // boring impl details
}

```

## Warnings

Nothing serious (yet)

1. The release branch must already exist on the target project, or no action will be taken to uprev versions of images provided. create the release branch manually. This is to enable the workflow below.
2. Currently, there is no protection in place to prevent a version going backwards; the thought is, once a release is cut, the related branch will be deleted on the remote repository. If the branch is deleted on the remote, then this script will be unable to change image versions further, as it checks to see if the branch exists on the remote repository before attempting to make changes. If, for some reason, more changes are required on a given release, a new branch can be created from the `tag` made for the release with additional hotfixes.

## Usage

See `auto-version.spec.groovy` for an example of this script being called. It can be run locally.

```bash
cd auto-version
groovy src/auto-version.spec.groovy
```

For usage with Jenkins, see below.

### Jenkins usage

The main reason to use this script is to use it with CI engine. An example set-up is described below

1. Add permissions to `<JENKINS_HOST>/script-approval`, under `Signatures Already Approved`
2. Create a pipeline jenkins job called 'uprev-version'
3. Check 'Do not allow concurrent builds'
4. Check 'This project is parameterized'
5. Add String parameter RELEASE_BRANCH with description from below
6. Add String parameter IMAGE_NAME with description from below
7. Add String parameter IMAGE_VERSION with description from below
8. Select 'Pipeline script'
9. Copy and paste the pipeline script found in this [Jenkinsfile](./example/jenkins-int/Jenkinsfile)
10. In the script pasted, `lines 35-37`, change the target project name, project directory and git repo URL to the project you would like to update.

The job can now be run by using 'build with parameters'

#### Required Signature Approvals

Add the following to `<JENKINS_HOST>/script-approval`, under `Signatures Already Approved`

```
method java.io.File getName
method java.io.File getPath
method java.lang.Process exitValue
method java.lang.Process waitFor
new groovy.util.AntBuilder
new java.io.File java.lang.String
staticField groovy.io.FileType FILES
staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods execute java.util.List
staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods getText java.io.File
staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods size java.util.regex.Matcher
staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods traverse java.io.File java.util.Map groovy.lang.Closure
```

#### Jenkins Parameter Descriptions

Parameter descriptions for Jenkins, if you are interested in that kind of thing.

```
# RELEASE_BRANCH
##
The RELEASE_BRANCH where the supplied IMAGE will go for the project

# IMAGE_NAME
##
NOTE: This is not the whole image name! If the image built was 'dockerhub.io/my-submodule/my-image:my-image-version', the IMAGE_NAME would be equal to 'my-image'.

This does not currently work for images which are not part of an image group. ex: It will fail for "dockerhub.io/my-image:my-image-version"

# IMAGE_VERSION
##
The version the image is tagged with. ex: "0.1.0-rc1"
```

#### Calling from other jobs

Next, call this Jenkins job from another job, supplying the parameters from the other job.

1. For non-pipeline jobs, the [Parameterized Trigger Plugin](https://wiki.jenkins.io/display/JENKINS/Parameterized+Trigger+Plugin) can be used. Supply as parameters the 'BRANCH_NAME' being built, the 'IMAGE_NAME' (no version or path), and 'IMAGE_VERSION" to the job.
2. For pipeline jobs, it can be done rather simply, see this [Stackoverflow post](https://stackoverflow.com/a/39656390) for more details

```Groovy
stage ('Run Uprev job') {
    // No reason to run wait
    build job: 'RunArtInTest', wait: false, parameters: [
        [$class: 'StringParameterValue', name: 'RELEASE_BRANCH', value: RELEASE_BRANCH]
        [$class: 'StringParameterValue', name: 'IMAGE_VERSION', value: IMAGE_VERSION]
        [$class: 'StringParameterValue', name: 'IMAGE_NAME', value: IMAGE_NAME]
      ]
}
```

This job might take a few minutes to run the first time, when it is pulling the target repository. However, afterwards, the repository is cached on Jenkins and this script should only take `5-10` seconds to run per image.
