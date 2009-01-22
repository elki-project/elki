package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Generic Pair<FIRST,SECOND> interface
 * Implementations vary when it comes to Comparable etc.
 * 
 * @author Erich Schubert
 *
 * @param <FIRST> first type
 * @param <SECOND> second type
 */
public interface PairInterface<FIRST, SECOND> {
  /**
   * Access first component in pair.
   * 
   * @return first component in pair
   */
  public FIRST getFirst();
  /**
   * Assign new value for first component in pair.
   * 
   * @param first new value for first component
   */
  public void setFirst(FIRST first);
  /**
   * Access second component in pair.
   * 
   * @return second component in pair
   */
  public SECOND getSecond();
  /**
   * Assign new value for second component in pair.
   * 
   * @param second new value for second component
   */
  public void setSecond(SECOND second);
}