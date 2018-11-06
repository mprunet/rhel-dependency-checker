package eu.prunet.cmdline;

import java.util.*;

public class ArgParser {
    private Map<Object, Arg> optionalArgs = new HashMap<>();
    private Set<Arg> argProvided = new HashSet<>();
    private List<Arg> argList = new ArrayList<>();

    public Arg optionalWithValue(String fullName, Character name, String valueName) {
        Arg arg = new Arg(fullName, name, valueName, false);
        if (fullName != null) optionalArgs.put(fullName, arg);
        if (name != null) optionalArgs.put(name, arg);
        return arg;
    }

    public Arg optional(String fullName, Character name) {
        Arg arg = new Arg(fullName, name, null,  false);
        if (fullName != null) optionalArgs.put(fullName, arg);
        if (name != null) optionalArgs.put(name, arg);
        return arg;
    }

    public Arg optionalWithDefaultValue(String fullName, Character name, String valueName, String defaultValue) {
        Arg arg = new Arg(fullName, name, valueName, true);
        if (fullName != null) optionalArgs.put(fullName, arg);
        if (name != null) optionalArgs.put(name, arg);
        arg.setValue(defaultValue);
        argProvided.add(arg);
        return arg;
    }

    public Arg arg(String name, boolean mandatory) {
        Arg arg = new Arg(name, null, null, mandatory);
        argList.add(arg);
        return arg;
    }


    private Arg addArgValue(Object argName, String argValue, boolean errorIfValueSupplied) throws BadArgumentException{
        Arg argA = Optional.ofNullable(optionalArgs.get(argName))
                .orElseThrow(() -> new BadArgumentException("Argument " + argName + " is not allowed "));
        if (argA.isWithValue()) {
            if (argValue == null) {
                throw new BadArgumentException("Argument " + argName + " must provide a value option " + argA.getValueName());
            } else {
                argA.setValue(argValue);
            }
        } else if (errorIfValueSupplied && argValue != null){
            throw new BadArgumentException("Argument " + argName + " mustn't provide a value");
        }
        argProvided.add(argA);
        return argA;
    }

    public Set<Arg> parse(String[] args) throws BadArgumentException {
        Iterator<Arg> ite = argList.iterator();
        boolean optionAllowed = true;
        for (int i = 0; i< args.length; i++) {
            if ("-h".equals(args[i]) || "--help".equals(args[i])) {
                return new HashSet<>();
            }
            if ("--".equals(args[i])) {
                optionAllowed = false;
            } else if (optionAllowed && args[i].startsWith("--")) {
                String key, value = null;
                int idx = args[i].indexOf('=');
                if (idx != -1) {
                    key = args[i].substring(2, idx);
                    value = args[i].substring(idx + 1);
                } else {
                    key = args[i].substring(2);
                }
                addArgValue(key, value, true);

            } else if (optionAllowed && args[i].startsWith("-")) {
                final char[] argsC = args[i].toCharArray();
                if (argsC.length > 2) {
                    for (int j = 1; j < argsC.length; j++) {
                        addArgValue(argsC[j], null, true);
                    }
                } else {
                    String value = null;
                    if (i + 1 < args.length) {
                        value = args[i+1];
                    }
                    Arg argA = addArgValue(argsC[1], value, false);
                    if (argA.getValue() != null){
                        i++;
                    }
                }
            } else {
                if (ite.hasNext()) {
                    Arg arg = ite.next();
                    arg.setValue(args[i]);
                    ite.remove();
                    argProvided.add(arg);
                }
            }
        }
        while (ite.hasNext()) {
            Arg arg = ite.next();
            if (arg.isMandatory()) {
                throw new BadArgumentException("Argument " + arg.getFullName() + " is mandatory");
            }
        }
        return argProvided;
    }

}
