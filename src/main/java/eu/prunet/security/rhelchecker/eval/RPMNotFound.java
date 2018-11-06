package eu.prunet.security.rhelchecker.eval;

import eu.prunet.security.rhelchecker.eval.formatting.Presenter;

import java.io.IOException;

public class RPMNotFound implements IEval {
    private final String packageName;

    public RPMNotFound(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

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
        pw.writeLine(false, packageName + " is not installed on the system");
    }
}
