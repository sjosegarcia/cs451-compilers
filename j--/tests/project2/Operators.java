import java.lang.Integer;

public class Operators {
    public static void main(String[] args) {
        int a = Integer.parseInt(args[0]);
        int b = Integer.parseInt(args[1]);
        System.out.println(a != b);
        System.out.println(a /= b);
        System.out.println(a -= b);
        System.out.println(++a);
        System.out.println(b--);
        System.out.println(a *= b);
        System.out.println(a %= b);
        System.out.println(a >>= b);
        System.out.println(a >>>= b);
        System.out.println(a >= b);
        System.out.println(a <<= b);
        System.out.println(a < b);
        System.out.println(a ^= b);
        System.out.println(a |= b);
        System.out.println(a == b || b == a);
        System.out.println(a &= b);
    }
}
