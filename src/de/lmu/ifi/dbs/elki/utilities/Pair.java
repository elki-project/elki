package de.lmu.ifi.dbs.elki.utilities;

/**
 * Generic Pair<FIRST,SECOND> class
 * 
 * @author Erich Schubert
 *
 * @param <FIRST> first type
 * @param <SECOND> second type
 */
public class Pair<FIRST, SECOND> {
  /* these are public by intention, Pair<> is supposed to be a simple wrapper */
  public FIRST first;
  public SECOND second;

  /**
   * Initialize pair
   * 
   * @param first first parameter
   * @param second second parameter
   */
  public Pair(FIRST first, SECOND second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Canonical toString operator
   */
  public String toString() {
    return "Pair(" + first.toString() + ", " + second.toString() + ")";
  }

  /**
   * Getter for first
   * 
   * @return first element in pair
   */
  public FIRST getFirst() {
    return first;
  }

  /**
   * Setter for first
   * 
   * @param first new value for first element
   */
  public void setFirst(FIRST first) {
    this.first = first;
  }

  /**
   * Getter for second element in pair
   * 
   * @return second element in pair
   */
  public SECOND getSecond() {
    return second;
  }

  /**
   * Setter for second
   * 
   * @param second new value for second element
   */
  public void setSecond(SECOND second) {
    this.second = second;
  }
}