package de.lmu.ifi.dbs.utilities;

/**
 * Encapsulates the attributes of a pair of object ids.
 */
public class IDPair {
  /**
   * The first id.
   */
  Integer id1;

  /**
   * The second id.
   */
  Integer id2;

  /**
   * Creates a new pair of object ids.
   *
   * @param id1 the first id
   * @param id2 the second id
   */
  public IDPair(Integer id1, Integer id2) {
    this.id1 = id1;
    this.id2 = id2;
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the reference object with which to compare
   * @return <code>true</code> if the obj argument is instance of
   *         IDPair and has the same id values than this object,
   *         <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final IDPair idPair = (IDPair) o;

    if (!id1.equals(idPair.id1)) return false;
    return id2.equals(idPair.id2);

  }

  /**
   * Returns a hash code value for this pair of ids.
   *
   * @return a hash code value for this pair of ids
   */
  public int hashCode() {
    int result;
    result = id1.hashCode();
    result = 29 * result + id2.hashCode();
    return result;
  }

}


