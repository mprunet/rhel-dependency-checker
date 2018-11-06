package eu.prunet.security.rhelchecker.rpm;

@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "SameParameterValue"})
class CharPointer {
    private final char[] c;
    private int idx;


    CharPointer(CharPointer cp) {
//            c = new char[cp.c.length];
//            System.arraycopy(cp.c, 0, c, 0, c.length);
        c = cp.c;
        idx = cp.idx;
    }

    CharPointer(String s) {
        c = new char[s.length() + 1];
        c[c.length - 1] = '\0';
        s.getChars(0, s.length(), c, 0);
        idx = 0;
    }

    boolean nEof() {
        return c[idx] != '\0';
    }

    boolean eof() {
        return c[idx] == '\0';
    }

    private static boolean rislower(char c)  {
        return (c >= 'a' && c <= 'z');
    }

    private static boolean risupper(char c)  {
        return (c >= 'A' && c <= 'Z');
    }

    private static boolean risalpha(char c)  {
        return (rislower(c) || risupper(c));
    }

    private static boolean risdigit(char c)  {
        return (c >= '0' && c <= '9');
    }

    private static boolean risalnum(char c)  {
        return (risalpha(c) || risdigit(c));
    }

    boolean risalnum() {
        return idx < c.length && risalnum(c[idx]);
    }

    boolean risalpha() {
        return idx < c.length && risalpha(c[idx]);
    }

    boolean risdigit() {
        return idx < c.length && risdigit(c[idx]);
    }

    boolean not(char c) {
        return c != this.c[idx];
    }

    boolean is(char c) {
        return c == this.c[idx];
    }

    void inc() {
        idx++;
    }

    void set(char c) {
        this.c[idx] = c;
    }

    char get() {
        return this.c[idx];
    }

    int strlen() {
        int size = 0;
        for (int i = idx; i < c.length; i++) {
            if (c[i] == '\0') {
                break;
            }
            size++;
        }
        return size;
    }

    public String toString() {
        int size = strlen();
        return new String(c, idx, size);
    }
}
