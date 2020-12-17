import java.lang.Integer;
import java.lang.System;

public class ContinueStatement {
    public static void main(String[] args) {
        int n = Integer.parseInt(args[0]);
        double sum = 0.0;
        int sign = +1;
        for (int i = 0; i <= n; i++) {
            System.out.println(i);
            if (i % 2 == 0) {
                continue;
            }
            
            if (sign == +1) {
                sum += 1.0 / (double) i;
            } else {
                sum -= 1.0 / (double) i;
            }
            sign *= -1;
        }
        System.out.println(sum * 4.0);
    }
}
