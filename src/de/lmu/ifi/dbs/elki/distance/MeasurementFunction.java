package de.lmu.ifi.dbs.elki.distance;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Interface Measurement describes the requirements of any measurement
 * function (e.g. distance function or similarity function), that provides a measurement
 * for comparing database objects.
 *
 * @author Elke Achtert
 * @param <D> the type of Distance used as measurement for comparing database objects
 * @param <O> the type of DatabaseObject for which a measurement is provided for comparison
 */
public interface MeasurementFunction<O extends DatabaseObject, D extends Distance<D>> {
  /**
   * Set the database that holds the associations for the DatabaseObject for
   * which the measurements should be computed.
   *
   * @param database the database to be set
   */
  void setDatabase(Database<O> database);
  
  /**
   * Method to get the distance functions factory.
   * 
   * @return Factory for distance objects
   */
  D getDistanceFactory();
  
  /**
   * Provides an infinite distance.
   * 
   * @return an infinite distance
   */
  D infiniteDistance();

  /**
   * Provides a null distance.
   * 
   * @return a null distance
   */
  D nullDistance();

  /**
   * Provides an undefined distance.
   * 
   * @return an undefined distance
   */
  D undefinedDistance();
  
  /**
   * Parse a string value into a distance
   * 
   * @param val input string 
   * @return parsed value
   * @throws IllegalArgumentException on parsing error
   */
  D valueOf(String val) throws IllegalArgumentException;
  
  /**
   * Get the input data type of the function.
   */
  Class<? super O> getInputDatatype();
}