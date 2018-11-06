package eu.prunet.security.rhelchecker.eval;

import eu.prunet.schema.oval.common.OperatorEnumeration;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

public class LogicOperation {
    public static <T> IEval evaluate(OperatorEnumeration operator, boolean negate, Stream<T> stream, Function<T, IEval> evaluator) {
        IEval ret;
        switch (operator) {
            case OR:
                ret = or(stream, evaluator, negate);
                break;
            case ONE:
                ret = one(stream, evaluator, negate);
                break;
            case XOR:
                ret = xor(stream, evaluator, negate);
                break;
            case AND:
            default: // null
                ret = and(stream, evaluator, negate);

        }
        return ret;
    }

    private static <T> IEval or(Stream<T>s, Function<T, IEval> evaluator, boolean negate) {
        EvalStack stack = new EvalStack("OR");
        Iterator<T> ite = s.iterator();
        IEval val;
        while (ite.hasNext()) {
            val = evaluator.apply(ite.next());
            stack.add(val);
            if (val.isTrue()) {
                stack.setTrue();
                break;
            } else if (val.isFalse()) {
                stack.setFalse();
            }
        }
        stack.negate(negate);
        return stack;
    }

    private static <T> IEval and(Stream<T>s, Function<T, IEval> evaluator, boolean negate) {
        EvalStack stack = new EvalStack("AND");
        Iterator<T> ite = s.iterator();
        IEval val = null;
        int nb = 0;
        while (ite.hasNext()) {
            val = evaluator.apply(ite.next());
            stack.add(val);
            if (val.isTrue()) {
                stack.setTrue();
            } else {
                stack.setFalse();
                break;
            }
            nb++;
        }
        stack.negate(negate);
        if (nb == 1 && stack.isTrue() && !negate) {
            return val; // Reduce stack size for better lisibility
        }
        return stack;
    }

    private static <T> IEval one(Stream<T>s, Function<T, IEval> evaluator, boolean negate) {
        EvalStack stack = new EvalStack("ONE");
        Iterator<T> ite = s.iterator();
        IEval val;
        int nbT = 0;
        while (ite.hasNext()) {
            val = evaluator.apply(ite.next());
            stack.add(val);
            if (val.isTrue()) {
                if (nbT > 1) {
                    stack.setFalse();
                    break;
                }
                nbT++;
            }
        }
        if (nbT == 1) {
            stack.setTrue();
        }
        stack.negate(negate);
        return stack;
    }

    private static <T> IEval xor(Stream<T>s, Function<T, IEval> evaluator, boolean negate) {
        EvalStack stack = new EvalStack("XOR");
        Iterator<T> ite = s.iterator();
        IEval val;
        int nbT = 0;
        while (ite.hasNext()) {
            val = evaluator.apply(ite.next());
            stack.add(val);
            if (val.isTrue()) {
                nbT++;
            }
        }
        if ((nbT % 2) == 0) {
            stack.setTrue();
        }
        stack.negate(negate);
        return stack;
    }

}
