package de.lmu.ifi.dbs.index;

import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;

/**
 * Abstract superclass for entries in an index structure.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractEntry implements Entry {
  /**
   * Holds the id of the object (node or data object) represented by this entry.
   */
  private int id;

  /**
   * Empty constructor for serialization purposes.
   */
  protected AbstractEntry() {
  }

  /**
   * Provides a new AbstractEntry with the specified id.
   * @param id the id of the object (node or data object) represented by this entry.
   */
  protected AbstractEntry(int id) {
    this.id = id;
  }

  /**
   * Returns the id of the node or data object that is represented by this entry.
   *
   * @return the id of the node or data object that is represented by this entry
   */
  public final Integer getID() {
    return id;
  }

  /**
   * Writes the id of the object (node or data object) that is
   * represented by this entry to the specified stream.
   *
   * @param out the stream to write the object to
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.write(id);
  }

  /**
   * Restore the id of the object (node or data object) that is
   * represented by this entry from the specified stream.
   *
   * @param in the stream to read data from in order to restore the object
   *                                restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.id = in.readInt();
  }
}
