# Copyright 2026 Hamid Gholami
# SPDX-License-Identifier: Apache-2.0

ARG JENKINS_IMAGE
FROM ${JENKINS_IMAGE}
COPY --chown=jenkins:jenkins plugins/ /usr/share/jenkins/ref/plugins/
ENV JAVA_OPTS="-Xms256m -Xmx768m -Djenkins.install.runSetupWizard=false"
ENV CASC_JENKINS_CONFIG=/tmp/jenkins.yaml
