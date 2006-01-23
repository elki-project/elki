package de.lmu.ifi.dbs.data;

import java.util.ArrayList;
import java.util.List;

/**
 * MultiRepresentedObject represents a collection of several MetricalObjects of arbitrary type.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class MultiRepresentedObject<M extends MetricalObject<M>> implements MetricalObject<MultiRepresentedObject> {
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
   *
   * @param representations a list of representations - the references of the representations
   *                        are kept as given, but in a new list
   */
  public MultiRepresentedObject(List<M> representations) {
    this.representations = new ArrayList<M>(representations.size());
    this.representations.addAll(representations);
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
  public MultiRepresentedObject copy() {
    List<M> copyRepresentations = new ArrayList<M>(this.representations.size());
    for (M member : this.representations) {
      copyRepresentations.add(member.copy());
    }
    return new MultiRepresentedObject<M>(copyRepresentations);
  }

  /**
   * Returns the ith representation of this object
   * @param i the index of the representation to be retuned
   * @return the ith representation of this object
   */
  public M getRepresentation(int i) {
    return representations.get(i);
  }

}
