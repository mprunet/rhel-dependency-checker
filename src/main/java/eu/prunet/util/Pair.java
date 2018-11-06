package eu.prunet.util;

public class Pair<T,U> {
    private T one;
    private U two;

    private Pair(T one, U two) {
        this.one = one;
        this.two = two;
    }

    public T getOne() {
        return one;
    }

    public void setOne(T one) {
        this.one = one;
    }

    public U getTwo() {
        return two;
    }

    public void setTwo(U two) {
        this.two = two;
    }

    public static <T, U> Pair<T, U> of(T one, U two) {
        return new Pair<>(one, two);
    }
}
