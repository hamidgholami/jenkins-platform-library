/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

@Library('jenkins-platform-library')
import io.github.hamidgholami.jenkins.platform.git.GitCheckoutOptions

node('integration') {
    gitUtils.checkout(GitCheckoutOptions.builder('http://git:8081/source.git')
            .branch('main').credentialsId('fixture-invalid').build())
}
