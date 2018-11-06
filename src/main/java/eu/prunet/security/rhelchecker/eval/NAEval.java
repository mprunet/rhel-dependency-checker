package eu.prunet.security.rhelchecker.eval;

import eu.prunet.security.rhelchecker.eval.formatting.Presenter;

import java.io.IOException;

public class NAEval implements IEval {


    @Override
    public boolean isTrue() {
        return false;
    }

    @Override
    public boolean isFalse() {
        return false;
    }

    @Override
    public void print(Presenter pw)  throws IOException {
        pw.writeLine(false, "NA");
    }
}
