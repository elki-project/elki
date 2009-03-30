package de.lmu.ifi.dbs.elki.utilities.pairs;

import java.util.Comparator;

/**
 * Generic SimplePair<FIRST,SECOND> class.
 * 
 * Does not implement any "special" interfaces such as Comparable, use
 * {@link ComparablePair} if you want comparable pairs.
 * 
 * @author Erich Schubert
 * 
 * @param <FIRST> first type
 * @param <SECOND> second type
 */
public class SimplePair<FIRST, SECOND> {
  /**
   * First value in pair
   */
  public FIRST first;

  /**
   * Second value in pair
   */
  public SECOND second;

  /**
   * Initialize pair
   * 
   * @param first first parameter
   * @param second second parameter
   */
  public SimplePair(FIRST first, SECOND second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Canonical toString operator
   */
  @Override
  public String toString() {
    return "Pair(" + first.toString() + ", " + second.toString() + ")";
  }

  /**
   * Getter for first
   * 
   * @return first element in pair
   */
  public final FIRST getFirst() {
    return first;
  }

  /**
   * Setter for first
   * 
   * @param first new value for first element
   */
  public final void setFirst(FIRST first) {
    this.first = first;
  }

  /**
   * Getter for second element in pair
   * 
   * @return second element in pair
   */
  public final SECOND getSecond() {
    return second;
  }

  /**
   * Setter for second
   * 
   * @param second new value for second element
   */
  public final void setSecond(SECOND second) {
    this.second = second;
  }

  /**
   * Create a new array of the given size (for generics)
   * 
   * @param size array size
   * @return empty array of the new type.
   */
  @SuppressWarnings("unchecked")
  public static final <F, S> SimplePair<F, S>[] newArray(int size) {
    return new SimplePair[size];
  }

  /**
   * Simple equals statement.
   * 
   * @param obj Object to compare to
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(!(obj instanceof SimplePair)) {
      return false;
    }
    SimplePair<FIRST, SECOND> other = (SimplePair<FIRST, SECOND>) obj;
    return (this.first.equals(other.getFirst())) && (this.second.equals(other.getSecond()));
  }

  /**
   * Canonical hash function, mixing the two hash values.
   */
  @Override
  public final int hashCode() {
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    final long prime = 2654435761L;
    long result = 1;
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((second == null) ? 0 : second.hashCode());
    return (int) result;
  }

  /**
   * Compare two SimplePairs based on a comparator for the first component.
   * 
   * @param <FIRST> first type
   * @param <SECOND> second type
   */
  public static class CompareByFirst<FIRST, SECOND> implements Comparator<SimplePair<FIRST, SECOND>> {
    /**
     * A comparator for type P.
     */
    private Comparator<FIRST> comparator;

    /**
     * Provides a comparator for an {@link SimplePair} based on the given
     * Comparator for type <code>P</code>.
     * 
     * @param comparator a Comparator for type <code>P</code> to base the
     *        comparison of an {@link SimplePair} on
     */
    public CompareByFirst(Comparator<FIRST> comparator) {
      this.comparator = comparator;
    }

    /**
     * To Objects of type {@link SimplePair} are compared based on the
     * comparison of their property using the current {@link #comparator}.
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(SimplePair<FIRST, SECOND> o1, SimplePair<FIRST, SECOND> o2) {
      return comparator.compare(o1.getFirst(), o2.getFirst());
    }
  }
  
  /**
   * Compare two SimplePairs based on a comparator for the second component.
   * 
   * @param <FIRST> first type
   * @param <SECOND> second type
   */
  public static class CompareBySecond<FIRST, SECOND> implements Comparator<SimplePair<FIRST, SECOND>> {
    /**
     * A comparator for type P.
     */
    private Comparator<SECOND> comparator;

    /**
     * Provides a comparator for an {@link SimplePair} based on the given
     * Comparator for type <code>P</code>.
     * 
     * @param comparator a Comparator for type <code>P</code> to base the
     *        comparison of an {@link SimplePair} on
     */
    public CompareBySecond(Comparator<SECOND> comparator) {
      this.comparator = comparator;
    }

    /**
     * To Objects of type {@link SimplePair} are compared based on the
     * comparison of their property using the current {@link #comparator}.
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(SimplePair<FIRST, SECOND> o1, SimplePair<FIRST, SECOND> o2) {
      return comparator.compare(o1.getSecond(), o2.getSecond());
    }
  }
  
  /**
   * Compare two SimplePairs based on two comparators
   * 
   * @param <FIRST> first type
   * @param <SECOND> second type
   */
  public static class Compare<FIRST, SECOND> implements Comparator<SimplePair<FIRST, SECOND>> {
    /**
     * A comparator for type FIRST.
     */
    private Comparator<FIRST> fcomparator;

    /**
     * A comparator for type FIRST.
     */
    private Comparator<SECOND> scomparator;

    /**
     * Provides a comparator for an {@link SimplePair} based on the given
     * Comparator for type <code>P</code>.
     * 
     * @param comparator a Comparator for type <code>P</code> to base the
     *        comparison of an {@link SimplePair} on
     */
    public Compare(Comparator<FIRST> fcomparator, Comparator<SECOND> scomparator) {
      this.fcomparator = fcomparator;
      this.scomparator = scomparator;
    }

    /**
     * Two Objects of type {@link SimplePair} are compared based on the
     * comparison of their property using the comparators {@link #fcomparator}, then {@link #scomparator}.
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(SimplePair<FIRST, SECOND> o1, SimplePair<FIRST, SECOND> o2) {
      int delta1 = fcomparator.compare(o1.getFirst(), o2.getFirst());
      if (delta1 != 0) {
        return delta1;
      }
      return scomparator.compare(o1.getSecond(), o2.getSecond());
    }
  }
  
  /**
   * Compare two SimplePairs based on two comparators, but by second component first.
   * 
   * @param <FIRST> first type
   * @param <SECOND> second type
   */
  public static class CompareSwapped<FIRST, SECOND> implements Comparator<SimplePair<FIRST, SECOND>> {
    /**
     * A comparator for type FIRST.
     */
    private Comparator<FIRST> fcomparator;

    /**
     * A comparator for type FIRST.
     */
    private Comparator<SECOND> scomparator;

    /**
     * Provides a comparator for an {@link SimplePair} based on the given
     * Comparator for type <code>P</code>.
     * 
     * @param comparator a Comparator for type <code>P</code> to base the
     *        comparison of an {@link SimplePair} on
     */
    public CompareSwapped(Comparator<FIRST> fcomparator, Comparator<SECOND> scomparator) {
      this.fcomparator = fcomparator;
      this.scomparator = scomparator;
    }

    /**
     * Two Objects of type {@link SimplePair} are compared based on the
     * comparison of their property using the given comparators {@link #scomparator}, then {@link #fcomparator}.
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(SimplePair<FIRST, SECOND> o1, SimplePair<FIRST, SECOND> o2) {
      int delta2 = scomparator.compare(o1.getSecond(), o2.getSecond());
      if (delta2 != 0) {
        return delta2;
      }
      return fcomparator.compare(o1.getFirst(), o2.getFirst());
    }
  }
}