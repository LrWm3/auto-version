
// Load in the standard file and test it here
// Must be called from root directory of this project
// cd auto-version
// groovy src/auto-version.groovy
GroovyShell shell = new GroovyShell()
def autoVersion = shell.parse(new File('src/auto-version.groovy'))

// Release, file and version we're updating for
// Hardcoded as an example here
def release="release-0.1.0"
def image="example-project"
def version="0.1.0-rc4"

// Call the parameterized method
autoVersion.autoVersion(release,image,version);
