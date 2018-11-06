package eu.prunet.cmdline;

import java.nio.file.Path;

public class DownloadManuallyOvalException extends Exception {
    private Path ovalFile;
    public DownloadManuallyOvalException(Path ovalFile) {
        this.ovalFile = ovalFile;
    }

    public Path getOvalFile() {
        return ovalFile;
    }
}
