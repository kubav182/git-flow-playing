import java.util.*
import java.io.*

buildscript {
    repositories {
        // Use jcenter for resolving your dependencies.
        // You can declare any Maven/Ivy/file repository here.
        jcenter()
        mavenLocal()
    }

    dependencies {
        classpath("com.etherealscope:gradle-git-flow-version-plugin:1.0.0.RELEASE")
    }
}

plugins {
    // Apply the java plugin to add support for Java
    java

    // Apply the application plugin to add support for building an application
    application
}

apply(plugin = "com.etherealscope.gradlegitflowversionplugin")

tasks {

}

repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    mavenLocal()
}

dependencies {
    // This dependency is found on compile classpath of this component and consumers.
    implementation("com.google.guava:guava:27.0.1-jre")

    // Use JUnit test framework
    testImplementation("junit:junit:4.12")
}

application {
    // Define the main class for the application
    mainClassName = "git.flow.playing.App"
}


val versionPropertyFile = File(projectDir, "version.properties")
val versionProps = Properties()
versionProps.load(FileInputStream(versionPropertyFile))


fun getMajorVersion(): String {
    return versionProps.getProperty("app.version.major")
}

fun getMinorVersion(): String {
    return versionProps.getProperty("app.version.minor")
}

fun getPatchVersion(): String {
    return versionProps.getProperty("app.version.patch")
}

fun getStageVersion(): String {
    return versionProps.getProperty("app.version.stage")
}

fun getGitBranchType(): GitBranchType {
    val currentBranch = getCurrentGitBranch()
    if (currentBranch == null) {
        return GitBranchType.OTHER
    }
    for (item in GitBranchType.values()) {
        if (currentBranch.startsWith(item.prefix))
            return item
    }
    return GitBranchType.OTHER
}

fun getCurrentGitBranch(): String? {
    return "git rev-parse --abbrev-ref HEAD".runCommand()
}

fun getGitDiff(branch: String): String? {
    return "git log %s.. --oneline".format(branch).runCommand()
}

enum class GitBranchType(val prefix: String) {
    MASTER("master"),
    DEVELOP("develop"),
    RELEASE("release/"),
    FEATURE("feature/"),
    HOTFIX("hotfix/"),
    OTHER("")
}

enum class VersionScope {
    MAJOR, MINOR, PATCH
}

enum class VersionStage {
    RELEASE, RC, SNAPSHOT
}

fun String.runCommand(): String? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(File("./"))
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        val result = proc.inputStream.bufferedReader().readText()
        println(result)
        return result
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
}

fun hasUntrackedFiles(): Boolean {
    return "untracked".equals("git diff-index --quiet HEAD -- || echo 'untracked'".runCommand())
}

fun loadVersionProps() {
    versionProps.load(FileInputStream(versionPropertyFile))
}

fun changeVersion(scope: VersionScope?, stage: VersionStage) {
    when (scope) {
        VersionScope.MAJOR -> {
            versionProps["app.version.major"] = (versionProps.getProperty("app.version.major").toInt() + 1).toString()
            versionProps["app.version.minor"] = "0"
            versionProps["app.version.patch"] = "0"
        }
        VersionScope.MINOR -> {
            versionProps["app.version.minor"] = (versionProps.getProperty("app.version.minor").toInt() + 1).toString()
            versionProps["app.version.patch"] = "0"
        }
        VersionScope.PATCH -> versionProps["app.version.patch"] = (versionProps.getProperty("app.version.patch").toInt() + 1).toString()
    }
    when (stage) {
        VersionStage.RELEASE -> versionProps["app.version.stage"] = "RELEASE"
        VersionStage.RC -> versionProps["app.version.stage"] = "RC"
        VersionStage.SNAPSHOT -> versionProps["app.version.stage"] = "SNAPSHOT"
    }
    versionProps.store(FileOutputStream(versionPropertyFile), null)
    loadVersionProps()
    project.version = currentVersionString()
}

fun currentVersionString(): String {
    return (currentVersionWithoutStage()
            + "." + versionProps.getProperty("app.version.stage"))
}

fun currentVersionWithoutStage(): String {
    return (versionProps.getProperty("app.version.major")
            + "." + versionProps.getProperty("app.version.minor")
            + "." + versionProps.getProperty("app.version.patch"))
}

fun startRelease(scope: VersionScope) {
    val branchType = getGitBranchType()
    if (hasUntrackedFiles()) {
        throw IllegalStateException("Branch has untracked files")
    }
    if (branchType != GitBranchType.DEVELOP) {
        "git checkout develop".runCommand()
    }
    if (scope == VersionScope.MAJOR) {
        changeVersion(VersionScope.MAJOR, VersionStage.RC)
    } else {
        changeVersion(null, VersionStage.RC)
    }
    "git branch %s%s".format(GitBranchType.RELEASE.prefix, currentVersionWithoutStage()).runCommand()
    "git checkout %s%s".format(GitBranchType.RELEASE.prefix, currentVersionWithoutStage()).runCommand()
    "git push --set-upstream origin %s%s".format(GitBranchType.RELEASE.prefix, currentVersionWithoutStage()).runCommand()
    pushVersionProperties()
    "git checkout develop".runCommand()

    changeVersion(VersionScope.MINOR, VersionStage.SNAPSHOT)
    pushVersionProperties()
}

fun pushVersionProperties() {
    "git add version.properties".runCommand()
    "git commit -m version.json(%s)".format(currentVersionString()).runCommand()
    "git push".runCommand()
}

fun finishRelease() {

}

fun startHotfix() {
    val branchType = getGitBranchType()
    if (hasUntrackedFiles()) {
        throw IllegalStateException("Branch has untracked files")
    }
    if (branchType != GitBranchType.MASTER) {
        "git checkout master".runCommand()
    }
    changeVersion(VersionScope.PATCH, VersionStage.RC)
    "git branch %s%s".format(GitBranchType.HOTFIX.prefix, currentVersionWithoutStage()).runCommand()
    "git checkout %s%s".format(GitBranchType.HOTFIX.prefix, currentVersionWithoutStage()).runCommand()
    "git push --set-upstream origin %s%s".format(GitBranchType.HOTFIX.prefix, currentVersionWithoutStage()).runCommand()
    pushVersionProperties()
}

fun finishHotfix() {
    val branchType = getGitBranchType()
    if (branchType != GitBranchType.HOTFIX) {
        if (hasUntrackedFiles()) {
            throw IllegalStateException("Branch has untracked files")
        }

    }
}

fun startFeature(featureName: String) {
    val branchType = getGitBranchType()
    if (branchType != GitBranchType.DEVELOP) {
        if (hasUntrackedFiles()) {
            throw IllegalStateException("Branch has untracked files")
        }
        "git checkout develop".runCommand()
        "git checkout -b %s%s".format(GitBranchType.FEATURE.prefix, featureName).runCommand()
        "git push".runCommand()
    }
}

fun finishFeature() {

}

tasks.register("hello") {
    group = "Welcome"
    description = "Produces a greeting"

    doLast {
        //startRelease(VersionScope.MAJOR)
        //startRelease(VersionScope.MINOR)
        //startHotfix()
        //println("git diff-index HEAD".runCommand().isNullOrEmpty())

        println("1")
        val patch = "git format-patch master --stdout".runCommand()
        if (patch.isNullOrBlank()) {
            File("crazy.patch").writeText(patch)
            println("2")
            println("git checkout master".runCommand())
            println("3")
            println("git apply crazy.patch --check".runCommand())
            println("4")
            //println("rm crazy.patch".runCommand())
            println("5")
            println("git checkout develop".runCommand())
        }

    }
}