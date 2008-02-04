package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.DatabaseObject;

import java.util.List;

/**
 * Provides a single database objects and a list of labels associated with this object.
 *
 * @author Elke Achtert 
 */
public class ObjectAndLabels<O extends DatabaseObject> {
  /**
   * The database object.
   */
  private final O object;

  /**
   * The list of labels associated with the database objects.
   */
  private final List<String> labels;
  

  /**
   * Provides a single database object and a list of labels associated with this object.
   * 
   * 
   * @param object the database object
   * @param labels the list of string labels associated with this object
   *
   */
  public ObjectAndLabels(O object, List<String> labels)
  {
    this.object = object;
    this.labels = labels;
  }

  /**
   * Returns the database object.
   * @return the database object
   */
  public O getObject() {
    return object;
  }

  /**
   * Returns the list of string labels associated with the database object.
   * @return the list of string labels associated with the database object
   */
  public List<String> getLabels() {
    return labels;
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
//    return labels.toString();
    return object.toString() + " " + labels.toString();
  }
}
