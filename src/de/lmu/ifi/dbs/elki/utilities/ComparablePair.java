package de.lmu.ifi.dbs.elki.utilities;

/**
 * Pair with canonical comparison function
 * 
 * @author Erich Schubert
 *
 * @param <FIRST> first type
 * @param <SECOND> second type
 */
public class ComparablePair<FIRST extends Comparable<FIRST>,SECOND extends Comparable<SECOND>> extends Pair<FIRST,SECOND> implements Comparable<ComparablePair<FIRST,SECOND>> {
  public ComparablePair(FIRST first, SECOND second) {
    super(first, second);
  }

  public int compareTo(ComparablePair<FIRST, SECOND> other) {
    int delta1 = this.first.compareTo(other.first);
    if (delta1 != 0) return delta1;
    return this.second.compareTo(other.second);
  }
}
