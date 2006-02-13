package de.lmu.ifi.dbs.data;

import java.util.ArrayList;
import java.util.List;

/**
 * MultiInstanceObject represents a collection of several MetricalObjects of an equal type.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class MultiInstanceObject<O extends DatabaseObject<O>> extends AbstractDatabaseObject<MultiInstanceObject> {
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

  /**
   * @see DatabaseObject#copy()
   */
  public MultiInstanceObject copy() {
    List<O> copyMembers = new ArrayList<O>(this.members.size());
    for (O member : this.members) {
      copyMembers.add(member.copy());
    }
    return new MultiInstanceObject<O>(copyMembers);
  }
}
