package eu.prunet.security.rhelchecker.eval;

import eu.prunet.security.rhelchecker.eval.formatting.Presenter;

import java.io.IOException;

public interface IEval {
    boolean isTrue();

    boolean isFalse();

    void print(Presenter pw)  throws IOException;
}
