import groovy.io.FileType
import groovy.io.FileVisitResult

//
// Automatically increase the version of an image in a project
//
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

  // Fetch all branches, in case there's a new one
  def fetch = ["git", "-C", projectDir, "fetch", release].execute()
  fetch.waitFor();

  // See if the branch exists, now that we've pulled
  // see: https://git-scm.com/docs/git-rev-parse
  def branchExists = ["git", "-C", projectDir, "rev-parse", "--verify", release].execute()
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

  def list = []
  def root = new File(projectDir)
  def imageVersions = new File(root.path + "/release/src.images")
  def regexp = /${image}:(.+)/
  def oldVersion = (imageVersions.text =~ regexp)[0][1]

  if (version == oldVersion) {
    println "Thankfully, no need to update the version for ${image} as we are already at version ${version} on ${release}"
    return 0;
  }

  println "release       :${release}"
  println "image         :${image}"
  println "version (new) :${version}"
  println "version (old) :${oldVersion}"

  final excludedDirs = ['.svn', '.git', '.hg', '.idea', 'node_modules', '.gradle', 'build']
  int count = 0

  // Get all files for now
  root.traverse(
          type                : FileType.FILES,
          preDir              : { if (it.name in excludedDirs) return FileVisitResult.SKIP_SUBTREE }, // excludes children of excluded dirs
          excludeNameFilter   : { it in excludedDirs }, // excludes the excluded dirs as well
          // nameFilter          : { it == 'settings.gradle'} // matched only given names
  ) {
    list << it
  }

  // For each file, replace the value of the image version
  def ant = new AntBuilder()
  list.each {
    println it.path

    // Replace the image everywhere
    ant.replaceregexp(file: it.path, match: "${image}:.+", replace: "${image}:${version}")
  }

  println "Moved ${image} to '${version}' from '${oldVersion}'"

  // Commit the change 
  ["git", "-C", projectDir, "add", "."].execute()
  ["git", "-C", projectDir, "commit", "-m", "${release}: [ROBO] ${image} to '${version}' from '${oldVersion}'"].execute()

  println "Committed changes to ${release}"

  // Push to remote
  ["git", "-C", projectDir, "push", "origin", release].execute()

  println "Pushed changes to ${release}"
}