package fr.brouillard.oss.gradle.plugins

import static org.assertj.core.api.Assertions.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.nio.file.*
import org.gradle.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Test that generates a multi-module Gradle project and runs the build to verify that jgitver is not loaded multiple times
 * 
 * IMPORTANT: does not run within IDE
 */
class JGitverPluginMultiModuleTest extends Specification {

  @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
  
  File buildFile

  def executeBashCommand(def baseFolder, def command) {
      def fullCommand = "cd $baseFolder && $command"
      println "Executing: $fullCommand"
      def p = ['/bin/bash', '-c', fullCommand].execute()
      def rc = p.waitFor()
      println p.text
      assert rc == 0
  }
  
  def setup() {
   def testProjectPath = testProjectDir.root.absolutePath
   println "Executing in ${testProjectPath}"
      
   // create git repo
   executeBashCommand(testProjectPath, "git init")
   executeBashCommand(testProjectPath, "git config user.name nobody")
   executeBashCommand(testProjectPath, "git config user.email nobody@nowhere.com")

    // create build.gradle file
    buildFile = testProjectDir.newFile('build.gradle')
    buildFile << """
plugins {
    id 'fr.brouillard.oss.gradle.jgitver'
}
"""
    // create multimodule build files
    File submodule1 = testProjectDir.newFolder("submodule1")
    File buildFile1 = new File(submodule1, 'build.gradle')
    buildFile1 << """
"""
    
    File submodule2 = testProjectDir.newFolder("submodule2")
    File buildFile2 = new File(submodule2, 'build.gradle')
    buildFile2 << """
"""
    // add files and commit
    executeBashCommand(testProjectPath, "git add -A")
    executeBashCommand(testProjectPath, "git commit -m A_initial_commit_master")

    // create settings file
    File settings = testProjectDir.newFile('settings.gradle')
    settings << """
include 'submodule1'
include 'submodule2'
"""
   
    // add files and commit (2)
    executeBashCommand(testProjectPath, "git add -A")
    executeBashCommand(testProjectPath, "git commit -m B_second_commit_master")

    // copy generated output for debugging, delete directory first if existing
    // FIXME: remove/disable
    Path dest = Paths.get("./build/test-git-repo");
    Path src = Paths.get(testProjectPath);
    if (Files.exists(dest)) {
        dest.toFile().deleteDir()
    }
    
    Files.walk(src).forEach {
        source -> Files.copy(source, dest.resolve(src.relativize(source)), REPLACE_EXISTING)
    };
  }

  def "print version"() {
    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('version', '--stacktrace', '--debug') // add --debug to get more output
        .forwardOutput()
        .withPluginClasspath()
        .build()

    then:
    println result.output
    result.output.contains("Version: 0.0.0-1")
    result.task(":version").outcome == TaskOutcome.SUCCESS
  }
}