package com.gigaspaces.newman;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Created by tamirs
 * on 9/3/15.
 */
public class Sha {

    public static String compute(String name, List<String> arguments) {
        try {
            MessageDigest cript = MessageDigest.getInstance("SHA-1");
            cript.reset();
            cript.update(name.getBytes("utf-8"));
            if (arguments != null) {
                for (String argument : arguments) {
                    cript.update(argument.getBytes("utf-8"));
                }
            }
            return new String(Base64.getEncoder().encode(cript.digest()));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void main(String[] args){
        System.out.println(compute("foo", Arrays.asList("arg1", "arg2")));
    }
}
