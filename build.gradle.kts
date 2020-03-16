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

fun finishRelease() {

}

fun finishHotfix() {
}

fun finishFeature() {

}

tasks.register("hello") {
    group = "Welcome"
    description = "Produces a greeting"

    doLast {
        println("1")
        val patch = "git format-patch master --stdout".runCommand()
        if (!patch.isNullOrBlank()) {
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