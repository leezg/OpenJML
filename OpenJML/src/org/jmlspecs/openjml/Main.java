package org.jmlspecs.openjml;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import org.jmlspecs.annotations.*;
import org.jmlspecs.openjml.JmlSpecs.FieldSpecs;
import org.jmlspecs.openjml.JmlSpecs.TypeSpecs;
import org.jmlspecs.openjml.JmlTree.JmlClassDecl;
import org.jmlspecs.openjml.JmlTree.JmlCompilationUnit;
import org.jmlspecs.openjml.JmlTree.JmlMethodSpecs;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.comp.JmlAttr;
import com.sun.tools.javac.comp.JmlCheck;
import com.sun.tools.javac.comp.JmlEnter;
import com.sun.tools.javac.comp.JmlFlow;
import com.sun.tools.javac.comp.JmlMemberEnter;
import com.sun.tools.javac.comp.JmlResolve;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.CommandLine;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.JavaCompiler.CompileState;
import com.sun.tools.javac.parser.JmlParser;
import com.sun.tools.javac.parser.JmlScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JavacMessages;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;

/**
 * This class is the main entry point for the JML tool. It uses the javac
 * compiler, but overrides some of the functionality here in order to register
 * tools for the compiler to use that are extensions with JML functionality of
 * the usual tools. Also the handling of JML-specific options is initialized
 * here.  For programmatic access to the facilities of openjml, use the methods
 * of the API class.
 * 
 */

/* NOTE ON OUTPUT:
 * Output is produced by this program in a variety of ways, through mechanisms
 * that enable it to be captured for various means of display by the wrapping
 * UI.
 * 
 * Log - there is one Log instance per context, retrieved by Log.instance(context).
 * It has a number of writers:
 *  log.noticeWriter - general information, often guarded by if (verbose) or if (jmldebug)
 *  log.warnWriter - for compiler/JML/ESC warnings
 *  log.errWriter - for compiler/JML/ESC errors
 *  
 * The IProgressReporter - this is used for messages that report progress back
 * to the UI (textual or graphic).
 * 
 * Diagnostic listener - this listens for warnings and errors, as written to
 * log.warnWriter and log.errWriter.
 * 
 * The output of the log goes to the registered instance of Log [ by 
 * context.put(logKey, new Log(...)) ].  If no log is registered, a default log
 * is used, which sends all channels of information to the PrintWriter registered
 * under context.put(outKey,...).  There is a constructor for a Log that allows
 * the notice, warning and error output to be directed to different PrintWriters.
 * 
 * If there is no DiagnosticListener registered, the warning and error outputs
 * are treated as described above.  If there is a DiagnosticListener, then the
 * diagnostic output is sent to that listener instead.
 * 
 * For OpenJML:
 * - command-line tool: no diagnostic listener is defined; Log output can be
 * directed to either System.out or System.err by a command-line option
 * - Eclipse: a diagnostic listener is defined that converts warnings and errors
 * into Eclipse markers; Log output is captured by a PrintWriter that directs
 * the output to the Eclipse console.
 * 
 * Progress Reporting: Various tools within OpenJML report progress (e.g. as
 * individual files are parsed).  The progress messages are tiered: 
 *  level 1: low volume, appropriate for text and normal output
 *  level 2: transient progress messages
 *  level 3: verbose only
 * A tool can receive these progress reports by
 * registering as a delegate via 
 */
public class Main extends com.sun.tools.javac.main.Main {

    /** The compilation unit context associated with this instance of Main
     * (for the programmatic API); for the command-line API it is simply 
     * the most recent value of the context, and is used that way in testing. */
    protected Context context;
    
    /** This is a cached copy of the output writer, for use when
     * direct writing is needed (such as printing out help information) before
     * the Log facility is registered.
     */
    // @edu.umd.cs.findbugs.annotations.NonNull
    @NonNull
    protected PrintWriter out;
    
    @Nullable
    protected DiagnosticListener<?> diagListener;
    
    /** Instances of this class are used to abruptly terminate long-running
     * JML operations.
     * @author David R. Cok
     */
    public static class JmlCanceledException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public JmlCanceledException(String message) {
            super(message);
        }
    }
    
    public ChangeableProgressReporter progressDelegate = new ChangeableProgressReporter();

    public static interface IProgressReporter {
        void setContext(Context context);
        boolean report(int ticks, int level, String message);
    }
    
    static public class ChangeableProgressReporter implements IProgressReporter {
        private IProgressReporter delegate;
        public boolean report(int ticks, int level, String message) {
            if (delegate != null) return delegate.report(ticks,level,message);
            return false;
        }
        @Pure
        public boolean hasDelegate() { return delegate != null; }
        public void setDelegate(IProgressReporter d) {
            delegate = d;
        }
        //JAVA16 @Override
        public void setContext(Context context) { if (delegate!= null) delegate.setContext(context); }
    }
    
    public static class PrintProgressReporter implements IProgressReporter {
        PrintWriter pw;
        Context context;
        
        public PrintProgressReporter(Context context, OutputStream out) {
            pw = new PrintWriter(out);
            this.context = context;
        }
        
        public PrintProgressReporter(Context context, PrintWriter w) {
            pw = w;
            this.context = context;
        }
        
        //JAVA16 @Override
        public boolean report(int ticks, int level, String message) {
            if (level <= 1 || (context != null && JmlOptionName.isOption(context,JmlOptionName.JMLVERBOSE))) pw.println(message);
            return false;
        }

        //JAVA16 @Override
        public void setContext(Context context) { this.context = context; }
}

    
    
    /**
     * Construct a compiler instance.  Errors go to stderr.
     */
    // @edu.umd.cs.findbugs.annotations.SuppressWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
    public Main() {
        this(applicationName, new PrintWriter(System.err, true), null);
    }

    /**
     * Construct a compiler instance.
     * @param name the name to use for the application
     * @param out  the PrintWriter to which to send information and error messages
     */
    public Main(/*@ non_null */String name, /*@ non_null */PrintWriter out, DiagnosticListener<? extends JavaFileObject> diagListener) {
        super(name,out);
        this.out = out;  // FIXME - would not need this if the super class declared out protected
        this.diagListener = diagListener;
    }

    public void registerReporter(Context context) {
        context.put(IProgressReporter.class,progressDelegate);
    }
    
    /** The external entry point - simply calls compiler(args) and exits with the
     * exit code returned.
     * @param args the command-line arguments
     */
    //@ requires args != null && \nonnullelements(args);
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("-Xjdb")) {
            // Copied directly from com.sun.tools.javac.Main and not tested
            String[] newargs = new String[args.length + 2];
            Class<?> c = Class.forName("com.sun.tools.example.debug.tty.TTY");
            Method method = c.getDeclaredMethod ("main", new Class[] {args.getClass()});
            method.setAccessible(true);
            System.arraycopy(args, 1, newargs, 3, args.length - 1);
            newargs[0] = "-connect";
            newargs[1] = "com.sun.jdi.CommandLineLaunch:options=-esa -ea:com.sun.tools...";
            newargs[2] = "org.jmlspecs.openjml.Main";
            method.invoke(null, new Object[] { newargs });
        } else {
            System.exit(execute(args));
        }
    }
    
    /** The default application name */
    // @edu.umd.cs.findbugs.annotations.NonNull
    @NonNull
    final public static String applicationName = "jml";
    
    /** The option string for requesting help information */
    // @edu.umd.cs.findbugs.annotations.NonNull
    @NonNull
    final public static String helpOption = "-help";
    
    /** The option string for requesting interactive mode */
    // @edu.umd.cs.findbugs.annotations.NonNull
    @NonNull
    final public static String interactiveOption = "-i";

    /** Invokes the compiler on the given command-line arguments; errors go to
     * stdout.
     * @param args the command-line arguments
     * @return     the exit code, as returned to the shell - 0 is success
     */
    //@ requires args != null && \nonnullelements(args);
    public static int execute(String[] args) {
        return execute(args,false);  // The boolean: true - errors to stdErr, false - errors to stdOut
    }

    /** A programmatic interface to the compiler that returns the exit code, but
     * does not itself call System.exit.  [This is called execute rather than
     * compile as in com.sun.tools.javac.Main because we also have to override
     * com.sun.tools.javac.main.Main.compile ]
     * @param args the command-line arguments
     * @param useStdErr if true, errors go to stderr; if false they go to stdout
     * @return the exit code as sent to the shell (0 is success)
     */ 
    //@ requires args != null && \nonnullelements(args);
    public static int execute(String[] args, boolean useStdErr) {
        return execute(new PrintWriter(useStdErr ? System.err : System.out, true), null, args);
    }
    
    public static Main lastRun = null;
    
    public static int execute(@NonNull PrintWriter writer, @Nullable DiagnosticListener<JavaFileObject> diagListener, @NonNull String[] args) {
        //System.out.println("STARTING");
        int errorcode = 1; //com.sun.tools.javac.main.Main.EXIT_ERROR;
        try {
            if (args == null) {
                Context context = new Context(); // This is a temporary context just for this error message.
                                    // It is not the one used for the options and compilation
                Log log = Log.instance(context);
                JavacMessages.instance(context).add(Utils.messagesJML);
                log.error("jml.main.null.args","org.jmlspecs.openjml.Main.main");
                errorcode = 2; // com.sun.tools.javac.main.Main.EXIT_CMDERR;
            } else {
                // We have to interpret the -useJavaCompiler option before we start
                // the compiler (which does the normal option processing).
                // Since this is rare, we'll require that it be the first
                // option.
                boolean useJavaCompiler = args.length > 0 &&
                        args[0].equals(JmlOptionName.USEJAVACOMPILER.optionName());
                if (useJavaCompiler) {
                    errorcode = com.sun.tools.javac.Main.compile(args);
                } else if (args.length > 0 && args[0].equals(interactiveOption)) {
                    // interactive mode
                    errorcode = new org.jmlspecs.openjml.Interactive().run(args);
                    if (Utils.jmldebug || errorcode != 0) writer.println("ENDING with exit code " + errorcode); 
                } else {
                    Main compiler = new Main(applicationName, writer,diagListener);
                    lastRun = compiler;
                    errorcode = compiler.compile(args);
                    if (Utils.jmldebug || errorcode != 0) writer.println("ENDING with exit code " + errorcode);
                }
            }
        } catch (Exception e) {
            // Most exceptions are caught prior to this, so this will happen only for the
            // most catastrophic kinds of failure such as failures to initialize
            // properly.  (You can test this by programmatically throwing an exception in the try
            // block above.)
            Context context = new Context(); // This is a temporary context just for this error message.
                                               // It is not the one used for the options and compilation
            Log log = Log.instance(context);
            JavacMessages.instance(context).add(Utils.messagesJML);
            log.error("jml.toplevel.exception",e);
            e.printStackTrace(System.err);
            errorcode = 3; //com.sun.tools.javac.main.Main.EXIT_SYSERR;
        }
        return errorcode;
    }


    /** This method is overwritten so that the JML compiler can register its
     *  own tools for the various phases.  This fits in just the right place in 
     *  the compiler initialization and invocation sequence, but I'm not sure how
     *  fragile it is with updates and reorganizations of the code.
     *  
     *  @param args the command-line arguments
     *  @param context the compilation context to use
     *  @return the value to use as the command-line exit code
     */
    //@ requires args != null && \nonnullelements(args);
    //@ requires context != null;
    @Override
    public int compile(String[] args, Context context) {
        this.context = context;
        int exit = 0;
        if (args.length == 0) {
            register(context);
            exit = super.compile(args,context);
            helpJML();
        } else {
            register(context);
            // Note that the Java option processing happens in compile below.
            // Those options are not read at the time of the register call,
            // but the register call has to happen before compile is called.
            exit = super.compile(args,context);
            if (Options.instance(context).get(helpOption) != null) {
                helpJML();
            }
        }
//        JmlSpecs specs = JmlSpecs.instance(context); // This is just for debugging
//        specs.printDatabase();
//        {
//            Log.instance(context).noticeWriter.println("OUTPUT: " + ((JmlCompiler)JmlCompiler.instance(context)).counter.classes + " classes");
//            java.util.Iterator<java.util.Map.Entry<String,Integer>> iter =
//                ((JmlCompiler)JmlCompiler.instance(context)).counter.iterator();
//            for (int i = 0; i< 100 && iter.hasNext(); i++) {
//                java.util.Map.Entry<String,Integer> e = iter.next();
//                Log.instance(context).noticeWriter.println(e.getValue() + " : " + e.getKey());
//            }
//            
//        }

        return exit;
    }
        
    /** This is a utility method to print out all of the JML help information */
    protected void helpJML() {
        out.print(JmlOptionName.helpInfo());
        out.flush();
    }
    
    /** This method scans the input sequence of command-line arguments, 
     * processing any that are recognized as JML options.  Those options
     * are registered in the Options map.  The String[] returned contains
     * all of the command-line arguments in the input, except those recognized
     * as JML options.
     * @param args the input command-line arguments
     * @param options the Options map in which recognized options are recorded
     * @return all arguments not recognized as JML
     */
    //@ requires \nonnullelements(args);
    //@ ensures \result != null && \nonnullelements(\result);
    String[] processJmlArgs(@NonNull String [] args, @NonNull Options options) {
        ArrayList<String> newargs = new ArrayList<String>();
        int i = 0;
        while (i<args.length) {
            i = processJmlArg(args,i,options,newargs);
        }
        return newargs.toArray(new String[newargs.size()]);
    }
    
    /** Processes a single JML command-line option and any arguments.
     * 
     * @param args the full array of command-line arguments
     * @param i    the index of the argument to be processed
     * @param options the options object to be adjusted as JML options are found
     * @param remainingArgs any arguments that are not JML options
     * @return the index of the next argument to be processed
     */
    //@ requires \nonnullelements(args);
    //@ requires (* elements of remainingArgs are non-null *);
    //@ requires 0<= i && i< args.length;
    //@ ensures \result > i;
    int processJmlArg(@NonNull String[] args, int i, @NonNull Options options, @NonNull java.util.List<String> remainingArgs ) {
        String res = "";
        String s = args[i++];
        OptionInterface o = JmlOptionName.find(s);
        if (o == null) {
            int k = s.indexOf('=');
            if (k != -1) {
                o = JmlOptionName.find(s.substring(0,k));
                if (o != null) {
                    res = s.substring(k+1,s.length());
                    if ("false".equals(res)) res = null;
                    else if ("true".equals(res)) res = "";
                    else if (!o.hasArg()) {
                        // FIXME - problem -  ignoring argument
                    }
                }
            }
            if (s.equals(helpOption)) options.put(s,"");
        } else if (o.hasArg()) {
            res = args[i++];
        }
        if (o == null) {
            remainingArgs.add(s);
        } else if (JmlOptionName.DIR.optionName().equals(s) || JmlOptionName.DIRS.optionName().equals(s)) {
            java.util.List<File> todo = new LinkedList<File>();
            todo.add(new File(res));
            if (JmlOptionName.DIRS.optionName().equals(s)) {
                while (i<args.length && (res=args[i]).charAt(0) != '-') {
                    todo.add(new File(res));
                    i++;
                }
            }
            while (!todo.isEmpty()) {
                File file = todo.remove(0);
                if (file.isDirectory()) {
                    for (File ff: file.listFiles()) {
                        todo.add(ff);
                    }
                } else if (!file.isFile()) {
                    out.println("Not a file or directory: " + file); // FIXME - make a warning?
                } else if (file.getName().endsWith(".java")) {
                    remainingArgs.add(file.toString());
                } else {
                    // Just skip it
                }
            }
        } else if (JmlOptionName.JMLVERBOSE.optionName().equals(s)) {
            if (!progressDelegate.hasDelegate()) progressDelegate.setDelegate(new PrintProgressReporter(context,out));
            options.put(s,res);
        } else {
            // An empty string is the value of the option if it takes no arguments
            // That is, for boolean options, "" is true, null is false
            options.put(s,res);
        }
        return i;
    }

    /* We override this method in order to process the JML command-line 
     * arguments and to do any tool-specific initialization
     * after the command-line arguments are processed.  
     */   // FIXME - we are hacking the availability of 'context' here in a non-thread-safe way;
        // but note that Main treats options in a non-thread safe way
    // @edu.umd.cs.findbugs.annotations.SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    @Override
    public List<File> processArgs(String[] args) {
        Context context = this.context; // At least cache it here at the beginning
        args = processJmlArgs(args,Options.instance(context));
        List<File> files = super.processArgs(args);
        Utils.jmldebug = Options.instance(context).get(JmlOptionName.JMLDEBUG.optionName()) != null; 
        JmlSpecs.instance(context).initializeSpecsPath();
        if (!JmlOptionName.isOption(context,JmlOptionName.NOINTERNALRUNTIME)) {
            JmlSpecs.instance(context).appendRuntime();
        }
        return files;
    }
        
    /** This registers the JML versions of many of the tools (e.g. scanner, parser,
     * specifications database,...) used by the compiler.  They must be registered
     * before the Java versions are invoked, since in most cases singleton
     * values are lazily created.  Note also that since tools can only be registered
     * into the context once, circular dependencies can arise from pairs of tools
     * that use each other, if you are not careful.
     * @param context the compilation context into which to register the tools
     */
    public void register(/*@ non_null @*/ Context context) {
        this.context = context;// A hack so that context is available in processArgs()
        if (progressDelegate != null) progressDelegate.setContext(context);
        context.put(IProgressReporter.class,progressDelegate);
        registerTools(context,out,diagListener);
    }
    
    /** Called to register the JML internal tools that replace the tools used
     * in the Java compiler.
     * @param context the compiler context in which the tools are to be used
     * @param out the PrintWriter used for error and informational messages
     */
    static public <S> void registerTools(@NonNull Context context, @NonNull PrintWriter out, @Nullable DiagnosticListener<S> diagListener) {

        // This is done later, but we do it here so that the Log is
        // available if tool registration (or argument processing) needs the
        // Log.  However, note that if the Log is actually instantiated before
        // Java arguments are read, then it is not set consistently with those 
        // options.
        context.put(Log.outKey,out);
        
        if (diagListener != null) context.put(DiagnosticListener.class, diagListener);

        // These have to be first in case there are error messages during 
        // tool registration.
        JavacMessages.instance(context).add(Utils.messagesJML); // registering an additional source of JML-specific error messages

        // These register JML versions of the various tools.  ALL OF THESE MUST
        // REGISTER FACTORIES AND NOT CREATE ACTUAL INSTANCES.  Creating instances
        // will trigger a cascade of tool instance generation, which can create
        // tools (such as the Log) before the Options are processed, and can
        // trigger some circular dependencies in the constructors of the various
        // tools.
        // Any initialization of these tools that needs to be done based on 
        // options should be performed in processArgs() in this method.
        JmlSpecs.preRegister(context); // registering the specifications repository
        JmlParser.JmlFactory.preRegister(context); // registering a Jml-specific factory from which to generate JmlParsers
        JmlScanner.JmlFactory.preRegister(context); // registering a Jml-specific factory from which to generate JmlScanners
        JmlTree.Maker.preRegister(context); // registering a JML-aware factory for generating JmlTree nodes
        JmlCompiler.preRegister(context);
        JmlEnter.preRegister(context);
        JmlResolve.preRegister(context);
        JmlMemberEnter.preRegister(context);
        JmlFlow.preRegister(context);
        JmlAttr.preRegister(context);  // registering a JML-aware type checker
        JmlCheck.preRegister(context);
        JmlPretty.preRegister(context);
    }
    
    /** This is overridden so that serious internal bugs are reported as OpenJML
     * rather than Javac bugs.
     */
    // @Override
    protected void bugMessage(Throwable ex) {
        System.err.println("Internal JML bug - please report.  Build" + JavaCompiler.version());
        ex.printStackTrace(System.out);
    }
    
    
    // EXTERNAL API FOR PROGRAMATIC ACCESS TO JML COMPILER INTERNALS
    // FIXME - pretty printing needs lots of work
    // FIXME - internal classes for holding specs are not yet finalized
    
    /** Creates and initializes a compiler instance for programmatic use
     * @param args the command line arguments that setup options such as 
     * the class path and the location of specification files; any class file
     * names are ignored; annotation processors should be listed here.
     */
    public Main(@NonNull String[] args) throws Exception {
        this();
        initialize(args);
    }
    
    /** The argument is the array of command-line arguments without files - options only.
     * This should not be called by a client, but only internally.
     * 
     * @param args the array of command-line arguments used to setup options
     * 
     */
    protected Main initialize(@NonNull String[] args) throws Exception {
        context = new Context();
        JavacFileManager.preRegister(context); // can't create it until Log has been set up
        setOptions(Options.instance(context));
        filenames = new ListBuffer<File>();
        classnames = new ListBuffer<String>();
        register(context);
        processArgs(CommandLine.parse(args));
        // FIXME - warn about ignored files? or process them?
        return this;
    }
    
    public Context context() {
        return context;
    }
    

}