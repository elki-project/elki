package de.lmu.ifi.dbs.utilities;

import de.lmu.ifi.dbs.distance.Distance;

/**
 * QueryResult holds the id of a database object and its distance to a special
 * query object.
 *
 * @author Elke Achtert 
 */
public class QueryResult<D extends Distance<D>> implements Comparable<QueryResult<D>> {
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
  public int compareTo(QueryResult<D> o) {
    //noinspection unchecked
    int compare = distance.compareTo(o.getDistance());
    if (compare != 0)
    {
        return compare;
    }
    else
    {
      return this.getID() - o.getID();
    }
  }

  /**
   * Returns a string representation of this QueryResult object.
   *
   * @return a string representation of this QueryResult object.
   */
  @Override
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
  @Override
public boolean equals(Object o) {
    if (this == o)
    {
      return true;
    }
    if (o == null || getClass() != o.getClass())
    {
      return false;
    }
    final QueryResult<D> that = (QueryResult<D>) o;

    if (id != that.id)
    {
      return false;
    }
    return distance.equals(that.distance);
  }

  /**
   * Returns a hash code value for this object.
   *
   * @return a hash code value for this object
   */
  @Override
public int hashCode() {
    return id;
  }


}
