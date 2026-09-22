package org.gravitywavetech.extended.jpa.util;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

public class Base58 {
    public static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int[] INDEXES = new int[128];

    public Base58() {
    }

    public static String encode(byte[] input) {
        if (input.length == 0) {
            return "";
        } else {
            input = copyOfRange(input, 0, input.length);

            int zeroCount;
            for(zeroCount = 0; zeroCount < input.length && input[zeroCount] == 0; ++zeroCount) {
            }

            byte[] temp = new byte[input.length * 2];
            int j = temp.length;

            byte mod;
            for(int startAt = zeroCount; startAt < input.length; temp[j] = (byte)ALPHABET[mod]) {
                mod = divmod58(input, startAt);
                if (input[startAt] == 0) {
                    ++startAt;
                }

                --j;
            }

            while(j < temp.length && temp[j] == ALPHABET[0]) {
                ++j;
            }

            while(true) {
                --zeroCount;
                if (zeroCount < 0) {
                    byte[] output = copyOfRange(temp, j, temp.length);

                    try {
                        return new String(output, "US-ASCII");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }

                --j;
                temp[j] = (byte)ALPHABET[0];
            }
        }
    }

    public static byte[] decode(String input) throws IllegalArgumentException {
        if (input.length() == 0) {
            return new byte[0];
        } else {
            byte[] input58 = new byte[input.length()];

            for(int i = 0; i < input.length(); ++i) {
                char c = input.charAt(i);
                int digit58 = -1;
                if (c >= 0 && c < 128) {
                    digit58 = INDEXES[c];
                }

                if (digit58 < 0) {
                    throw new IllegalArgumentException("Illegal character " + c + " at " + i);
                }

                input58[i] = (byte)digit58;
            }

            int zeroCount;
            for(zeroCount = 0; zeroCount < input58.length && input58[zeroCount] == 0; ++zeroCount) {
            }

            byte[] temp = new byte[input.length()];
            int j = temp.length;

            byte mod;
            for(int startAt = zeroCount; startAt < input58.length; temp[j] = mod) {
                mod = divmod256(input58, startAt);
                if (input58[startAt] == 0) {
                    ++startAt;
                }

                --j;
            }

            while(j < temp.length && temp[j] == 0) {
                ++j;
            }

            return copyOfRange(temp, j - zeroCount, temp.length);
        }
    }

    public static BigInteger decodeToBigInteger(String input) throws IllegalArgumentException {
        return new BigInteger(1, decode(input));
    }

    private static byte divmod58(byte[] number, int startAt) {
        int remainder = 0;

        for(int i = startAt; i < number.length; ++i) {
            int digit256 = number[i] & 255;
            int temp = remainder * 256 + digit256;
            number[i] = (byte)(temp / 58);
            remainder = temp % 58;
        }

        return (byte)remainder;
    }

    private static byte divmod256(byte[] number58, int startAt) {
        int remainder = 0;

        for(int i = startAt; i < number58.length; ++i) {
            int digit58 = number58[i] & 255;
            int temp = remainder * 58 + digit58;
            number58[i] = (byte)(temp / 256);
            remainder = temp % 256;
        }

        return (byte)remainder;
    }

    private static byte[] copyOfRange(byte[] source, int from, int to) {
        byte[] range = new byte[to - from];
        System.arraycopy(source, from, range, 0, range.length);
        return range;
    }

    public static void main(String[] args) {
        byte[] ss = decode("EKJMFEkWt6cdiBpatPooJ2");
        System.out.print(ss.length);
        System.out.println("---");
        System.out.println(-125);
        int[] data = new int[]{126, 176, 176, 217, 163, 97, 127, 73, 184, 234, 59, 19, 175, 27, 147, 233};
        byte[] real = new byte[16];

        for(int i = 0; i < data.length; ++i) {
            real[i] = (byte)(data[i] & 255);
        }

        System.out.println(encode(real));
        byte d = -125;
        System.out.println(d);
        String testString = "A2wFfue5wUWPbFctC4tw6Y";
        System.out.println(testString + "--->" + testString.getBytes().length);
        String encodedTestString = encode(testString.getBytes());
        System.out.println(encodedTestString + "--->" + encodedTestString.length());
        System.out.println(new String(decode(encodedTestString)));
        System.out.println("A2wFfue5wUWPbFctC4tw6Y");
        byte[] datasss = new byte[]{125, 2, 3, 4, 5};
        System.out.println(encode(datasss).getBytes().length);
    }

    static {
        for(int i = 0; i < INDEXES.length; ++i) {
            INDEXES[i] = -1;
        }

        for(int i = 0; i < ALPHABET.length; INDEXES[ALPHABET[i]] = i++) {
        }

    }
}

