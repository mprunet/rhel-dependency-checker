package eu.prunet.cmdline;

public class TranslatedException extends Exception {
    private ExitCode exitCode;

    public TranslatedException(ExitCode exitCode, String message, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public ExitCode getExitCode() {
        return exitCode;
    }
}
