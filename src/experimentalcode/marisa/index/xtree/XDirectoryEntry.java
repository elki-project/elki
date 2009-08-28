package experimentalcode.marisa.index.xtree;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;
import experimentalcode.marisa.index.xtree.util.SplitHistory;

public class XDirectoryEntry extends SpatialDirectoryEntry implements SplitHistorySpatialEntry {

  /**
   * The split history of this entry. Should be set via {@link #splitHistory}
   * and only afterwards queried by {@link #getSplitHistory()} or extended by
   * {@link #addSplitDimension(int)}.
   */
  private SplitHistory splitHistory = null;

  public XDirectoryEntry() {
    super();
  }

  public XDirectoryEntry(int id, HyperBoundingBox mbr) {
    super(id, mbr);
    if(mbr != null) {
      splitHistory = new SplitHistory(mbr.getDimensionality());
    }
  }

  @Override
  public void addSplitDimension(int dimension) {
    splitHistory.setDim(dimension);
  }

  @Override
  public SplitHistory getSplitHistory() {
    return splitHistory;
  }

  @Override
  public void setSplitHistory(SplitHistory splitHistory) {
    this.splitHistory = splitHistory;
  }

  /**
   * Calls the super method and writes the MBR object of this entry to the
   * specified output stream.
   * 
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    splitHistory.writeExternal(out);
  }

  /**
   * Calls the super method and reads the MBR object of this entry from the
   * specified input stream.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.splitHistory = SplitHistory.readExternal(in);
  }

}
