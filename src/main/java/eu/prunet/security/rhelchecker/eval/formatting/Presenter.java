package eu.prunet.security.rhelchecker.eval.formatting;

import java.io.IOException;

public interface Presenter extends AutoCloseable {
    Presenter indent() throws IOException;
    void writeLine(boolean b, String line)  throws IOException;
    void close()  throws IOException;

}
