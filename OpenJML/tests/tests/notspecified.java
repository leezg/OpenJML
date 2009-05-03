package tests;

public class notspecified extends TCBase {


    @Override
    public void setUp() throws Exception {
//        noCollectDiagnostics = true;
//        jmldebug = true;
        super.setUp();
        options.put("-showNotImplemented", null);
    }

    public void testNotSpecified() {
        helpTCF("A.java","public class A { //@ requires \\not_specified; ensures \\not_specified; diverges \\not_specified; when \\not_specified; \n public void m(boolean i) {}}"
                );        // OK

    }
    
    public void testNotSpecified1() {
        helpTCF("A.java","public class A { //@ signals \\not_specified; \n public void m(boolean i) {}}"
                ,"/A.java:1: Expected a left parenthesis after a signals keyword",30
                ); 

    }
    
    public void testNotSpecified1s() {
        helpTCF("A.java","public class A { //@ signals (Exception) \\not_specified; \n public void m(boolean i) {}}"
                ); 

    }
    
    public void testNotSpecified1a() {
        helpTCF("A.java","public class A { //@ signals_only \\not_specified; \n public void m(boolean i) {}}"
                ,"/A.java:1: The type or expression near here is invalid (or not implemented): ( token \\not_specified in JmlParser.term3())",35
                ,"/A.java:1: Missing comma or otherwise ill-formed type name",35
                );        // Not allowed

    }
    
    public void testNotSpecified1b() {
        helpTCF("A.java","public class A { //@ assignable \\not_specified; assignable \\not_specified, \\not_specified; \n public void m(boolean i) {}}"
                );        // OK

    }
    
    public void testNotSpecified2() {
        helpTCF("A.java","public class A { //@ measured_by \\not_specified; working_space \\not_specified; duration \\not_specified; \n public void m(boolean i) {}}"
                );        // OK

    }
        
//    FIXME
    public void testNotSpecified3() {
        helpTCF("A.java","public class A { //@ measured_by \\not_specified if true; working_space \\not_specified if false ; duration \\not_specified if true; \n public void m(boolean i) {}}"
                );        // NOT OK

    }
    
    public void testNotSpecified4() {
        helpTCF("A.java","public class A { //@ callable \\not_specified; accessible \\not_specified; captures \\not_specified;  \n public void m(boolean i) {}}"
                );        // OK

    }
    

}
