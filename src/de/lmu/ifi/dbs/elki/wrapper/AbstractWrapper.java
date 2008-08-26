package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.util.ArrayList;
import java.util.List;

/**
 * AbstractWrapper sets the values for flags verbose and help. <p/> Any Wrapper
 * class that makes use of these flags may extend this class. Beware to make
 * correct use of parameter settings via optionHandler as commented with
 * constructor and methods.
 *
 * @author Elke Achtert
 */

public abstract class AbstractWrapper extends AbstractParameterizable implements Wrapper {
    /**
     * Flag to obtain help-message.
     * <p>Key: {@code -h} </p>
     */
    private final Flag HELP_FLAG;

    /**
     * Flag to allow verbose messages while performing the wrapper.
     * <p>Key: {@code -verbose} </p>
     */
    private final Flag VERBOSE_FLAG;

    /**
     * Print backtraces on exceptions.
     * <p>Key: {@code -stack-trace} </p>
     */
    private final Flag TRACE_FLAG;

    /**
     * Value of verbose flag.
     */
    private boolean verbose;
    
    /**
     * Value of trace flag.
     */
    private boolean trace;

    /**
     * The remaining parameters after the option handler grabbed the options for
     * this wrapper.
     */
    private List<String> remainingParameters;

    /**
     * Adds the flags {@link #VERBOSE_FLAG} and {@link #HELP_FLAG} to the option handler.
     * Any extending class should call this constructor, then add further
     * parameters.
     */
    protected AbstractWrapper() {
        // verbose
        VERBOSE_FLAG = new Flag(OptionID.ALGORITHM_VERBOSE);
        VERBOSE_FLAG.setShortDescription("Flag to allow verbose messages while performing the wrapper.");
        optionHandler.put(VERBOSE_FLAG);

        // help
        HELP_FLAG = new Flag(OptionID.HELP);
        HELP_FLAG.setShortDescription("Flag to obtain help-message. Causes immediate stop of the program.");
        optionHandler.put(HELP_FLAG);
        
        // trace
        TRACE_FLAG = new Flag(OptionID.TRACE_DEBUG);
        TRACE_FLAG.setShortDescription("Print stack trace on exceptions. Useful for development within the framework.");
        optionHandler.put(TRACE_FLAG);
    }

     /**
     * Calls {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#setParameters(String[]) AbstractParameterizable#setParameters(args)}
     * and sets additionally the values of the flags
     * {@link #VERBOSE_FLAG} and {@link #HELP_FLAG}.
     *
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        this.remainingParameters = new ArrayList<String>(remainingParameters.length);
        for (String s : remainingParameters) {
            this.remainingParameters.add(s);
        }

        // help
        if (HELP_FLAG.isSet())
            throw new AbortException(optionHandler.usage(""));

        // verbose
        verbose = VERBOSE_FLAG.isSet();
        
        // stack trace
        trace = TRACE_FLAG.isSet();

        setParameters(args, new String[0]);
        return new String[0];
    }

    /**
     * Returns whether verbose messages should be printed while executing the
     * wrapper.
     *
     * @return whether verbose messages should be printed while executing the
     *         wrapper
     */
    public final boolean isVerbose() {
        return verbose;
    }

    /**
     * Returns whether a stack trace should be printed on error.
     *
     * @return whether stack trace should be printed on error.
     */
    public final boolean wantStackTrace() {
        return trace;
    }

    /**
     * Returns a copy of the remaining parameters after the option handler
     * grabbed the options for this wrapper.
     *
     * @return the remaining parameters
     */
    public final List<String> getRemainingParameters() {
        if (remainingParameters == null) {
            return new ArrayList<String>();
        }
        
        List<String> result = new ArrayList<String>(remainingParameters.size());
        result.addAll(remainingParameters);
        return result;
    }

    /**
     * Generic command line invocation.
     * Refactored to have a central place for outermost exception handling.
     * 
     * @param args the arguments to run this wrapper
     */
    public void runCLIWrapper(String[] args) {
      LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);
      try {
        setParameters(args);
        run();
      }
      catch(AbortException e) {
        if (wantStackTrace())
          e.printStackTrace(System.err);
        verbose(e.toString());
      }
      catch(ParameterException e) {
        if (wantStackTrace())
          e.printStackTrace(System.err);
        warning(e.toString());
      }
      catch(Exception e) {
        if (wantStackTrace())
          e.printStackTrace(System.err);
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        exception(optionHandler.usage(e.toString()), cause);
      }
    }
}
