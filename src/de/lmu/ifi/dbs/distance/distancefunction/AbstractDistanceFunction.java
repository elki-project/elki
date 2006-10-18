package de.lmu.ifi.dbs.distance.distancefunction;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.Distance;

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

  /**
   * Provides an abstract DistanceFunction based on the given Pattern
   * and initializes the option handler and the parameter map.
   *
   * @param pattern a pattern to define the required input format
   */
  protected AbstractDistanceFunction(Pattern pattern) {
    super();
    this.pattern = pattern;
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
   *
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objcts specified by their obejct ids
   */
  public D distance(Integer id1, Integer id2) {
    return distance(database.get(id1), database.get(id2));
  }

  /**
   * Returns the distance between the two specified objects.
   *
   * @param id1 first object id
   * @param o2  second DatabaseObject
   * @return the distance between the two objcts specified by their obejct ids
   */
  public D distance(Integer id1, O o2) {
    return distance(database.get(id1), o2);  }

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
   * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#isInfiniteDistance(de.lmu.ifi.dbs.distance.Distance)
   */
  public boolean isInfiniteDistance(D distance) {
    return distance.equals(infiniteDistance());
  }

  /**
   * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#isNullDistance(de.lmu.ifi.dbs.distance.Distance)
   */
  public boolean isNullDistance(D distance) {
    return distance.equals(nullDistance());
  }

  /**
   * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#isUndefinedDistance(de.lmu.ifi.dbs.distance.Distance)
   */
  public boolean isUndefinedDistance(D distance) {
    return distance.equals(undefinedDistance());
  }

  /**
   * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#requiredInputPattern()
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
   * Returns true if the given pattern matches the pattern defined, false
   * otherwise.
   *
   * @param pattern the pattern to be matched with the pattern defined
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
