package com.palantir.gradle.dist.tasks

import com.palantir.gradle.dist.GradleTestSpec
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.BuildResult

class ConfigTarTaskTest extends GradleTestSpec {

    def 'configTar task exists for services'() {
        setup:
        createUntarBuildFile(buildFile, "java-service", "service", "foo-service")

        when:
        BuildResult buildResult = run(':configTar').build()

        then:
        buildResult.task(':configTar').outcome == TaskOutcome.SUCCESS
    }

    def 'configTar task exists for assets'() {
        setup:
        createUntarBuildFile(buildFile, "asset", "asset", "foo-asset")

        when:
        BuildResult buildResult = run(':configTar').build()

        then:
        buildResult.task(':configTar').outcome == TaskOutcome.SUCCESS
    }

    def 'configTar task contains the necessary deployment files for services'() {
        setup:
        createUntarBuildFile(buildFile, "java-service", "service", "foo-service")

        when:
        run(':configTar', ':untar').build()

        then:
        def files = directory('dist/foo-service-0.0.1/', projectDir).list()
        files.length == 1
        files.contains('deployment')
        def manifest = file('dist/foo-service-0.0.1/deployment/manifest.yml', projectDir).text
        manifest.contains('service.v1')
    }

    def 'configTar task contains the necessary deployment files for assets'() {
        setup:
        createUntarBuildFile(buildFile, "asset", "asset", "foo-asset")

        when:
        run(':configTar', ':untar').build()

        then:
        def files = directory('dist/foo-asset-0.0.1/', projectDir).list()
        files.length == 1
        files.contains('deployment')
        def manifest = file('dist/foo-asset-0.0.1/deployment/manifest.yml', projectDir).text
        manifest.contains('asset.v1')
    }

    def 'configTar task fails for invalid product types'() {
        when:
        Project project = ProjectBuilder.builder().withName("foo").build()
        ConfigTarTask.createConfigTarTask(project, "configTar", "foo.bar")

        then:
        def err = thrown(IllegalArgumentException)
        err.message.contains("Product type must end with")
    }

    private static createUntarBuildFile(buildFile, pluginType, artifactType, name) {
        buildFile << """
            plugins {
                id 'com.palantir.sls-${pluginType}-distribution'
            }
            
            distribution {
                serviceName '${name}'
            }

            version "0.0.1"
            project.group = 'service-group'

            // most convenient way to untar the dist is to use gradle
            task untar (type: Copy) {
                from tarTree(resources.gzip("\${buildDir}/distributions/${name}-0.0.1.${artifactType}.config.tgz"))
                into "\${projectDir}/dist"
                dependsOn configTar
            }
        """.stripIndent()
    }
}
