package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.MetricalObject;

import java.util.List;

/**
 * Proviedes a list of metrical objects and a list of labels associated with these objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ParsingResult<M extends MetricalObject> {
  /**
   * The list of metrical objects.
   */
  private final List<M> objects;

  /**
   * The list of labels associated with the metrical objects.
   */
  private final List<String> labels;

  /**
   * Proviedes a list of metrical objects and a list of label obejcts associated with these objects.
   *
   * @param objects the list of metrical objects
   * @param labels  the list of label objects associated with the metrical objects
   */
  public ParsingResult(List<M> objects, List<String> labels) {
    this.objects = objects;
    this.labels = labels;
  }

  /**
   * Returns the list of metrical objects
   *
   * @return the list of metrical objects
   */
  public List<M> getObjects() {
    return objects;
  }

  /**
   * Returns the list of labels associated with the metrical objects.
   *
   * @return the list of labels associated with the metrical objects
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
