// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.sonarqube)
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("io.freefair.lombok") version "9.1.0"
}

sonar {
    properties {
        property("sonar.projectKey", "mbroglio_FastestLap")
        property("sonar.organization", "mbroglio")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.androidLint.reportPaths", "app/build/reports/lint-results-debug.xml")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "app/build/reports/coverage/test/debug/report.xml"
        )
    }
}