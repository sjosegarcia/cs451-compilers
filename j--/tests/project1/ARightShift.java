import java.lang.Integer;
import java.lang.System;

public class ARightShift {
    public static void main(String[] args) {
        int a = Integer.parseInt(args[0]);
        int b = Integer.parseInt(args[1]);
        int c = a >> b;
        System.out.println(Integer.toBinaryString(c));
    }
}
