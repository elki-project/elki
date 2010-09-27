package de.lmu.ifi.dbs.elki.database.ids;

/**
 * Immutable pair of two DBIDs. This can be stored more efficiently than when
 * using {@link de.lmu.ifi.dbs.elki.utilities.pairs.Pair}
 * 
 * @author Erich Schubert
 */
public interface DBIDPair {
  /**
   * Getter for first
   * 
   * @return first element in pair
   */
  public DBID getFirst();

  /**
   * Getter for second element in pair
   * 
   * @return second element in pair
   */
  public DBID getSecond();
}