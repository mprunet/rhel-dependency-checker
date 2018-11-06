package eu.prunet.security.rhelchecker.eval;

import eu.prunet.security.rhelchecker.eval.formatting.Presenter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EvalStack implements IEval {
    private String operation;
    private Boolean operationResult;
    private final List<IEval> subStack = new ArrayList<>();

    public EvalStack(String operation) {
        this.operation = operation;
    }

    public void add(IEval eval) {
        subStack.add(eval);
    }

    public boolean isTrue(){
        return operationResult == Boolean.TRUE;
    }

    public boolean isFalse(){
        return operationResult == Boolean.FALSE;
    }

    @Override
    public void print(Presenter pw) throws IOException {
        pw.writeLine(operationResult, operation);
        try (Presenter p = pw.indent()) {
            for (IEval e : subStack) {
                e.print(p);
            }
        }
    }

    public void setTrue() {
        this.operationResult = Boolean.TRUE;
    }

    public void setFalse() {
        this.operationResult = Boolean.FALSE;
    }

    public void negate(boolean negate) {
        if (negate) {
            operation = "!" + operation;
            if (operationResult != null) {
                this.operationResult = !operationResult;
            }
        }
    }

}
