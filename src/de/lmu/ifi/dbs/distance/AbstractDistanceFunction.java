package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Abstract Distance Function provides some methods valid for any extending
 * class.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractDistanceFunction<O extends MetricalObject, D extends Distance> implements DistanceFunction<O, D> {
  /**
   * Indicates an infintiy pattern.
   */
  public static final String INFINITY_PATTERN = "inf";

  /**
   * A pattern to define the required input format.
   */
  private Pattern pattern;

  /**
   * The database that holds the MetricalObjects for which
   * the distances should be computed.
   */
  private Database<O> database;

  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  protected Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * OptionHandler to handle options, optionHandler should be initialized in any non-abstract class
   * extending this class.
   */
  protected OptionHandler optionHandler;

  /**
   * Holds the number of performed distance computations.
   */
  protected int noDistanceComputations;

  /**
   * Provides an abstract DistanceFunction based on the given Pattern.
   *
   * @param pattern a pattern to define the required input format
   */
  protected AbstractDistanceFunction(Pattern pattern) {
    this.pattern = pattern;
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
    return database.cachedDistance(this, id1, id2);
  }

  /**
   * Set the database that holds the associations for the MetricalObject for
   * which the distances should be computed.
   * <p/>
   * If this method is overwritten by any subclass super.setDatabase(database, verbose) should be called.
   *
   * @param database the database to be set
   * @param verbose  flag to allow verbose messages while performing the method
   */
  public void setDatabase(Database<O> database, boolean verbose) {
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
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    return args;
  }

  /**
   * Returns the number of performed distance computations.
   *
   * @return the number of performed distance computations
   */
  public int getNumberOfDistanceComputations() {
    return noDistanceComputations;
  }

  /**
   * Resets the number of performed distance computations
   */
  public void resetNumberOfDistanceComputations() {
    this.noDistanceComputations = 0;
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
}
