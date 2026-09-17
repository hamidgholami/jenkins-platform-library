// Copyright 2026 Hamid Gholami
// SPDX-License-Identifier: Apache-2.0

rootProject.name = "jenkins-platform-library"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        maven("https://repo.jenkins-ci.org/public/") {
            name = "Jenkins"
            content {
                includeGroupByRegex("org\\.jenkins-ci(\\..*)?")
                includeGroupByRegex("io\\.jenkins(\\..*)?")
                includeGroup("com.cloudbees")
                includeGroup("com.lesfurets")
                includeGroup("org.kohsuke.stapler")
                includeGroup("org.kohsuke")
                includeGroup("org.jvnet")
                includeGroup("org.jvnet.hudson")
                includeGroup("org.jvnet.winp")
            }
        }
    }
}
