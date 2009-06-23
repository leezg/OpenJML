package tests;

public class QuerySecret extends TCBase {

    @Override
    public void setUp() throws Exception {
//        noCollectDiagnostics = true;
//        jmldebug = true;
        super.setUp();
    }
    
    public void testOK1() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Query(\"q\") int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    public void testBadParse() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Query(\"q\",\"r\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: annotation values must be of the form 'name=value'",10
                ,"/A.java:4: annotation values must be of the form 'name=value'",14
        );
    }
    
    public void testBadParse2() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Query(v=\"q\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: cannot find symbol\n  symbol:   method v()\n  location: interface org.jmlspecs.annotations.Query",10
        );
    }
    
    public void testBadParse3() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Query(9) int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: incompatible types\n  required: java.lang.String\n  found:    int",10
        );
    }
    
    public void testBadParse4() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Query(value=\"q\",value=\"r\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: duplicate annotation member value value in org.jmlspecs.annotations.Query",26
        );
    }
    
    public void testconstantExpresion() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Query(value=\"q\"+\"r\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: There is no model field or datagroup named qr in the class or its super types",19
        );
    }
    
    public void testOKnamed() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Query(value=\"q\") int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    public void testNotModel() { 
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  JMLDataGroup q;\n" +
                "  @Query(\"q\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: A datagroup must be declared model",10
        );
    }
    
    public void testSNotModel() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  JMLDataGroup q;\n" +
                "  @Secret(\"q\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: A datagroup must be declared model",11
        );
    }
    
    public void testOtherDeclOK() { 
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ model int q;\n" +
                "  @Query(\"q\") int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    public void testSOtherDeclOK() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ model int q;\n" +
                "  @Secret(\"q\") int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    /** Can be query for an inherited deata group */
    public void testOK2() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "class B { /*@ public model JMLDataGroup q; */ }\n" +
                "public class A extends B { \n" + 
                "  @Query(\"q\") int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    /** A named data group must exist */
    public void testNoDG() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //JMLDataGroup q;\n" +
                "  @Query(\"q\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: There is no model field or datagroup named q in the class or its super types",10
        );
    }
    
    /** A named data group may not be in an enclosing class */
    public void testNoDG2() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A {\n" +
                "  //@ public model JMLDataGroup q;\n" +
                "  public class X { \n" + 
                "    @Query(\"q\") int m() { return 0; } \n" +
                "  }\n" +
                "} \n"
                ,"/A.java:5: There is no model field or datagroup named q in the class or its super types",12
        );
    }
    
    /** A default existent datagroup */
    public void testOK3() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup m;\n" +
                "  @Query int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    /** A default non-existent datagroup */
    public void testOK4() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  @Query int m() { return 0; } \n" +
                "} \n"
        );
    }

    public void testSOK1() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Secret(\"q\") int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    public void testSBadParse() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Secret(\"q\",\"r\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: annotation values must be of the form 'name=value'",11
                ,"/A.java:4: annotation values must be of the form 'name=value'",15
        );
    }
    
    public void testSBadParse2() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Secret(v=\"q\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: cannot find symbol\n  symbol:   method v()\n  location: interface org.jmlspecs.annotations.Secret",11
        );
    }
    
    public void testSBadParse3() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Secret(9) int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: incompatible types\n  required: java.lang.String\n  found:    int",11
        );
    }
    
    public void testSBadParse4() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Secret(value=\"q\",value=\"r\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: duplicate annotation member value value in org.jmlspecs.annotations.Secret",27
        );
    }
    
   public void testSOKnamed() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Secret(value=\"q\") int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    /** Can be query for an inherited deata group */
    public void testSOK2() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "class B { /*@ public model JMLDataGroup q; */ }\n" +
                "public class A extends B { \n" + 
                "  @Secret(\"q\") int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    /** A named data group must exist */
    public void testSNoDG() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //JMLDataGroup q;\n" +
                "  @Secret(\"q\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: There is no model field or datagroup named q in the class or its super types",11
        );
    }
    
    /** A named data group may not be in an enclosing class */
    public void testSNoDG2() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A {\n" +
                "  //@ public model JMLDataGroup q;\n" +
                "  public class X { \n" + 
                "    @Secret(\"q\") int m() { return 0; } \n" +
                "  }\n" +
                "} \n"
                ,"/A.java:5: There is no model field or datagroup named q in the class or its super types",13
        );
    }
    
    /** A default existent datagroup */
    public void testSOK3() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup m;\n" +
                "  @Secret int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    /** A default non-existent datagroup */
    public void testSOK4() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  @Secret int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    /** Same datagroup */
    public void testSameDG() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  @Secret @Query int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:3: A method may not be both secret and query for the same datagroup",11
        );
    }
    
    /** Same datagroup */
    public void testSameDG1() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  @Secret @Query(\"m\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:3: There is no model field or datagroup named m in the class or its super types",18
        );
    }
    
    /** Same datagroup */
    public void testSameDG2() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  @Secret(\"m\") @Query int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:3: A method may not be both secret and query for the same datagroup",16
        );
    }
    
    /** Same datagroup */
    public void testSameDG3() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  @Secret(\"m\") @Query(\"m\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:3: There is no model field or datagroup named m in the class or its super types",23
                ,"/A.java:3: There is no model field or datagroup named m in the class or its super types",11
        );
    }
    
    /** Same datagroup */
    public void testSameDGOK() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Secret(\"q\") @Query int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    /** Same datagroup */
    public void testSameDGOK2() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Secret @Query(\"q\") int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    /** Same datagroup */
    public void testSameDGOK3() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q,r;\n" +
                "  @Secret(\"r\") @Query(\"q\") int m() { return 0; } \n" +
                "} \n"
        );
    }
    
    /** Same datagroup */
    public void testSameDG4() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Secret(\"q\") @Query(\"q\") int m() { return 0; } \n" +
                "} \n"
                ,"/A.java:4: A method may not be both secret and query for the same datagroup",16
        );
    }


    public void testFOK1() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public secret model JMLDataGroup q;\n" +
                "  @Secret int m; //@ in q; \n" +
                "} \n"
        );
    }
    
    /** Secret, but not in a datagroup */
    public void testFNotIn() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Secret int m;  \n" +
                "} \n"
                ,"/A.java:4: A secret field must be a model field or in a secret datagroup",15
        );
    }
    
    /** Secret, but not in a datagroup */
    public void testFInNonSecret() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  @Secret int m; //@ in q; \n" +
                "} \n"
                ,"/A.java:4: A datagroup for a secret field must be secret",22
        );
    }
    
    /** Not secret but in a secret datagroup */
    public void testFInSecret() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public secret model JMLDataGroup q;\n" +
                "  int m; //@ in q;  \n" +
                "} \n"
                ,"/A.java:4: A datagroup for a non-secret field must be non-secret",14
        );
    }
    
    /** Not secret but in a secret datagroup */
    public void testFInSecret2() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public secret model JMLDataGroup q;\n" +
                "  //@ model int m; //@ in q;  \n" +
                "} \n"
                ,"/A.java:4: A datagroup for a non-secret field must be non-secret",24
        );
    }
    
    /** OK - model fields are their own datagroups */
    public void testFNotInButModel() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  //@ public model JMLDataGroup q;\n" +
                "  //@ secret model int m;  \n" +
                "} \n"
        );
    }
    
    /** Valid argument, but not for a field */
    public void testFBadParse() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  @Secret(\"q\") int m; \n" +
                "} \n"
                ,"/A.java:3: A secret declaration for a field may not have arguments",11
                ,"/A.java:3: A secret field must be a model field or in a secret datagroup",20
        );
    }
    
    /** Invalid argument, also not for field */
    public void testFBadParse2() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  @Secret(v=\"q\") int m; \n" +
                "} \n"
                ,"/A.java:3: cannot find symbol\n  symbol:   method v()\n  location: interface org.jmlspecs.annotations.Secret",11
                ,"/A.java:3: A secret declaration for a field may not have arguments",13
                ,"/A.java:3: A secret field must be a model field or in a secret datagroup",22
        );
    }
    
    /** Invalid argument, aslo not for field */
    public void testFBadParse3() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  @Secret(9) int m; \n" +
                "} \n"
                ,"/A.java:3: incompatible types\n  required: java.lang.String\n  found:    int",11
                ,"/A.java:3: A secret declaration for a field may not have arguments",11
                ,"/A.java:3: A secret field must be a model field or in a secret datagroup",18
        );
    }
    
    /** Valid argument, but not for a field */
    public void testFBadParse4() {
        helpTCF("A.java",
                "import org.jmlspecs.annotations.*;\n" +
                "public class A { \n" + 
                "  @Secret(value=\"q\") int m; \n" +
                "} \n"
                ,"/A.java:3: A secret declaration for a field may not have arguments",17
                ,"/A.java:3: A secret field must be a model field or in a secret datagroup",26
        );
    }
    


}