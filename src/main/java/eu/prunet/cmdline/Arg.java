package eu.prunet.cmdline;

public class Arg {
    private String fullName;
    private Character name;
    private String value;

    private boolean mandatory;
    private String valueName;

    public Arg(String fullName, Character name, String valueName, boolean mandatory) {
        this.fullName = fullName;
        this.name = name;
        this.valueName = valueName;
        this.mandatory = mandatory;
    }

    public boolean isWithValue() {
        return valueName != null;
    }

    public String getFullName() {
        return fullName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Character getName() {
        return name;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean isMandatory() {
        return mandatory;
    }

}
