package de.lmu.ifi.dbs.elki.distance;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;

/**
 * Abstract implementation of interface {@link MeasurementFunction} that
 * provides some methods valid for any extending class.
 * 
 * @author Elke Achtert
 * @param <D> the type of Distance used as measurement for comparing database
 *        objects
 * @param <O> the type of DatabaseObject for which a measurement is provided for
 *        comparison
 */
public abstract class AbstractMeasurementFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractLoggable implements MeasurementFunction<O, D> {
  /**
   * The database that holds the objects for which the measurements should be
   * computed.
   */
  private Database<O> database;
  
  /**
   * The distance type
   */
  protected final D distance;

  /**
   * Provides an abstract MeasurementFunction.
   * 
   * @param distance Distance factory instance
   */
  protected AbstractMeasurementFunction(D distance) {
    super();
    this.distance = distance;
  }

  /**
   * @param database Database
   */
  public void setDatabase(Database<O> database) {
    this.database = database;
  }

  /**
   * Returns the database holding the objects for which the measurements should
   * be computed.
   * 
   * @return the database holding the objects for which the measurements should
   *         be computed
   */
  protected Database<O> getDatabase() {
    return database;
  }

  @Override
  public D infiniteDistance() {
    return distance.infiniteDistance();
  }

  @Override
  public D nullDistance() {
    return distance.nullDistance();
  }

  @Override
  public D undefinedDistance() {
    return distance.undefinedDistance();
  }

  @Override
  public D valueOf(String val) throws IllegalArgumentException {
    return distance.parseString(val);
  }
}