package de.lmu.ifi.dbs.elki.database.ids;

import de.lmu.ifi.dbs.elki.utilities.pairs.PairInterface;

/**
 * Immutable pair of two DBIDs. This can be stored more efficiently than when
 * using {@link de.lmu.ifi.dbs.elki.utilities.pairs.Pair}
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.database.ids.DBID
 */
// TODO: implement DBIDs?
public interface DBIDPair extends PairInterface {
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