package de.lmu.ifi.dbs.distance;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Abstract Distance Function provides some methods valid for any extending
 * class.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractDistanceFunction<O extends DatabaseObject, D extends Distance> extends AbstractParameterizable implements DistanceFunction<O, D> {
  /**
   * Indicates an infintiy pattern.
   */
  public static final String INFINITY_PATTERN = "inf";

  /**
   * A pattern to define the required input format.
   */
  private Pattern pattern;

  /**
   * The database that holds the DatabaseObject for which
   * the distances should be computed.
   */
  private Database<O> database;

//  /**
//   * Map providing a mapping of parameters to their descriptions.
//   */
//  protected Map<String, String> parameterToDescription;
//
//  /**
//   * OptionHandler to handle options, optionHandler should be initialized in any non-abstract class
//   * extending this class.
//   */
//  protected OptionHandler optionHandler;
//
//  /**
//   * Holds the currently set parameter array.
//   */
//  private String[] currentParameterArray = new String[0];

  /**
   * Provides an abstract DistanceFunction based on the given Pattern
   * and initializes the option handler and the parameter map.
   *
   * @param pattern a pattern to define the required input format
   */
  protected AbstractDistanceFunction(Pattern pattern) {
	  super();
    this.pattern = pattern;
//    parameterToDescription = new Hashtable<String, String>();
//    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Initializes the option handler and the parameter map. This constructor
   * can be used if the required input pattern is not yet known at instantiation
   * time and will therefore be set later.
   */
  protected AbstractDistanceFunction() {
    this(null);
  }

  /**
   * Returns the distance between the two objcts specified by their obejct ids.
   * If a cache is used, the distance value is looked up in the cache. If the distance
   * does not yet exists in cache, it will be computed an put to cache.  If
   * no cache is used, the distance is computed.
   *
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objcts specified by their obejct ids
   */
  public D distance(Integer id1, Integer id2) {
    return distance(database.get(id1), database.get(id2));
  }

  /**
   * Set the database that holds the associations for the DatabaseObject for
   * which the distances should be computed.
   * <p/>
   * If this method is overwritten by any subclass super.setDatabase(database, verbose) should be called.
   *
   * @param database the database to be set
   * @param verbose  flag to allow verbose messages while performing the method
   * @param time     flag to request output of performance time
   */
  public void setDatabase(Database<O> database, boolean verbose, boolean time) {
    this.database = database;
  }

  /**
   * @see de.lmu.ifi.dbs.distance.DistanceFunction#isInfiniteDistance(de.lmu.ifi.dbs.distance.Distance)
   */
  public boolean isInfiniteDistance(D distance) {
    return distance.equals(infiniteDistance());
  }

  /**
   * @see de.lmu.ifi.dbs.distance.DistanceFunction#isNullDistance(de.lmu.ifi.dbs.distance.Distance)
   */
  public boolean isNullDistance(D distance) {
    return distance.equals(nullDistance());
  }

  /**
   * @see de.lmu.ifi.dbs.distance.DistanceFunction#isUndefinedDistance(de.lmu.ifi.dbs.distance.Distance)
   */
  public boolean isUndefinedDistance(D distance) {
    return distance.equals(undefinedDistance());
  }

  /**
   * @see de.lmu.ifi.dbs.distance.DistanceFunction#requiredInputPattern()
   */
  public String requiredInputPattern() {
    return this.pattern.pattern();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
      String[] remainingParameters = optionHandler.grabOptions(args);
      setParameters(args, remainingParameters);
      return remainingParameters;
  }

//  /**
//   * Sets the difference of the first array minus the second array
//   * as the currently set parameter array.
//   * 
//   * 
//   * @param complete the complete array
//   * @param part an array that contains only elements of the first array
//   */
//  protected void setParameters(String[] complete, String[] part)
//  {
//      currentParameterArray = Util.parameterDifference(complete, part);
//  }
  
//  /**
//   * 
//   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
//   */
//  public String[] getParameters()
//  {
//      String[] param = new String[currentParameterArray.length];
//      System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
//      return param;
//  }
  
  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = new ArrayList<AttributeSettings>();

    AttributeSettings setting = new AttributeSettings(this);
    settings.add(setting);

    return settings;
  }

  /**
   * Returns true if the given pattern matches the defined pattern, false
   * otherwise.
   *
   * @param pattern the pattern to be matched woth the defined pattern
   * @return true if the given pattern matches the defined pattern, false
   *         otherwise
   */
  protected boolean matches(String pattern) {
    return this.pattern.matcher(pattern).matches();
  }

  /**
   * Returns the database holding the objects for which the distances should be computed.
   *
   * @return the database holding the objects for which the distances should be computed
   */
  protected Database<O> getDatabase() {
    return database;
  }

  /**
   * Sets the required input pattern.
   * @param pattern the pattern to be set
   */
  protected void setRequiredInputPattern(Pattern pattern) {
    this.pattern = pattern;
  }
}
