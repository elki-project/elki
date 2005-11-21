package de.lmu.ifi.dbs.distance;

/**
 * An abstract distance implements equals conveniently for any extending class.
 * At the same time any extending class is to implement hashCode properly.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
abstract class AbstractDistance<D extends AbstractDistance> implements Distance<D> {

  /**
   * Any extending class should implement a proper hashCode method.
   *
   * @see Object#hashCode()
   */
  public abstract int hashCode();

  /**
   * Returns true if o is of the same class as this instance
   * and <code>this.compareTo(o)</code> is 0,
   * false otherwise.
   *
   * @see Object#equals(Object)
   */
  public boolean equals(Object o) {
    try {
      return this.compareTo((D) o) == 0;
    }
    catch (ClassCastException e) {
      return false;
    }
  }
}
