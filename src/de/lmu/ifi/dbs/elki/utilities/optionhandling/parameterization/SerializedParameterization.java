package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Manage a parameterization serialized as String array, e.g. from command line.
 * 
 * When building parameter lists, use {@link ListParameterization} where possible.
 * 
 * @author Erich Schubert
 */
public class SerializedParameterization extends AbstractParameterization {
  /**
   * Prefix of option markers on the command line.
   * <p/>
   * The option markers are supposed to be given on the command line with
   * leading -.
   */
  public static final String OPTION_PREFIX = "-";

  /**
   * Parameter storage
   */
  LinkedList<String> parameters = null;

  /**
   * Constructor
   */
  public SerializedParameterization() {
    super();
    parameters = new LinkedList<String>();
  }

  /**
   * Constructor
   */
  public SerializedParameterization(String[] args) {
    this();
    for (String arg : args) {
      parameters.add(arg);
    }
  }

  /**
   * Constructor
   */
  public SerializedParameterization(List<String> args) {
    this();
    parameters.addAll(args);
  }

  /**
   * Return the yet unused parameters.
   * 
   * @return Unused parameters.
   */
  public List<String> getRemainingParameters() {
    return parameters;
  }

  @Override
  public boolean hasUnusedParameters() {
    return (parameters.size() > 0);
  }

  /**
   * Log a warning if there were unused parameters.
   */
  public void logUnusedParameters() {
    if(hasUnusedParameters()) {
      logger.warning("The following parameters were not processed: " + parameters);
    }
  }

  @Override
  public boolean setValueForOption(Parameter<?,?> opt) throws ParameterException {
    Iterator<String> piter = parameters.iterator();
    while(piter.hasNext()) {
      String cur = piter.next();
      if(!cur.startsWith(OPTION_PREFIX)) {
        continue;
        // throw new NoParameterValueException(cur + " is no parameter!");
      }

      // get the option without the option prefix -
      String noPrefixOption = cur.substring(OPTION_PREFIX.length());

      if(opt.getName().equals(noPrefixOption)) {
        // Consume.
        piter.remove();
        // check if the option is a parameter or a flag
        if(opt instanceof Flag) {
          String set = Flag.SET;
          // The next element must be a parameter
          if(piter.hasNext()) {
            String next = piter.next();
            if(Flag.SET.equals(next)) {
              set = Flag.SET;
              piter.remove();
            }
            else if(Flag.NOT_SET.equals(next)) {
              set = Flag.NOT_SET;
              piter.remove();
            }
            else if(!next.startsWith(OPTION_PREFIX)) {
              throw new NoParameterValueException("Flag " + opt.getName() + " requires no parameter-value! " + "(read parameter-value: " + next + ")");
            }
            // We do not consume the next if it's not for us ...
          }
          // set the Flag
          opt.setValue(set);
          return true;
        }
        else {
          // Ensure there is a potential value for this parameter
          if(!piter.hasNext()) {
            throw new NoParameterValueException("Parameter " + opt.getName() + " requires a parameter value!");
          }
          opt.setValue(piter.next());
          // Consume parameter
          piter.remove();
          // Success - return.
          return true;
        }
      }
    }
    return false;
  }

  /** {@inheritDoc}
   * Default implementation, for flat parameterizations. 
   */
  @Override
  public Parameterization descend(@SuppressWarnings("unused") Parameter<?, ?> option) {
    return this;
  }
}