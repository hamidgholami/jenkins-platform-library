// Copyright 2026 Hamid Gholami
// SPDX-License-Identifier: Apache-2.0

@Library('jenkins-platform-library')
import java.util.concurrent.TimeUnit

// Compilation fixture only; library retrieval requires real Jenkins.
return TimeUnit.MINUTES.name()
