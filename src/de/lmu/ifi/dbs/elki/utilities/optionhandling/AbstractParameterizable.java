package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Abstract superclass for classes parameterizable. Provides the option handler
 * and the parameter array.
 * 
 * @author Elke Achtert
 */
public abstract class AbstractParameterizable extends AbstractLoggable implements Parameterizable {

  /**
   * OptionHandler for handling options.
   */
  protected OptionHandler optionHandler;

  /**
   * Holds the currently set parameter array.
   */
  private List<String> currentParameterArrayList = new ArrayList<String>(0);
  
  /**
   * Hold parameterizable contained
   */
  private ArrayList<Pair<Parameterizable, List<OptionID>>> parameterizables = new ArrayList<Pair<Parameterizable, List<OptionID>>>(0);

  /**
   * Creates a new AbstractParameterizable that provides the option handler and
   * the parameter array.
   */
  public AbstractParameterizable() {
    super(LoggingConfiguration.DEBUG);
    optionHandler = new OptionHandler();
  }

  /**
   * Adds the given Option to the set of Options known to this Parameterizable.
   * 
   * @param option the Option to add to the set of known Options of this
   *        Parameterizable
   */
  protected void addOption(Option<?> option) {
    this.optionHandler.put(option);
  }

  /**
   * Deletes the given Option from the set of Options known to this
   * Parameterizable.
   * 
   * @param option the Option to remove from the set of Options known to this
   *        Parameterizable
   * @throws UnusedParameterException if the given Option is unknown
   */
  protected void removeOption(Option<?> option) throws UnusedParameterException {
    this.optionHandler.remove(option.getName());
  }
  
  /**
   * Add a new parameterizable to the list. Used for listing options and settings.
   * 
   * @param p parameterizable
   */
  protected void addParameterizable(Parameterizable p) {
    this.parameterizables.add(new Pair<Parameterizable, List<OptionID>>(p, null));
  }
  
  /**
   * Add a new parameterizable to the list. Used for listing options and settings.
   * 
   * @param p parameterizable
   * @param override overridden parameters
   */
  protected void addParameterizable(Parameterizable p, List<OptionID> override) {
    this.parameterizables.add(new Pair<Parameterizable, List<OptionID>>(p, override));
  }
  
  /**
   * Remove a parameterizable from the list. Used for listing options and settings.
   * 
   * @param p parameterizable to remove
   */
  protected void removeParameterizable(Parameterizable p) {
    for (ListIterator<Pair<Parameterizable, List<OptionID>>> iter = this.parameterizables.listIterator(); iter.hasNext(); ) {
      if (iter.next().getFirst() == p) {
        iter.remove();
        break;
      }
    }
  }

  /**
   * Grabs all specified options from the option handler. Any extending class
   * should call this method first and return the returned array without further
   * changes, but after setting further required parameters. An example for
   * overwriting this method taking advantage from the previously (in
   * superclasses) defined options would be:
   * <p/>
   * 
   * <pre>
   * {
   *   List<String> remainingParameters = super.setParameters(args);
   *   // set parameters for your class
   *   // for example like this:
   *   if(isSet(MY_PARAM_VALUE_PARAM))
   *   {
   *      myParamValue = getParameterValue(MY_PARAM_VALUE_PARAM);
   *   }
   *   .
   *   .
   *   .
   *   return remainingParameters;
   *   // or in case of attributes requesting parameters themselves
   *   // return parameterizableAttribbute.setParameters(remainingParameters);
   * }
   * </pre>
   */
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = optionHandler.grabOptions(args);
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Sets the difference of the first array minus the second array as the
   * currently set parameter array.
   * 
   * @param complete the complete array
   * @param part an array that contains only elements of the first array
   */
  protected final void rememberParametersExcept(List<String> complete, List<String> part) {
    currentParameterArrayList = OptionUtil.parameterDifference(complete, part);
  }

  /*
   * See: {@link Parameterizable#getParameters()}
   */
  public final ArrayList<String> getParameters() {
    ArrayList<String> param = new ArrayList<String>(currentParameterArrayList);
    return param;
  }

  /**
   * Returns the settings of all options assigned to the option handler.
   * @return the settings of all options assigned to the option handler
   */
  // FIXME - legacy adapter. Retire and use collectOptions instead.
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = new ArrayList<AttributeSettings>();
    // collect all options
    ArrayList<Pair<Parameterizable, Option<?>>> collection = this.collectOptions();
    // group by parameterizable
    HashMap<Parameterizable, AttributeSettings> map = new HashMap<Parameterizable, AttributeSettings>();
    for (Pair<Parameterizable, Option<?>> pair : collection) {
      AttributeSettings set = map.get(pair.getFirst());
      if (set == null) {
        set = new AttributeSettings(pair.getFirst());
        map.put(pair.getFirst(), set);
        settings.add(set);
      }
      set.addOption(pair.getSecond());
    }
    return settings;
  }

  /**
   * Returns a short description of the class.
   * 
   * @return Description of the class
   */
  public String shortDescription() {
    return null;
  }

  /**
   * @see OptionHandler#checkGlobalParameterConstraints()
   */
  // TODO: remove - only used in guidraft1?
  public void checkGlobalParameterConstraints() throws ParameterException {
    this.optionHandler.checkGlobalParameterConstraints();
  }

  /**
   * Get all possible options.
   * 
   * @return list of parameterizable and options.
   */
  public ArrayList<Pair<Parameterizable, Option<?>>> collectOptions() {
    ArrayList<Pair<Parameterizable, Option<?>>> r = new ArrayList<Pair<Parameterizable, Option<?>>>();
    for(Option<?> o : this.optionHandler.getOptions()) {
      r.add(new Pair<Parameterizable, Option<?>>(this, o));
      // TODO: recurse into ClassParameters?
    }
    for (int i = 0; i < parameterizables.size(); i++) {
      Pair<Parameterizable,List<OptionID>> p = parameterizables.get(i);
      if (p.getSecond() != null) {
        // remove any of the given parameters
        ArrayList<Pair<Parameterizable, Option<?>>> col = p.getFirst().collectOptions();
        ArrayList<OptionID> toremove = new ArrayList<OptionID>(p.getSecond());
        for (ListIterator<Pair<Parameterizable, Option<?>>> i2 = col.listIterator(); i2.hasNext(); ) {
          Pair<Parameterizable, Option<?>> p2 = i2.next();
          if (toremove.remove(p2.getSecond().getOptionID())) {
            i2.remove();
          }
        }
        // add remaining options to collection
        r.addAll(col);
        if (toremove.size() > 0) {
          StringBuffer remainingOptions = new StringBuffer();
          boolean first = true;
          for (OptionID oid : toremove) {
            if (!first) {
              remainingOptions.append(", ");
            }
            remainingOptions.append(oid.getName());
            first = false;
          }
          // TODO: change to "debugFine"?
          logger.warning("Options were given as ignore-because-predefined that were not found in the collected options: " + remainingOptions.toString(), new Throwable());
        }
      } else {
        r.addAll(p.getFirst().collectOptions());
      }
    }
    return r;
  }
}
