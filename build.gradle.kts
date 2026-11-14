// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.sonarqube)
}

sonar {
    properties {
        property("sonar.projectKey", "mbroglio_FastestLap")
        property("sonar.organization", "mbroglio")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

project(":app") {
    extensions.configure<org.sonarqube.gradle.SonarExtension> {
        properties {
            property(
                "sonar.androidLint.reportPaths",
                layout.buildDirectory.file("reports/lint-results-debug.xml").get().asFile.absolutePath
            )
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                layout.buildDirectory.file("reports/coverage/test/debug/report.xml").get().asFile.absolutePath
            )
        }
    }
}
