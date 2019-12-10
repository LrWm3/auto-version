# auto-version

Automatically version [auto-version-target](https://github.com/WilliamTheMarsman/auto-version-target) when an image version is updated.

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
def autoVersion(release, image, version){
  // boring impl details
}

```

## Usage

See `auto-version.spec.groovy` for an example of this script being called.

> `auto-version.spec.groovy` must be run from project root. See below.

```bash
# Must be called from root directory of this project
cd auto-version
groovy src/auto-version.spec.groovy
```
> `auto-version.groovy` can be run from anywhere.

## Notes

1. Currently, the GIT_PROJECT, PROJECT_PARENT_DIRECTORY, and GIT_PROJECT_URL variables are all hardcoded; change as required for individual needs
1. If converted into a jenkins job, the stage this is run must have a [Pipeline Lock](https://jenkins.io/blog/2016/10/16/stage-lock-milestone/#lock) to avoid multiple jobs performing pushes at once. This shouldn't slow down build-times much, since the target project is meant to be shared among all jobs, and jobs do not hold the lock long if no action is taken.
1. release branch must already exist, or no action will be taken to uprev versions of images provided. create the release branch manually.
1. As a jenkins job, the branch name being built for the INDIVIDUAL project is supplied as the 'release' value provided to the `auto-version` function.
