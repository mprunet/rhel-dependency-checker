package eu.prunet.security.rhelchecker.eval;

import eu.prunet.security.rhelchecker.eval.formatting.Presenter;

import java.io.IOException;

public class EndEval implements IEval {
    private Boolean operationResult;
    private String packageName;

    public String getPackageName() {
        return packageName;
    }

    public String getEvalType() {
        return evalType;
    }

    public void setEvalType(String evalType) {
        this.evalType = evalType;
    }

    private String evalType;
    private String debug;

    public void setOperationResult(boolean operationResult) {
        this.operationResult = operationResult;
    }
    public boolean isTrue(){
        return operationResult == Boolean.TRUE;
    }

    public boolean isFalse(){
        return operationResult == Boolean.FALSE;
    }

    @Override
    public void print(Presenter pw) throws IOException {
        pw.writeLine(operationResult, debug);
    }

    public void debug(String s, Object ...args) {
        debug = String.format(s, args);
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
