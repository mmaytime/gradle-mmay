package org.gradle.kotlin.dsl.integration

import org.gradle.kotlin.dsl.concurrent.future
import org.gradle.kotlin.dsl.embeddedKotlinVersion

import org.gradle.kotlin.dsl.fixtures.AbstractIntegrationTest
import org.gradle.kotlin.dsl.fixtures.customInstallation
import org.gradle.kotlin.dsl.fixtures.matching
import org.gradle.kotlin.dsl.fixtures.withTestDaemon

import org.gradle.kotlin.dsl.resolver.GradleInstallation
import org.gradle.kotlin.dsl.resolver.KotlinBuildScriptModelRequest
import org.gradle.kotlin.dsl.resolver.fetchKotlinBuildScriptModelFor
import org.gradle.kotlin.dsl.tooling.models.KotlinBuildScriptModel

import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat

import java.io.File


/**
 * Base class for [KotlinBuildScriptModel] integration tests.
 */
abstract class ScriptModelIntegrationTest : AbstractIntegrationTest() {

    protected
    fun assertSourcePathIncludesGradleSourcesGiven(rootProjectScript: String, subProjectScript: String) {

        assertSourcePathGiven(
            rootProjectScript,
            subProjectScript,
            hasItems("core-api"))
    }

    protected
    fun assertSourcePathIncludesKotlinStdlibSourcesGiven(rootProjectScript: String, subProjectScript: String) {

        assertSourcePathGiven(
            rootProjectScript,
            subProjectScript,
            hasItems("kotlin-stdlib-jdk8-$embeddedKotlinVersion-sources.jar"))
    }

    protected
    fun assertSourcePathIncludesKotlinPluginSourcesGiven(rootProjectScript: String, subProjectScript: String) {

        assertSourcePathGiven(
            rootProjectScript,
            subProjectScript,
            hasItems(
                equalTo("kotlin-gradle-plugin-$embeddedKotlinVersion-sources.jar"),
                matching("annotations-[0-9.]+-sources\\.jar")))
    }

    private
    fun assertSourcePathGiven(
        rootProjectScript: String,
        subProjectScript: String,
        matches: Matcher<Iterable<String>>
    ) {

        val subProjectName = "sub"
        withSettings("include(\"$subProjectName\")")

        withBuildScript(rootProjectScript)
        val subProjectScriptFile = withBuildScriptIn(subProjectName, subProjectScript)

        assertThat(sourcePathFor(subProjectScriptFile).map { it.name }, matches)
    }

    protected
    fun sourcePathFor(scriptFile: File) =
        kotlinBuildScriptModelFor(projectRoot, scriptFile).sourcePath

    protected
    class ProjectSourceRoots(val projectDir: File, val sourceSets: List<String>, val languages: List<String>)

    protected
    fun withMainSourceSetJavaIn(projectDir: String) =
        ProjectSourceRoots(existing(projectDir), listOf("main"), listOf("java"))

    protected
    fun withMainSourceSetJavaKotlinIn(projectDir: String) =
        ProjectSourceRoots(existing(projectDir), listOf("main"), listOf("java", "kotlin"))

    protected
    fun matchesProjectsSourceRoots(vararg projectSourceRoots: ProjectSourceRoots): Matcher<Iterable<File>> {

        fun hasLanguageDir(base: File, set: String, lang: String): Matcher<Iterable<*>> =
            hasItem(base.resolve("src/$set/$lang"))

        return allOf(
            *projectSourceRoots
                .filter { it.languages.isNotEmpty() }
                .flatMap { sourceRoots ->
                    val languageDirs =
                        sourceRoots.sourceSets.flatMap { sourceSet ->
                            listOf("java", "kotlin").map { language ->
                                val hasLanguageDir = hasLanguageDir(sourceRoots.projectDir, sourceSet, language)
                                if (language in sourceRoots.languages) hasLanguageDir
                                else not(hasLanguageDir)
                            }
                        }

                    val resourceDirs =
                        sourceRoots.sourceSets.map { sourceSet ->
                            hasLanguageDir(sourceRoots.projectDir, sourceSet, "resources")
                        }

                    languageDirs + resourceDirs
                }.toTypedArray())
    }

    protected
    fun withMultiProjectKotlinBuildSrc(): Array<ProjectSourceRoots> {
        withFile("buildSrc/settings.gradle.kts", """include(":a", ":b", ":c")""")
        withFile("buildSrc/build.gradle.kts", """
            plugins {
                java
                `kotlin-dsl` apply false
            }

            val kotlinDslProjects = listOf(project.project(":a"), project.project(":b"))

            kotlinDslProjects.forEach {
                it.apply(plugin = "org.gradle.kotlin.kotlin-dsl")
            }

            dependencies {
                kotlinDslProjects.forEach {
                    "runtime"(project(it.path))
                }
            }
        """)
        withFile("buildSrc/b/build.gradle.kts", """dependencies { implementation(project(":c")) }""")
        withFile("buildSrc/c/build.gradle.kts", "plugins { java }")

        return arrayOf(
            withMainSourceSetJavaIn("buildSrc"),
            withMainSourceSetJavaKotlinIn("buildSrc/a"),
            withMainSourceSetJavaKotlinIn("buildSrc/b"),
            withMainSourceSetJavaIn("buildSrc/c"))
    }

    protected
    fun assertContainsGradleKotlinDslJars(classPath: List<File>) {
        val version = "[0-9.]+(-.+?)?"
        assertThat(
            classPath.map { it.name },
            hasItems(
                matching("gradle-kotlin-dsl-$version\\.jar"),
                matching("gradle-api-$version\\.jar"),
                matching("gradle-kotlin-dsl-extensions-$version\\.jar")))
    }

    protected
    fun assertClassPathFor(buildScript: File, includes: Set<File>, excludes: Set<File>) =
        assertThat(
            classPathFor(projectRoot, buildScript).map { it.name },
            allOf(
                hasItems(*includes.map { it.name }.toTypedArray()),
                not(hasItems(*excludes.map { it.name }.toTypedArray()))))

    protected
    fun assertClassPathContains(vararg files: File) =
        assertClassPathContains(canonicalClassPath(), *files)

    protected
    fun assertClassPathContains(classPath: List<File>, vararg files: File) =
        assertThat(
            classPath.map { it.name },
            hasItems(*fileNameSetOf(*files)))

    protected
    fun assertContainsBuildSrc(classPath: List<File>) =
        assertThat(
            classPath.map { it.name },
            hasItem("buildSrc.jar"))

    protected
    fun assertIncludes(classPath: List<File>, vararg files: File) =
        assertThat(
            classPath.map { it.name },
            hasItems(*fileNameSetOf(*files)))

    protected
    fun assertExcludes(classPath: List<File>, vararg files: File) =
        assertThat(
            classPath.map { it.name },
            not(hasItems(*fileNameSetOf(*files))))

    private
    fun fileNameSetOf(vararg files: File) =
        files.map { it.name }.toSet().toTypedArray().also {
            assert(it.size == files.size)
        }

    protected
    fun canonicalClassPath() =
        canonicalClassPathFor(projectRoot)
}


internal
fun canonicalClassPathFor(projectDir: File, scriptFile: File? = null) =
    classPathFor(projectDir, scriptFile).map(File::getCanonicalFile)


private
fun classPathFor(projectDir: File, scriptFile: File?) =
    kotlinBuildScriptModelFor(projectDir, scriptFile).classPath


internal
fun kotlinBuildScriptModelFor(projectDir: File, scriptFile: File? = null): KotlinBuildScriptModel =
    withTestDaemon {
        future {
            fetchKotlinBuildScriptModelFor(
                KotlinBuildScriptModelRequest(
                    projectDir = projectDir,
                    scriptFile = scriptFile,
                    gradleInstallation = customGradleInstallation(),
                    jvmOptions = listOf("-Xms128m", "-Xmx256m"))) {

                setStandardOutput(System.out)
                setStandardError(System.err)
            }
        }.get()
    }


internal
fun customGradleInstallation() =
    GradleInstallation.Local(customInstallation())
