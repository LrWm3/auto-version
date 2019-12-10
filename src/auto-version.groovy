import groovy.io.FileType
import groovy.io.FileVisitResult

//
// Automatically increase the version of an image in a project
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

  ///
  // May want to get location we should check out git projects to from an environment variable; HC for now
  ///
  // def env = System.getenv()
  // def projectParentDir = env['PROJECT_PARENT_DIR'] 
  ///

  def projectParentDir=".."
  def projectDirName="auto-version-target"
  def projectDir="${projectParentDir}/auto-version-target"
  def projectGitURL="https://github.com/WilliamTheMarsman/auto-version-target.git"

  // So far this will check out the project
  def clone = ["git", "-C", "..", "clone", projectGitURL, projectDir].execute()
  clone.waitFor();

  // Fetch all branches
  def fetch = ["git", "-C", projectDir, "fetch", release].execute()
  fetch.waitFor();

  // See if the branch exists remotely
  // see: https://git-scm.com/docs/ls-remote
  def branchExists = ["git", "-C", projectDir, "ls-remote", "--heads", "--exit-code", "origin", release].execute()
  branchExists.waitFor()
  if (branchExists.exitValue()) {
    println "Branch " + release + " does not exist, so group project requires no updates!"
  }

  // Checkout the branch
  def checkout = ["git", "-C", projectDir, "checkout", release].execute()
  checkout.waitFor()

  // Pull the latest for branch
  def pullLatest = ["git", "-C", projectDir, "pull"].execute()
  pullLatest.waitFor()

  def files = []
  def root = new File(projectDir)
  def imageVersions = new File(root.path + "/release/src.images")

  // Assume all images are part of a larger group 'main-project/image:version'
  def regexp = /[\/]${image}:(.+)/
  def oldVersion = (imageVersions.text =~ regexp)[0][1]

  if (version == oldVersion) {
    println "Thankfully, no need to update the version for ${image} as we are already at version ${version} on ${release}"
    return 0;
  }

  println "release       :${release}"
  println "image         :${image}"
  println "version (new) :${version}"
  println "version (old) :${oldVersion}"

  final excludedDirs = ['.svn', '.git', '.idea', 'node_modules', 'build']

  // Get all files for now
  root.traverse(
          type                : FileType.FILES,
          preDir              : { if (it.name in excludedDirs) return FileVisitResult.SKIP_SUBTREE }, // excludes children of excluded dirs
          excludeNameFilter   : { it in excludedDirs }, // excludes the excluded dirs as well
          // nameFilter          : { it == 'settings.gradle'} // matched only given names
  ) {
    files << it
  }

  // For each file, replace the value of the image version
  def ant = new AntBuilder()
  files.each {
    println it.path

    // Replace the image everywhere
    ant.replaceregexp(file: it.path, match: "[/]${image}:.+", replace: "/${image}:${version}")
  }

  println "Moved ${image} to '${version}' from '${oldVersion}'"

  // Commit the change 
  def gitAdd = ["git", "-C", projectDir, "add", "."].execute()
  gitAdd.waitFor();

  gitAdd.waitFor();
  def gitCommit = ["git", "-C", projectDir, "commit", "-m", "📦 '${image}:${version}' (from '${oldVersion}')"].execute()
  gitCommit.waitFor();

  println "Committed changes to ${release}"

  // Push to remote
  def gitPush = ["git", "-C", projectDir, "push", "origin", release].execute()
  gitPush.waitFor();

  println "Pushed changes to ${release}"
}


return this