package de.lmu.ifi.dbs.elki.utilities;

/**
 * Generic Pair<FIRST,SECOND> interface
 * Implementations vary when it comes to Comparable etc.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 *
 * @param <FIRST> first type
 * @param <SECOND> second type
 */
public interface Pair<FIRST, SECOND> {
  public FIRST getFirst();
  public void setFirst(FIRST first);
  public SECOND getSecond();
  public void setSecond(SECOND second);
}