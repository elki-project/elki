package de.lmu.ifi.dbs.elki.database.relation;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Interface for double-valued relations.
 * 
 * @author Erich Schubert
 */
public interface DoubleRelation extends Relation<Double> {
  /**
   * Get the representation of an object.
   * 
   * @param id Object ID
   * @return object instance
   */
  public double doubleValue(DBIDRef id);

  /**
   * Set an object representation.
   * 
   * @param id Object ID
   * @param val Value
   */
  // TODO: remove / move to a writable API?
  public void set(DBIDRef id, double val);

  /**
   * @deprecated use {@link #doubleValue} instead.
   */
  @Deprecated
  @Override
  public Double get(DBIDRef id);

  /**
   * @deprecated use {@link #set(id, double)} instead.
   */
  @Deprecated
  @Override
  public void set(DBIDRef id, Double val);
}
