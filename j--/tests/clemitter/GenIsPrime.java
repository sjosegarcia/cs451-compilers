import jminusminus.CLEmitter;
import static jminusminus.CLConstants.*;
import java.util.ArrayList;


public class GenIsPrime {

    public static void main(String[] args) {
        CLEmitter e = new CLEmitter(true);
        ArrayList<String> accessFlags = new ArrayList<String>();

        // IsPrime class
        accessFlags.add("public");
        e.addClass(accessFlags, "IsPrime", "java/lang/Object", null, true);
        accessFlags.clear();

        //Entry Point
        accessFlags.add("public");
        accessFlags.add("static");
        e.addMethod(accessFlags, "main", "([Ljava/lang/String;)V", null, true);

        //int n = Integer.parseInt(args[0]);
        e.addNoArgInstruction(ALOAD_0);
        e.addNoArgInstruction(ICONST_0);
        e.addNoArgInstruction(AALOAD);
        e.addMemberAccessInstruction(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I");
        e.addNoArgInstruction(ISTORE_1);

        //boolean result = isPrime(n);
        e.addNoArgInstruction(ILOAD_1);
        e.addMemberAccessInstruction(INVOKESTATIC, "IsPrime", "isPrime", "(I)Z");
        e.addNoArgInstruction(ISTORE_2);


        
        // Get System.out on stack
        e.addMemberAccessInstruction(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        
        // Create an intance (say sb) of StringBuffer on stack for string concatenations
        //    sb = new StringBuffer();
        e.addReferenceInstruction(NEW, "java/lang/StringBuffer");
        e.addNoArgInstruction(DUP);
        e.addMemberAccessInstruction(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "()V");

        //if ( result ) {
        e.addNoArgInstruction(ILOAD_2);
        e.addBranchInstruction(IFEQ, "notEqual");
        

        //System.out.println(n + " is a prime number ");
        e.addNoArgInstruction(ILOAD_1);
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
        "(I)Ljava/lang/StringBuffer;");

        e.addLDCInstruction(" is a prime number");
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuffer;");

        // System.out.println(sb.toString());
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/lang/StringBuffer",
        "toString", "()Ljava/lang/String;");
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        e.addBranchInstruction(GOTO, "prime");



        //System.out.println(n + " is not a prime number ");
        e.addLabel("notEqual");
        e.addNoArgInstruction(ILOAD_1);
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
        "(I)Ljava/lang/StringBuffer;");

        e.addLDCInstruction(" is not a prime number");
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuffer;");

        // System.out.println(sb.toString());
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/lang/StringBuffer",
        "toString", "()Ljava/lang/String;");
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");

        //return
        e.addLabel("prime");
        e.addNoArgInstruction(RETURN);
        accessFlags.clear();
        //end main

        //isPrime
        accessFlags.add("private");
        accessFlags.add("static");
        e.addMethod(accessFlags, "isPrime", "(I)Z", null, true);

        //if (n < 2)
        e.addNoArgInstruction(ILOAD_0);
        e.addNoArgInstruction(ICONST_2);
        e.addBranchInstruction(IF_ICMPLT, "isNotPrime");
        
        //for(int i = 2)
        e.addNoArgInstruction(ICONST_2);
        e.addNoArgInstruction(ISTORE_1);
        e.addLabel("forLoop");
        
        //This is the i <= n/i;
        // i
        e.addNoArgInstruction(ILOAD_1);
        // n / i
        e.addNoArgInstruction(ILOAD_0);
        e.addNoArgInstruction(ILOAD_1);
        e.addNoArgInstruction(IDIV);

        // <=
        e.addBranchInstruction(IF_ICMPLE, "isLessThan");
        e.addNoArgInstruction(ICONST_1);
        e.addNoArgInstruction(IRETURN);

    
        e.addLabel("isLessThan");
        //if (n % i == 0)
        e.addNoArgInstruction(ILOAD_0);
        e.addNoArgInstruction(ILOAD_1);
        e.addNoArgInstruction(IREM);
        e.addBranchInstruction(IFEQ, "isNotPrime");

        //if not equal to 0
        e.addIINCInstruction(1, 1);
        e.addBranchInstruction(GOTO, "forLoop");

        e.addLabel("isNotPrime");
        e.addNoArgInstruction(ICONST_0);
        e.addNoArgInstruction(IRETURN);

        accessFlags.clear();

        e.write();
    }
}
