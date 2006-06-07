package de.lmu.ifi.dbs.index;

import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;

/**
 * Default implementation of the entry interface.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultEntry extends AbstractEntry {
  /**
   * True, if this entry is a leaf entry (i.e. this entry represents a data object),
   * false otherwise.
   */
  private boolean isLeafEntry;

  /**
   * Empty constructor for serialization purposes.
   */
  public DefaultEntry() {
    super();
  }

  /**
   * Provides a new DefaultEntry with the specified parameters.
   *
   * @param id          the id of this entry
   * @param isLeafEntry indicates if this entry is a leaf entry
   */
  public DefaultEntry(int id, boolean isLeafEntry) {
    super(id);
    this.isLeafEntry = isLeafEntry;
  }

  /**
   * Returns true if this entry is an entry in a leaf node
   * (i.e. this entry represents a data object),  false otherwise.
   *
   * @return true if this entry is an entry in a leaf node, false otherwise
   */
  public boolean isLeafEntry() {
    return isLeafEntry;
  }

  /**
   * Calls the super method and writes the isLeafEntry flag to the specified stream.
   *
   * @param out the stream to write the object to
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeBoolean(isLeafEntry);
  }

  /**
   * Calls the super method and restores the isLeafEntry flag from the specified stream.
   *
   * @param in the stream to read data from in order to restore the object
   *           restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    isLeafEntry = in.readBoolean();
  }
}
