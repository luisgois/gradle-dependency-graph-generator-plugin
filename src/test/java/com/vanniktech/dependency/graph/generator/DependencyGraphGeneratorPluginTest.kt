package com.vanniktech.dependency.graph.generator

import com.vanniktech.dependency.graph.generator.DependencyGraphGeneratorExtension.Generator.Companion.ALL
import org.assertj.core.api.Java6Assertions.assertThat
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.StringWriter

class DependencyGraphGeneratorPluginTest {
  @get:Rule val testProjectDir = TemporaryFolder()

  private lateinit var singleProject: DefaultProject

  @Before fun setUp() {
    // Casting this to DefaultProject so we can call evaluate later.
    singleProject = ProjectBuilder.builder().withName("single").build() as DefaultProject
    singleProject.plugins.apply(JavaLibraryPlugin::class.java)
    singleProject.repositories.run { add(mavenCentral()) }
    singleProject.dependencies.add("api", "org.jetbrains.kotlin:kotlin-stdlib:1.2.30")
    singleProject.dependencies.add("implementation", "io.reactivex.rxjava2:rxjava:2.1.10")
  }

  @Test fun taskProperties() {
    singleProject.plugins.apply(DependencyGraphGeneratorPlugin::class.java)

    singleProject.evaluate() // Need to call this for afterEvaluate() to pick up.

    val task = singleProject.tasks.getByName("generateDependencyGraph") as DependencyGraphGeneratorTask
    assertThat(task.generator).isSameAs(ALL)
    assertThat(task.group).isEqualTo("reporting")
    assertThat(task.description).isEqualTo("Generates a dependency graph")
    assertThat(task.inputFile).hasToString(singleProject.buildFile.toString())
    assertThat(task.outputFileDot).hasToString(File(singleProject.buildDir, "reports/dependency-graph/dependency-graph.dot").toString())
    assertThat(task.outputFileImage).hasToString(File(singleProject.buildDir, "reports/dependency-graph/dependency-graph.png").toString())
  }

  @Test fun integrationTestGradle46() {
    integrationTest("4.6")
  }

  @Test fun integrationTestGradle40() {
    integrationTest("4.0")
  }

  @Test fun integrationTestGradle33() {
    integrationTest("3.3")
  }

  private fun integrationTest(gradleVersion: String) {
    testProjectDir.newFile("build.gradle").writeText("""
        |plugins {
        |  id "java"
        |  id "com.vanniktech.dependency.graph.generator"
        |}
        |
        |repositories {
        |  mavenCentral()
        |}
        |
        |dependencies {
        |  compile "org.jetbrains.kotlin:kotlin-stdlib:1.2.30"
        |  compile "io.reactivex.rxjava2:rxjava:2.1.10"
        |}
        |""".trimMargin())

    val stdErrorWriter = StringWriter()

    GradleRunner.create()
        .withPluginClasspath()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.root)
        .withArguments("generateDependencyGraph")
        .forwardStdError(stdErrorWriter)
        .build()

    // No errors.
    assertThat(stdErrorWriter).hasToString("")

    // We don't want to assert the content of the image, just that it exists.
    assertThat(File(testProjectDir.root, "build/reports/dependency-graph/dependency-graph.png")).exists()

    assertThat(File(testProjectDir.root, "build/reports/dependency-graph/dependency-graph.dot")).hasContent("""
        digraph "G" {
        "${testProjectDir.root.name}" ["shape"="rectangle"]
        "kotlin-stdlib" ["shape"="rectangle"]
        "jetbrains-annotations" ["shape"="rectangle"]
        "rxjava" ["shape"="rectangle"]
        "reactive-streams" ["shape"="rectangle"]
        "${testProjectDir.root.name}" -> "kotlin-stdlib"
        "${testProjectDir.root.name}" -> "rxjava"
        "kotlin-stdlib" -> "jetbrains-annotations"
        "rxjava" -> "reactive-streams"
        }
        """.trimIndent())
  }

  @Test fun multiProjectIntegrationTest() {
    testProjectDir.newFile("build.gradle").writeText("""
        |plugins {
        |  id "com.vanniktech.dependency.graph.generator"
        |}
        |""".trimMargin())

    testProjectDir.newFile("settings.gradle").writeText("""
        |include ":lib"
        |include ":lib1"
        |include ":lib2"
        |include ":app"
        |""".trimMargin())

    testProjectDir.newFolder("lib")
    testProjectDir.newFile("lib/build.gradle").writeText("""
        |plugins { id "java-library" }
        |
        |repositories { mavenCentral() }
        |
        |dependencies {
        |  api "io.reactivex.rxjava2:rxjava:2.1.10"
        |}
        |""".trimMargin())

    testProjectDir.newFolder("lib1")
    testProjectDir.newFile("lib1/build.gradle").writeText("""
        |plugins { id "java-library" }
        |
        |repositories { mavenCentral() }
        |
        |dependencies {
        |  api project(":lib")
        |  implementation "org.jetbrains.kotlin:kotlin-stdlib:1.2.30"
        |}
        |""".trimMargin())

    testProjectDir.newFolder("lib2")
    testProjectDir.newFile("lib2/build.gradle").writeText("""
        |plugins { id "java-library" }
        |
        |repositories { mavenCentral() }
        |
        |dependencies {
        |  api project(":lib")
        |}
        |""".trimMargin())

    testProjectDir.newFolder("app")
    testProjectDir.newFile("app/build.gradle").writeText("""
        |plugins { id "java-library" }
        |
        |repositories { mavenCentral() }
        |
        |dependencies {
        |  implementation project(":lib1")
        |  implementation project(":lib2")
        |}
        |""".trimMargin())

    val stdErrorWriter = StringWriter()

    GradleRunner.create()
        .withPluginClasspath()
        .withGradleVersion("4.6")
        .withProjectDir(testProjectDir.root)
        .withArguments("generateDependencyGraph")
        .forwardStdError(stdErrorWriter)
        .build()

    // No errors.
    assertThat(stdErrorWriter).hasToString("")

    // We don't want to assert the content of the image, just that it exists.
    assertThat(File(testProjectDir.root, "build/reports/dependency-graph/dependency-graph.png")).exists()

    assertThat(File(testProjectDir.root, "build/reports/dependency-graph/dependency-graph.dot")).hasContent("""
        digraph "G" {
        "app" ["shape"="rectangle"]
        "lib1" ["shape"="rectangle"]
        "lib" ["shape"="rectangle"]
        "rxjava" ["shape"="rectangle"]
        "reactive-streams" ["shape"="rectangle"]
        "kotlin-stdlib" ["shape"="rectangle"]
        "jetbrains-annotations" ["shape"="rectangle"]
        "lib2" ["shape"="rectangle"]
        "app" -> "lib1"
        "app" -> "lib2"
        "lib1" -> "lib"
        "lib1" -> "kotlin-stdlib"
        "lib" -> "rxjava"
        "rxjava" -> "reactive-streams"
        "kotlin-stdlib" -> "jetbrains-annotations"
        "lib2" -> "lib"
        }
        """.trimIndent())
  }
}
