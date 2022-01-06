package test;

public class JavaClass {

    /**
     * Runs method 0 in {@link JavaClass}
     */
    public void method0() {
        /*
            Multiline comment
            transferred to another line
         */
    }

    /**
     * Runs method 1 in {@link JavaClass} with {@code param1}
     * as a String parameter
     *
     * @param param1 String parameter
     *               with a long description
     * @param param2
     * @return     {@code String}-typed object
     *         with a long description
     * @throws NullPointerException every time
     */
    public String method1(String param1, int param2) {

        // One-line comment
        throw new NullPointerException();
    }

    /**
     * @param param1 String parameter
     *               with a long description
     * @return A literal
     */
    public String method2(String param1) {

        // Short comment
        //     split into different
        // lines
        //

        return "Hello world"
    }

    public String method3() {

        // Comment with *emphasized words*
        return "null"
    }
}

// Comment after
// the whole class
