// Copyright 2026 Hamid Gholami
// SPDX-License-Identifier: Apache-2.0

// Jenkins supplies this import to consumers; reproduce it for offline compilation.
withConfig(configuration) {
    imports {
        normal('org.jenkinsci.plugins.workflow.libs.Library')
    }
}
