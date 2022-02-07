package com.dropbox.focus

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
public abstract class CreateFocusSettingsTask : DefaultTask() {

  @get:OutputFile
  public abstract val settingsFile: RegularFileProperty

  @TaskAction
  public fun createFocusSettings() {
    val dependencies = project.collectDependencies().sortedBy { it.path }

    settingsFile.get().asFile.writer().use { writer ->
      writer.write("// ${project.path} specific settings\n")
      writer.appendLine("//")
      writer.appendLine("// This file is autogenerated by the focus task. Changes will be overwritten.")
      writer.appendLine()

      // Add the includes statements
      dependencies.forEach { dep ->
        writer.appendLine("include(\"${dep.path}\")")
      }

      writer.appendLine()

      // Add overrides for projects with a root that's different from the gradle path
      dependencies
        .forEach { dep ->
          val gradleProjectPath = dep.path.substring(1).replace(":", "/")
          if (project.rootDir.resolve(gradleProjectPath) != dep.projectDir) {
            writer.appendLine("project(\"${dep.path}\").projectDir = new File(\"${dep.projectDir}\")")
          }
        }
    }
  }

  private fun Project.collectDependencies(excludes: Set<Project> = emptySet()): Set<Project> {
    return configurations.flatMap {
      it.dependencies
        .filterIsInstance<ProjectDependency>()
        .filter { it.dependencyProject != this }
        .flatMap { it.dependencyProject.collectDependencies(excludes) }
    }.toSet().plus(this)
  }

  public companion object {
    public operator fun invoke(): CreateFocusSettingsTask.() -> Unit = {
      group = FOCUS_TASK_GROUP
      settingsFile.set(project.layout.buildDirectory.file("focus.settings.gradle"))
    }
  }
}
