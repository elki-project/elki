package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.DatabaseObject;

import java.util.List;

/**
 * Provides a list of database objects and a list of labels associated with these objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ParsingResult<O extends DatabaseObject> {
  /**
   * The list of database objects.
   */
  private final List<O> objects;

  /**
   * The list of labels associated with the database objects.
   */
  private final List<String> labels;

  /**
   * Provides a list of database objects and a list of label obejcts associated with these objects.
   *
   * @param objects the list of database objects
   * @param labels  the list of label objects associated with the database objects
   */
  public ParsingResult(List<O> objects, List<String> labels) {
    this.objects = objects;
    this.labels = labels;
  }

  /**
   * Returns the list of database objects
   *
   * @return the list of database objects
   */
  public List<O> getObjects() {
    return objects;
  }

  /**
   * Returns the list of labels associated with the database objects.
   *
   * @return the list of labels associated with the database objects
   */
  public List<String> getLabels() {
    return labels;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return "objects " + objects + "\nlabels" + labels;
  }
}
