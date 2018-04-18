package com.vanniktech.dependency.graph.generator

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.vanniktech.dependency.graph.generator.DependencyGraphGeneratorExtension.Generator.Companion.ALL
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.attribute.Label.Justification.LEFT
import guru.nidi.graphviz.attribute.Label.Location.TOP
import guru.nidi.graphviz.attribute.Shape
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.model.MutableNode
import org.assertj.core.api.Java6Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Random

class DotGeneratorTest {
  private lateinit var singleEmpty: Project
  private lateinit var singleProject: Project
  private lateinit var multiProject: Project
  private lateinit var androidProject: DefaultProject // We always need to call evaluate() for Android Projects.
  private lateinit var androidProjectExtension: AppExtension

  @Before @Suppress("Detekt.LongMethod") fun setUp() {
    singleEmpty = ProjectBuilder.builder().withName("singleempty").build()
    singleEmpty.plugins.apply(JavaLibraryPlugin::class.java)
    singleEmpty.repositories.run { add(mavenCentral()) }

    singleProject = ProjectBuilder.builder().withName("single").build()
    singleProject.plugins.apply(JavaLibraryPlugin::class.java)
    singleProject.repositories.run { add(mavenCentral()) }
    singleProject.dependencies.add("api", "org.jetbrains.kotlin:kotlin-stdlib:1.2.30")
    singleProject.dependencies.add("implementation", "io.reactivex.rxjava2:rxjava:2.1.10")

    multiProject = ProjectBuilder.builder().withName("multi").build()

    val multiProject1 = ProjectBuilder.builder().withParent(multiProject).withName("multi1").build()
    multiProject1.plugins.apply(JavaLibraryPlugin::class.java)
    multiProject1.repositories.run { add(mavenCentral()) }
    multiProject1.dependencies.add("api", "org.jetbrains.kotlin:kotlin-stdlib:1.2.30")
    multiProject1.dependencies.add("implementation", "io.reactivex.rxjava2:rxjava:2.1.10")

    val multiProject2 = ProjectBuilder.builder().withParent(multiProject).withName("multi2").build()
    multiProject2.plugins.apply(JavaLibraryPlugin::class.java)
    multiProject2.repositories.run { add(mavenCentral()) }
    multiProject2.dependencies.add("implementation", "io.reactivex.rxjava2:rxjava:2.1.10")
    multiProject2.dependencies.add("implementation", "io.reactivex.rxjava2:rxandroid:2.0.2")

    androidProject = ProjectBuilder.builder().withName("android").build() as DefaultProject
    androidProject.plugins.apply(AppPlugin::class.java)
    androidProject.repositories.run {
      add(mavenCentral())
      add(google())
    }

    androidProjectExtension = androidProject.extensions.getByType(AppExtension::class.java)
    androidProjectExtension.compileSdkVersion(27)
    val manifestFile = File(androidProject.projectDir, "src/main/AndroidManifest.xml")
    manifestFile.parentFile.mkdirs()
    manifestFile.writeText("""<manifest package="com.foo.bar"/>""".trimIndent())
  }

  @Test fun singleProjectAllNoTestDependencies() {
    singleEmpty.dependencies.add("testImplementation", "junit:junit:4.12")

    assertThat(DotGenerator(singleEmpty, ALL).generateGraph()).hasToString("""
        digraph "G" {
        "singleempty" ["shape"="rectangle"]
        }
        """.trimIndent())
  }

  @Test fun singleProjectEmptyAllNoProjects() {
    assertThat(DotGenerator(singleEmpty, ALL.copy(includeProject = { false })).generateGraph()).hasToString("""
        digraph "G" {
        }
        """.trimIndent())
  }

  @Test fun singleProjectEmptyAll() {
    assertThat(DotGenerator(singleEmpty, ALL).generateGraph()).hasToString("""
        digraph "G" {
        "singleempty" ["shape"="rectangle"]
        }
        """.trimIndent())
  }

  @Test fun singleProjectEmptyAllHeader() {
    assertThat(DotGenerator(singleEmpty, ALL.copy(label = Label.of("my custom header").locate(TOP).justify(LEFT))).generateGraph()).hasToString("""
        digraph "G" {
        "labeljust"="l"
        "labelloc"="t"
        "label"="my custom header"
        "singleempty" ["shape"="rectangle"]
        }
        """.trimIndent())
  }

  @Test fun singleProjectEmptyAllRootFormatted() {
    assertThat(DotGenerator(singleEmpty, ALL.copy(projectNode = { node, _ -> node.add(Shape.EGG, Style.DOTTED, Color.rgb("ff0099")) })).generateGraph()).hasToString("""
        digraph "G" {
        "singleempty" ["shape"="egg","color"="#ff0099","style"="dotted"]
        }
        """.trimIndent())
  }

  @Test fun singleProjectAll() {
    assertThat(DotGenerator(singleProject, ALL).generateGraph()).hasToString("""
        digraph "G" {
        "single" ["shape"="rectangle"]
        "kotlin-stdlib" ["shape"="rectangle"]
        "jetbrains-annotations" ["shape"="rectangle"]
        "rxjava" ["shape"="rectangle"]
        "reactive-streams" ["shape"="rectangle"]
        "single" -> "kotlin-stdlib"
        "single" -> "rxjava"
        "kotlin-stdlib" -> "jetbrains-annotations"
        "rxjava" -> "reactive-streams"
        }
        """.trimIndent())
  }

  @Test fun singleProjectAllDependencyFormattingOptions() {
    // Generate a color for each dependency.
    val dependencyNode: (MutableNode, ResolvedDependency) -> MutableNode = { node, project ->
      val random = Random(project.hashCode().toLong())
      node.add(Style.FILLED, Color.hsv(random.nextDouble(), random.nextDouble(), random.nextDouble())
      )
    }

    assertThat(DotGenerator(singleProject, ALL.copy(dependencyNode = dependencyNode)).generateGraph()).hasToString("""
        digraph "G" {
        "single" ["shape"="rectangle"]
        "kotlin-stdlib" ["shape"="rectangle","color"="0.5729030306231915 0.730480096472168 0.6199754367027828","style"="filled"]
        "jetbrains-annotations" ["shape"="rectangle","color"="0.4288231821397507 0.6911813426492972 0.6290787664264184","style"="filled"]
        "rxjava" ["shape"="rectangle","color"="0.16317995652814232 0.937505295349677 0.3856775265969894","style"="filled"]
        "reactive-streams" ["shape"="rectangle","color"="0.7630981414446663 0.06104724686147023 0.3765458063358519","style"="filled"]
        "single" -> "kotlin-stdlib"
        "single" -> "rxjava"
        "kotlin-stdlib" -> "jetbrains-annotations"
        "rxjava" -> "reactive-streams"
        }
        """.trimIndent())
  }

  @Test fun singleProjectNoChildren() {
    assertThat(DotGenerator(singleProject, ALL.copy(children = { false })).generateGraph()).hasToString("""
        digraph "G" {
        "single" ["shape"="rectangle"]
        "kotlin-stdlib" ["shape"="rectangle"]
        "rxjava" ["shape"="rectangle"]
        "single" -> "kotlin-stdlib"
        "single" -> "rxjava"
        }
        """.trimIndent())
  }

  @Test fun singleProjectFilterRxJavaOut() {
    assertThat(DotGenerator(singleProject, ALL.copy(include = { it.moduleGroup != "io.reactivex.rxjava2" })).generateGraph()).hasToString("""
        digraph "G" {
        "single" ["shape"="rectangle"]
        "kotlin-stdlib" ["shape"="rectangle"]
        "jetbrains-annotations" ["shape"="rectangle"]
        "single" -> "kotlin-stdlib"
        "kotlin-stdlib" -> "jetbrains-annotations"
        }
        """.trimIndent())
  }

  @Test fun singleProjectNoDuplicateDependencyConnections() {
    // Both RxJava and RxAndroid point transitively on reactivestreams.
    singleProject.dependencies.add("implementation", "io.reactivex.rxjava2:rxandroid:2.0.2")

    assertThat(DotGenerator(singleProject, ALL).generateGraph()).hasToString("""
        digraph "G" {
        "single" ["shape"="rectangle"]
        "kotlin-stdlib" ["shape"="rectangle"]
        "jetbrains-annotations" ["shape"="rectangle"]
        "rxjava" ["shape"="rectangle"]
        "reactive-streams" ["shape"="rectangle"]
        "rxandroid" ["shape"="rectangle"]
        "single" -> "kotlin-stdlib"
        "single" -> "rxjava"
        "single" -> "rxandroid"
        "kotlin-stdlib" -> "jetbrains-annotations"
        "rxjava" -> "reactive-streams"
        "rxandroid" -> "rxjava"
        }
        """.trimIndent())
  }

  @Test fun multiProjectAll() {
    assertThat(DotGenerator(multiProject, ALL).generateGraph()).hasToString("""
        digraph "G" {
        "multi1" ["shape"="rectangle"]
        "kotlin-stdlib" ["shape"="rectangle"]
        "jetbrains-annotations" ["shape"="rectangle"]
        "rxjava" ["shape"="rectangle"]
        "reactive-streams" ["shape"="rectangle"]
        "multi2" ["shape"="rectangle"]
        "rxandroid" ["shape"="rectangle"]
        "multi1" -> "kotlin-stdlib"
        "multi1" -> "rxjava"
        "kotlin-stdlib" -> "jetbrains-annotations"
        "rxjava" -> "reactive-streams"
        "multi2" -> "rxjava"
        "multi2" -> "rxandroid"
        "rxandroid" -> "rxjava"
        }
        """.trimIndent())
  }

  @Test fun androidProjectArchitectureComponents() {
    androidProject.evaluate()

    androidProject.dependencies.add("implementation", "android.arch.persistence.room:runtime:1.0.0")

    assertThat(DotGenerator(androidProject, ALL).generateGraph()).hasToString("""
        digraph "G" {
        "android" ["shape"="rectangle"]
        "persistence-room-runtime" ["shape"="rectangle"]
        "persistence-room-common" ["shape"="rectangle"]
        "support-annotations" ["shape"="rectangle"]
        "persistence-db-framework" ["shape"="rectangle"]
        "persistence-db" ["shape"="rectangle"]
        "core-runtime" ["shape"="rectangle"]
        "core-common" ["shape"="rectangle"]
        "support-core-utils" ["shape"="rectangle"]
        "support-compat" ["shape"="rectangle"]
        "android" -> "persistence-room-runtime"
        "persistence-room-runtime" -> "persistence-room-common"
        "persistence-room-runtime" -> "persistence-db-framework"
        "persistence-room-runtime" -> "persistence-db"
        "persistence-room-runtime" -> "core-runtime"
        "persistence-room-runtime" -> "support-core-utils"
        "persistence-room-common" -> "support-annotations"
        "persistence-db-framework" -> "persistence-db"
        "persistence-db-framework" -> "support-annotations"
        "persistence-db" -> "support-annotations"
        "core-runtime" -> "core-common"
        "core-runtime" -> "support-annotations"
        "core-common" -> "support-annotations"
        "support-core-utils" -> "support-compat"
        "support-core-utils" -> "support-annotations"
        "support-compat" -> "support-annotations"
        }
        """.trimIndent())
  }

  @Test fun androidProjectSqlDelight() {
    androidProject.evaluate()

    androidProject.dependencies.add("implementation", "com.squareup.sqldelight:runtime:0.6.1")

    assertThat(DotGenerator(androidProject, ALL).generateGraph()).hasToString("""
        digraph "G" {
        "android" ["shape"="rectangle"]
        "sqldelight-runtime" ["shape"="rectangle"]
        "support-annotations" ["shape"="rectangle"]
        "android" -> "sqldelight-runtime"
        "sqldelight-runtime" -> "support-annotations"
        }
        """.trimIndent())
  }

  @Test fun androidProjectIncludeAllFlavorsByDefault() {
    androidProjectExtension.flavorDimensions("test")
    androidProjectExtension.productFlavors {
      it.create("flavor1").dimension = "test"
      it.create("flavor2").dimension = "test"
    }

    androidProject.evaluate()

    androidProject.dependencies.add("flavor1Implementation", "io.reactivex.rxjava2:rxandroid:2.0.2")
    androidProject.dependencies.add("flavor2DebugImplementation", "io.reactivex.rxjava2:rxjava:2.1.10")
    androidProject.dependencies.add("flavor2ReleaseImplementation", "org.jetbrains.kotlin:kotlin-stdlib:1.2.30")

    assertThat(DotGenerator(androidProject, ALL).generateGraph()).hasToString("""
        digraph "G" {
        "android" ["shape"="rectangle"]
        "rxandroid" ["shape"="rectangle"]
        "rxjava" ["shape"="rectangle"]
        "reactive-streams" ["shape"="rectangle"]
        "kotlin-stdlib" ["shape"="rectangle"]
        "jetbrains-annotations" ["shape"="rectangle"]
        "android" -> "rxandroid"
        "android" -> "rxjava"
        "android" -> "kotlin-stdlib"
        "rxandroid" -> "rxjava"
        "rxjava" -> "reactive-streams"
        "kotlin-stdlib" -> "jetbrains-annotations"
        }
        """.trimIndent())
  }

  @Test fun androidProjectIncludeAllBuildTypesByDefault() {
    androidProjectExtension.buildTypes {
      it.create("staging")
    }

    androidProject.evaluate()

    androidProject.dependencies.add("releaseImplementation", "io.reactivex.rxjava2:rxandroid:2.0.2")
    androidProject.dependencies.add("debugImplementation", "io.reactivex.rxjava2:rxjava:2.1.10")
    androidProject.dependencies.add("stagingImplementation", "org.jetbrains.kotlin:kotlin-stdlib:1.2.30")

    assertThat(DotGenerator(androidProject, ALL).generateGraph()).hasToString("""
        digraph "G" {
        "android" ["shape"="rectangle"]
        "rxjava" ["shape"="rectangle"]
        "reactive-streams" ["shape"="rectangle"]
        "rxandroid" ["shape"="rectangle"]
        "kotlin-stdlib" ["shape"="rectangle"]
        "jetbrains-annotations" ["shape"="rectangle"]
        "android" -> "rxjava"
        "android" -> "rxandroid"
        "android" -> "kotlin-stdlib"
        "rxjava" -> "reactive-streams"
        "rxandroid" -> "rxjava"
        "kotlin-stdlib" -> "jetbrains-annotations"
        }
        """.trimIndent())
  }

  @Test fun androidProjectIncludeOnlyStagingCompileClasspath() {
    androidProjectExtension.buildTypes {
      it.create("staging")
    }

    androidProject.evaluate()

    androidProject.dependencies.add("releaseImplementation", "io.reactivex.rxjava2:rxandroid:2.0.2")
    androidProject.dependencies.add("debugImplementation", "io.reactivex.rxjava2:rxjava:2.1.10")
    androidProject.dependencies.add("stagingImplementation", "org.jetbrains.kotlin:kotlin-stdlib:1.2.30")

    assertThat(DotGenerator(androidProject, ALL.copy(includeConfiguration = { it.name == "stagingCompileClasspath" })).generateGraph()).hasToString("""
        digraph "G" {
        "android" ["shape"="rectangle"]
        "kotlin-stdlib" ["shape"="rectangle"]
        "jetbrains-annotations" ["shape"="rectangle"]
        "android" -> "kotlin-stdlib"
        "kotlin-stdlib" -> "jetbrains-annotations"
        }
        """.trimIndent())
  }

  @Test fun androidProjectDoNotIncludeTestDependency() {
    androidProject.evaluate()

    androidProject.dependencies.add("testImplementation", "junit:junit:4.12")

    assertThat(DotGenerator(androidProject, ALL).generateGraph()).hasToString("""
        digraph "G" {
        "android" ["shape"="rectangle"]
        }
        """.trimIndent())
  }

  @Test fun androidProjectDoNotIncludeAndroidTestDependency() {
    androidProject.evaluate()

    androidProject.dependencies.add("androidTestImplementation", "junit:junit:4.12")

    assertThat(DotGenerator(androidProject, ALL).generateGraph()).hasToString("""
        digraph "G" {
        "android" ["shape"="rectangle"]
        }
        """.trimIndent())
  }
}
