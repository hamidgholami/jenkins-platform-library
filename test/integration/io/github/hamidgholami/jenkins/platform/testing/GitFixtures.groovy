/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

package io.github.hamidgholami.jenkins.platform.testing

import java.nio.charset.StandardCharsets

/**
 * Builds disposable bare histories from bytes; never stages or commits the user's repository.
 * */
final class GitFixtures {

    private final CommandRunner commands
    private final File fixtures
    String libraryCommit
    String sourceCommit
    String firstCommit

    GitFixtures(final CommandRunner commands, final File fixtures) {
        this.commands = commands
        this.fixtures = fixtures
    }

    void create(final File project) {
        fixtures.mkdirs()
        final File library = new File(fixtures, 'library.git')
        final File source = new File(fixtures, 'source.git')
        commands.execute(['git', 'init', '--bare', '--initial-branch=main', library.absolutePath])
        commands.execute(['git', 'init', '--bare', '--initial-branch=main', source.absolutePath])
        final Map<String, byte[]> files = new TreeMap<>()
        for (final String root : ['src', 'vars', 'resources']) {
            final File directory = new File(project, root)
            directory.eachFileRecurse { final File file ->
                if (file.isFile() && file.name != '.gitkeep') {
                    final String relative = project.toPath().relativize(file.toPath()).toString().replace('\\', '/')
                    files.put(relative, file.bytes)
                }
            }
        }
        importCommit(library, 'refs/heads/main', files, 1)
        libraryCommit = revision(library, 'main')
        for (int number = 1; number <= 5; number++) {
            importCommit(source, 'refs/heads/main', ['version.txt': ('version-' + number + '\n').getBytes(StandardCharsets.UTF_8)], number)
            if (number == 1) {
                firstCommit = revision(source, 'main')
                commands.execute(['git', '--git-dir=' + source.absolutePath, 'update-ref', 'refs/tags/v1', firstCommit])
            }
        }
        sourceCommit = revision(source, 'main')
        commands.execute(['git', '--git-dir=' + source.absolutePath, 'update-ref', 'refs/heads/feature/example', sourceCommit])
    }

    private void importCommit(final File repository, final String branch, final Map<String, byte[]> files,
                              final int number) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream()
        write(stream, 'commit ' + branch + '\ncommitter Fixture <fixture@example.invalid> ' + (1700000000 + number) + ' +0000\n')
        write(stream, 'data 7\nfixture\n')
        if (number > 1) {
            write(stream, 'from ' + branch + '^0\n')
        }
        for (final Map.Entry<String, byte[]> entry : files.entrySet()) {
            final String path = entry.key
            if (!path.matches('[A-Za-z0-9_./-]+')) {
                throw new IllegalArgumentException('Unsupported fixture path: ' + path)
            }
            write(stream, 'M 100644 inline ' + path + '\ndata ' + entry.value.length + '\n')
            stream.write(entry.value)
            write(stream, '\n')
        }
        write(stream, '\ndone\n')
        commands.execute(['git', '--git-dir=' + repository.absolutePath, 'fast-import', '--quiet'], stream.toByteArray())
    }

    private String revision(final File repository, final String ref) {
        return commands.execute(['git', '--git-dir=' + repository.absolutePath, 'rev-parse', ref])
    }

    private static void write(final ByteArrayOutputStream stream, final String value) {
        stream.write(value.getBytes(StandardCharsets.UTF_8))
    }
}
