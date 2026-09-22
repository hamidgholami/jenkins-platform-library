import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.libs.LibraryRetriever

final class LocalLibraryRetriever extends LibraryRetriever {

    private final File sourceRoot

    LocalLibraryRetriever(final File sourceRoot) {
        this.sourceRoot = sourceRoot
    }

    @Override
    void retrieve(final String name, final String version, final boolean changelog,
                  final FilePath target, final Run<?, ?> run, final TaskListener listener) {
        new FilePath(sourceRoot).copyRecursiveTo(
                'src/**/*.groovy,vars/*.groovy,vars/*.txt,resources/**', null, target)
    }

    @Override
    void retrieve(final String name, final String version, final FilePath target,
                  final Run<?, ?> run, final TaskListener listener) {
        retrieve(name, version, false, target, run, listener)
    }
}
