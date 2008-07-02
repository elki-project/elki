package de.lmu.ifi.dbs.elki.data;

import java.util.ArrayList;
import java.util.List;

/**
 * MultiInstanceObject represents a collection of several DatabaseObjects of an equal type.
 *
 * @author Arthur Zimek
 */
public class MultiInstanceObject<O extends DatabaseObject> extends AbstractDatabaseObject {
  /**
   * Holds the members of this MultiInstanceObject.
   */
  private List<O> members;

  /**
   * Provides a MultiInstanceObject comprising the specified members.
   *
   * @param members a list of members - the references of the members
   *                are kept as given, but in a new list
   */
  public MultiInstanceObject(List<O> members) {
    this.members = new ArrayList<O>(members.size());
    this.members.addAll(members);
  }
}
