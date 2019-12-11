
// Load in the standard file and test it here
// Must be called from root directory of this project
// cd auto-version
// groovy src/auto-version.groovy
GroovyShell shell = new GroovyShell()
def autoVersion = shell.parse(new File('src/auto-version.groovy'))

// Release, file and version we're updating for
// Hardcoded as an example here
def projectParentDir=".."
def projectDirName="auto-version-target"
def projectGitURL="https://github.com/WilliamTheMarsman/auto-version-target.git"
def release="release-0.1.0"
def image="example-project"
def version="0.1.0-rc4"

// Call the parameterized method
autoVersion.autoVersion(release,image,version);

// Try with an image which does not exist
def imageNotExist="i-dont-exist"

// Call the parameterized method
autoVersion.autoVersion(release,imageNotExist,version);

checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "../${projectDirName}"]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'myId', url: projectGitURL]]])

// Release, file and version we're updating for
// Hardcoded as an example here
version="0.1.0-rc5"

// Call the parameterized method
autoVersion.autoVersion(release,image,version);

// Release, file and version we're updating for
// Hardcoded as an example here
version="0.1.0-rc4"

// Call the parameterized method
autoVersion.autoVersion(release,image,version);

////
// Test to see the fully-parameterized version works
////
projectParentDir="/Users/wmarsman/work/test-alt"
projectDirName="auto-version-target-test"
projectGitURL="https://github.com/WilliamTheMarsman/auto-version-target.git"
release="release-0.1.0"
image="example-project"
version="0.1.0-rc7"

autoVersion.autoVersion(projectParentDir,projectDirName,projectGitURL,release,image,version)