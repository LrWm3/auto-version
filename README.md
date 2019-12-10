# auto-version

Automatically version [auto-version-target](https://github.com/WilliamTheMarsman/auto-version-target) when an image version is updated.

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

1. If converted into a jenkins job, the stage this is run must have a [Pipeline Lock](https://jenkins.io/blog/2016/10/16/stage-lock-milestone/#lock) to avoid multiple jobs performing pushes at once. This shouldn't slow down build-times much, since the target project is meant to be shared among all jobs, and jobs do not hold the lock long if no action is taken.
1. release branch must already exist, or no action will be taken to uprev versions of images provided. create the release branch manually.
1. As a jenkins job, the branch being built should be the same as the 'release' value provided to the jenkins job.
