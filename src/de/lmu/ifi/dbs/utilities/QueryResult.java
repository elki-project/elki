package de.lmu.ifi.dbs.utilities;

import de.lmu.ifi.dbs.distance.Distance;

/**
 * QueryResult holds the id of a database object and its distance to a special
 * query object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class QueryResult<D extends Distance> implements KListEntry<D> {
  /**
   * The id of the underlying database object.
   */
  private final int id;

  /**
   * The distance of the underlying database object to the query object.
   */
  private final D distance;

  /**
   * Creates a new QueryResult object.
   *
   * @param id       the id of the underlying database object
   * @param distance the distance of the underlying database object to the query
   *                 object
   */
  public QueryResult(int id, D distance) {
    this.id = id;
    this.distance = distance;
  }

  public D getKey() {
    return getDistance();
  }

  /**
   * Returns the id of the underlying database object.
   *
   * @return the id of the underlying database object
   */
  public int getID() {
    return id;
  }

  /**
   * Returns the distance of the underlying database object to the query
   * object.
   *
   * @return the distance of the underlying database object to the query
   *         object
   */
  public D getDistance() {
    return distance;
  }



  /**
   * Compares this QueryResult with the given QueryResult with respect to
   * the distances.
   *
   * @see java.lang.Comparable#compareTo(Object)
   */
  public int compareTo(KListEntry<D> o) {
    QueryResult<D> other = (QueryResult<D>) o;
    int compare = distance.compareTo(other.getDistance());
    if (compare != 0) return compare;
    else
      return this.getID() - other.getID();
  }

  /**
   * Returns a string representation of this QueryResult object.
   *
   * @return a string representation of this QueryResult object.
   */
  public String toString() {
    return id + " (" + distance + ")";
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the reference object with which to compare.
   * @return <code>true</code> if this object is the same as the o
   *         argument; <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final QueryResult that = (QueryResult) o;

    if (id != that.id) return false;
    return distance.equals(that.distance);
  }

  /**
   * Returns a hash code value for this object.
   *
   * @return a hash code value for this object
   */
  public int hashCode() {
    int result;
    result = id;
    result = 29 * result + distance.hashCode();
    return result;
  }


}
