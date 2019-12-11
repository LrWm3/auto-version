// Imports
import groovy.io.FileType
import groovy.io.FileVisitResult

node {
   stage('Preparation') { // for display purposes
        def projectParentDir=".."
        def projectDirName="auto-version-target"
        def projectDir="${projectParentDir}/${projectDirName}"
        def projectGitURL="https://github.com/WilliamTheMarsman/auto-version-target.git"
        def credentialsId='316ac40d-22d3-46c7-b138-cde567899405'

        // Get some code from a GitHub repository
        withCredentials([usernamePassword(credentialsId: "${credentialsId}", usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]){    
            sh('''
                git config --global credential.helper "!f() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; f"
            ''')
            autoVersion(projectParentDir,projectDirName, projectGitURL, RELEASE_BRANCH, IMAGE_NAME, IMAGE_VERSION)
        }
        
   }
}



// Wrapper around main function, contains HC values
@NonCPS
def autoVersion(release, image, version){
  def projectParentDir="/var/jenkins_home/workspace"
  def projectDirName="auto-version-target"
  def projectGitURL="https://github.com/WilliamTheMarsman/auto-version-target.git"
  
  return autoVersion(projectParentDir,projectDirName,projectGitURL,release,image,version)
}

// 
// run git command
//
// command - an array of commands
//
// returns {exitValue, sout, serr}
//
@NonCPS
def gitFunction(command) {
  def gitCommand = (["git"] + command).execute()
  def sout = new StringBuilder(), serr = new StringBuilder()
  gitCommand.consumeProcessOutput(sout, serr)

  gitCommand.waitFor()
  println "$sout"
  if (gitCommand.exitValue()) {
    println "Error encountered: $serr"
  }

  return [exitValue: gitCommand.exitValue(), sout: sout, serr: serr]
}

// 
// run git command against a specific project directory
//
// command - an array of commands
//
// returns {exitValue, sout, serr}
//
@NonCPS
def gitFunction(projectDir, command) {
  return gitFunction(["-C", projectDir] + command)
}

@NonCPS
def autoVersion(projectParentDir,projectDirName,projectGitURL,release,image,version){

  /// Constants
  def OK_STATUS=0
  def WARNING_STATUS=67
  def ERROR_STATUS=1
  def TARGET_FILE_WITH_IMAGE_VERSION="/release/src.images"
  def THICK_DELIM="==============================================================================="
  def THIN_DELIM="-------------------------------------------------------------------------------"

  def projectDir="${projectParentDir}/${projectDirName}"

  println THICK_DELIM
  println "Auto-Version paramters"
  println THICK_DELIM
  println "projectParentDir : ${projectParentDir}"
  println "projectDirName   : ${projectDirName}"
  println "projectDir       : ${projectDir}"
  println "projectGitURL    : ${projectGitURL}"
  println "release          : ${release}"
  println "image            : ${image}"
  println "version (new)    : ${version}"
  println THIN_DELIM

  println "Performing git operations to get a reference to the target project..."
  println THIN_DELIM

  // So far this will check out the project
  gitFunction(["clone", projectGitURL, projectDir])
  println "... cloned '${projectGitURL}' to '${projectDir}' ..."
  

  // Fetch all branches
  gitFunction(projectDir, ["fetch", "origin"])
  println "... fetching all branches for '${projectDirName}' ..."

  // See if the branch exists remotely
  // see: https://git-scm.com/docs/ls-remote
  def branchExists = gitFunction(projectDir, ["ls-remote", "--heads", "--exit-code", "origin", release])
  def sout = new StringBuilder(), serr = new StringBuilder()
  
  if (branchExists.exitValue) {
    println "Branch " + release + " does not exist, so group project requires no updates!"
    return OK_STATUS;
  }

  println "... found target release branch '${release}' ..."

  // Checkout the branch
  gitFunction(projectDir, ["checkout", release])
  println "... checked out release branch '${release}' ..."

  // Clean the branch
  gitFunction(projectDir, ["checkout", "--", "."])

  gitFunction(projectDir, ["clean", "-xfd", "."])
  println "... cleaned branch '${release}' ..."

  // Pull the latest for branch
  gitFunction(projectDir, ["pull", "origin", release])
  println "... pulled latest from release branch '${release}' ..."

  def projectRootPath = new File(projectDir)
  def imageVersions = new File(projectRootPath.path + TARGET_FILE_WITH_IMAGE_VERSION)

  // Assume all images are part of a larger group 'main-project/image:version'
  def regexp = /[\/]${image}:(.+)/
  def regexpResult = (imageVersions.text =~ regexp);

  // No image matching what was provided was found. This is probably not ok.
  if (regexpResult.size()==0){
    println "The image '$image' was not found in $TARGET_FILE_WITH_IMAGE_VERSION, it will be necessary to add it manually if the intent is to put it in this release."
    
    // Return WARNING flag?
    return WARNING_STATUS;
  }

  if (regexpResult.size()>1){
    println "The image '$image' was found $regexpResult.size() times in '$TARGET_FILE_WITH_IMAGE_VERSION', instead of only once, please check and see if this is an error!"

    println "Contents of $TARGET_FILE_WITH_IMAGE_VERSION"
    println THIN_DELIM
    println imageVersions.text
    println THIN_DELIM

    // Return WARNING flag?
    return TARGET_FILE_WITH_IMAGE_VERSION;
  }

  // We've made it through above, which means we have exactly one match to our regexp
  def oldVersion = (imageVersions.text =~ regexp)[0][1]


  // Print out the provided values. We don't have the old version before this point.
  println THIN_DELIM
  println "Done set-up git operations!"
  println THICK_DELIM
  println "Performing uprevision activities..."
  println THIN_DELIM
  println "... found old version '${oldVersion}' for '${image}' in '${TARGET_FILE_WITH_IMAGE_VERSION}' ..."

  // See if the version has changed
  if (version == oldVersion) {
    println "Thankfully, no need to update the version for ${image} as we are already at version ${version} on ${release}"
    return 0;
  }

  final excludedDirs = ['.svn', '.git', '.idea', 'node_modules', 'build']

  // Get all files for now
  def files = []
  projectRootPath.traverse(
          type                : FileType.FILES,
          preDir              : { if (it.name in excludedDirs) return FileVisitResult.SKIP_SUBTREE }, // excludes children of excluded dirs
          excludeNameFilter   : { it in excludedDirs }, // excludes the excluded dirs as well
          // nameFilter          : { it == 'settings.gradle'} // matched only given names
  ) {
    files << it
  }

  // For each file, replace the value of the image version
  def ant = new AntBuilder()

  println "... incrementing version in the following files ..."
  filesModified=0
  files.each {

    // Replace the image everywhere
    // Get modified times from file to see if the file has changed
    def lastModifiedPrevious=it.lastModified()
    def fileRegexp = ant.replaceregexp(file: it.path, match: "[/]${image}:.+", replace: "/${image}:${version}").toString()

    def lastModifiedNew=it.lastModified()

    // If last modified has changed, print the path
    if (lastModifiedPrevious!=lastModifiedNew){
      println "... > '${it.path}'"
      ++filesModified
    }
  }

  println THIN_DELIM
  println "Done moving ${image} to '${version}'! (from '${oldVersion}')"
  println THICK_DELIM
  println "Committing changes and pushing them to the remote project..."
  println THIN_DELIM

  // Commit the change 
  gitFunction(projectDir, ["add", "."])
  println "... added changes to local repository on '${release}' branch ... "

  gitFunction(projectDir, ["commit", "-m", "📦 '${image}:${version}' (from '${oldVersion}')"])
  println "... committed changes to local repository on '${release}' branch ... "

  // Push to remote
  gitFunction(projectDir, ["push", "origin", release])
  println "... pushed changes to remote repository on '${release}' branch ... "

  println THIN_DELIM
  println "Done pushing changes to the remote repository for '${release}'!"
  println THICK_DELIM
  println "Done auto-versioning"
  println THICK_DELIM
}
