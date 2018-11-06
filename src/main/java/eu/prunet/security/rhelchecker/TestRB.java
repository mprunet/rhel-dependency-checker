package eu.prunet.security.rhelchecker;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class TestRB {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        SecureRandom sr = new SecureRandom();
        System.out.println(sr.getAlgorithm());
        SecureRandom.getInstanceStrong().nextBytes(new byte[1024]);
    }
}
