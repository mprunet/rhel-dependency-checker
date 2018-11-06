package eu.prunet.cmdline;

import java.util.Collections;
import java.util.List;

public class BadRpmInputException extends Exception {
    private List<String> errors;
    private String fileName;

    public BadRpmInputException(String fileName, List<String> errors) {
        this.fileName = fileName;
        this.errors = errors;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public String getFileName() {
        return fileName;
    }
}
