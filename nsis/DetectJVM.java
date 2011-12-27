/**
 * DetectJVM.java
 * 
 * A tiny standalone program to report the whether the JVM accessible to a
 * Launch4j-generated executable on the installation target machine is 32-
 * or 64-bit.  This class is compiled to a jar and then Launch4j makes an
 * executable from it.  The word width of the JVM is returned as the exit
 * code, either 32 or 64.  An optional command-line entry of -print will
 * cause printing of what the program detects to standard output.  However,
 * the Launch4j executable is a pure windows (non-console) app, so won't
 * work with this option.
 *
 * @author Gene Ressler
 */
public class DetectJVM {
    // Add to this list any Java attribute that, if the key exists in global
    // properties, the value contains a 64 if and only if we are running a
    // 64-bit JVM, which therefore needs AMD64 format DLLs in Windoes.
    private static final String keys [] = {
        "sun.arch.data.model",
        "com.ibm.vm.bitmode",
        "os.arch",
    };

    public static void main (String [] args) {
        boolean print = args.length > 0 && "-print".equals(args[0]);
        for (String key : keys ) {
            String property = System.getProperty(key);
            if (print) {
                System.out.println(key + "=" + property);
            }
            if (property != null) {
                int errCode = (property.indexOf("64") >= 0) ? 64 : 32;
                if (print) {
                    System.out.println("err code=" + errCode);
                }
                System.exit(errCode);
            }
        }
    }
}
