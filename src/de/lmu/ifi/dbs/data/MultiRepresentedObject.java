package de.lmu.ifi.dbs.data;

import java.util.List;
import java.util.ArrayList;

/**
 * MultiRepresentedObject represents a collection of several DatabaseObjects of
 * a same superclass.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MultiRepresentedObject<O extends DatabaseObject<O>> extends AbstractDatabaseObject<MultiRepresentedObject<O>> {
  /**
   * Holds the different representations of this object.
   */
  private List<O> representations;

  /**
   * Provides a MultiRepresentedObject comprising the specified representations.
   * If representation at index i does not exist, the representations array must return a null value
   * for this index.
   *
   * @param representations an array of the representations
   */
  public MultiRepresentedObject(List<O> representations) {
    this.representations = representations;
  }

  /**
   * @see DatabaseObject#copy()
   */
  public MultiRepresentedObject<O> copy() {
    List<O> copyRepresentations = new ArrayList<O>(representations.size());
    for (O representation: representations) {
      copyRepresentations.add(representation.copy());
    }
    return new MultiRepresentedObject<O>(copyRepresentations);
  }

  /**
   * Returns the ith representation of this object
   *
   * @param i the index of the representation to be retuned
   * @return the ith representation of this object
   */
  public O getRepresentation(int i) {
    return representations.get(i);
  }

  /**
   * Returns the number of representations.
   * @return the number of representations
   */
  public int getNumberOfRepresentations() {
    return representations.size();
  }

}
