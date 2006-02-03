package de.lmu.ifi.dbs.data;

import java.util.List;
import java.util.ArrayList;

/**
 * MultiRepresentedObject represents a collection of several MetricalObjects of
 * a same superclass.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MultiRepresentedObject<M extends MetricalObject<M>> implements MetricalObject<MultiRepresentedObject<M>> {
  /**
   * Holds the different representations of this object.
   */
  private List<M> representations;

  /**
   * Holds the unique id of this object.
   */
  private Integer id;

  /**
   * Provides a MultiRepresentedObject comprising the specified representations.
   * If representation at index i does not exist, the representations array must return a null value
   * for this index.
   *
   * @param id the id of the object
   * @param representations a aeeay of representations
   */
  public MultiRepresentedObject(Integer id, List<M> representations) {
    this.id = id;
    this.representations = representations;
  }

  /**
   * @see MetricalObject#getID()
   */
  public Integer getID() {
    return id;
  }

  /**
   * @see MetricalObject#setID(Integer)
   */
  public void setID(Integer id) {
    this.id = id;
  }

  /**
   * @see MetricalObject#copy()
   */
  public MultiRepresentedObject<M> copy() {
    List<M> copyRepresentations = new ArrayList<M>(representations.size());
    for (M representation: representations) {
      copyRepresentations.add(representation.copy());
    }
    return new MultiRepresentedObject<M>(id, copyRepresentations);
  }

  /**
   * Returns the ith representation of this object
   *
   * @param i the index of the representation to be retuned
   * @return the ith representation of this object
   */
  public M getRepresentation(int i) {
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
