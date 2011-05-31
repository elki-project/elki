package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Pair interface.
 * 
 * Note: this currently is <em>empty by design</em>. You should always decide
 * explicitly whether to use boxing pairs {@link Pair} or primitive pairs such
 * as {@link IntIntPair}
 * 
 * @author Erich Schubert
 * 
 * @param FIRST first type
 * @param SECOND second type
 */
public interface PairInterface<FIRST, SECOND> {
  /**
   * Get the first object - note: this may cause autoboxing, use pair.first for native pairs!
   * 
   * @return First object
   */
  public FIRST getFirst();
  
  /**
   * Get the second object - note: this may cause autoboxing, use pair.second for native pairs!
   * 
   * @return Second object
   */
  public SECOND getSecond();
}