////
// Automatically increase the version of an image in a project
////
// release : The release branch to target for the SUPERSET project
// image   : The image to target in the SUPERSET project
// version : The new version to uprev to in the SUPERSET project
////
// Notes
//   If SUPERSET project is not present, it is cloned
//   If version supplied equals the current version in the SUPERSET project, nothing is done
//   If release branch doesn't exist, nothing is done
//   Script assumes all images are part of a larger group: e.g. 'main-project/image:version'
////
import groovy.io.FileType
import groovy.io.FileVisitResult


def autoVersion(release, image, version){
  def projectParentDir=".."
  def projectDirName="auto-version-target"
  def projectGitURL="https://github.com/WilliamTheMarsman/auto-version-target.git"
  
  return autoVersion(projectParentDir,projectDirName,projectGitURL,release,image,version)
}

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
  def clone = ["git", "-C", "..", "clone", projectGitURL, projectDir].execute()
  clone.waitFor();
  println "... cloned '${projectGitURL}' to '${projectDir}' ..."
  

  // Fetch all branches
  def fetch = ["git", "-C", projectDir, "fetch", release].execute()
  fetch.waitFor();
  println "... fetching all branches for '${projectDirName}' ..."

  // See if the branch exists remotely
  // see: https://git-scm.com/docs/ls-remote
  def branchExists = ["git", "-C", projectDir, "ls-remote", "--heads", "--exit-code", "origin", release].execute()
  def sout = new StringBuilder(), serr = new StringBuilder()
  branchExists.consumeProcessOutput(sout, serr)

  branchExists.waitFor()
  if (branchExists.exitValue()) {
    println "Branch " + release + " does not exist, so group project requires no updates!"
    println "sout: $sout"
    println "serr: $serr"
    return OK_STATUS;
  }
  println "... found target release branch '${release}' ..."

  // Checkout the branch
  def checkout = ["git", "-C", projectDir, "checkout", release].execute()
  checkout.waitFor()
  println "... checking out release branch '${release}' ..."

  // Pull the latest for branch
  def pullLatest = ["git", "-C", projectDir, "pull"].execute()
  pullLatest.waitFor()
  println "... pulling latest from release branch '${release}' ..."

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
  def gitAdd = ["git", "-C", projectDir, "add", "."].execute()
  gitAdd.waitFor();
  println "... added changes to local repository on '${release}' branch ... "

  def gitCommit = ["git", "-C", projectDir, "commit", "-m", "📦 '${image}:${version}' (from '${oldVersion}')"].execute()
  gitCommit.waitFor();
  println "... committed changes to local repository on '${release}' branch ... "

  // Push to remote
  def gitPush = ["git", "-C", projectDir, "push", "origin", release].execute()
  gitPush.waitFor();
  println "... pushed changes to remote repository on '${release}' branch ... "

  println THIN_DELIM
  println "Done pushing changes to the remote repository for '${release}'!"
  println THICK_DELIM
  println "Done auto-versioning"
  println THICK_DELIM
}

// To allow importing in Jenkins, return this
return this