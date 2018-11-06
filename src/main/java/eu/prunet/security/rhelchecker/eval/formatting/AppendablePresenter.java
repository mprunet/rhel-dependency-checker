package eu.prunet.security.rhelchecker.eval.formatting;

import java.io.IOException;

public class AppendablePresenter implements Presenter {
    private int depth;

    private Appendable appendable;
    public AppendablePresenter(Appendable appendable) {
        this.appendable =appendable;
    }

    @Override
    public Presenter indent() {
        depth++;
        return this;
    }

    @Override
    public void writeLine(boolean style, String line) throws IOException {
        for (int i = 0; i< depth; i++) {
            appendable.append("   ");
        }
        appendable.append(style + " : " + line +System.lineSeparator());
    }

    @Override
    public void close() {
        depth--;
    }
}
