package experimentalcode.marisa.index.xtree;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.Entry;
import experimentalcode.marisa.index.xtree.util.SplitHistory;

public interface SplitHistorySpatialEntry extends Entry, SpatialComparable {
  /**
   * Get the split history of this entry's node.
   * 
   * @return the split history of this entry's node
   */
  public SplitHistory getSplitHistory();

  /**
   * Add a dimension to this entry's split history.
   * 
   * @param dimension dimension to be added to split history
   */
  public void addSplitDimension(int dimension);

  /**
   * Set the split history of this entry's node.
   * 
   * @param splitHistory
   */
  public void setSplitHistory(SplitHistory splitHistory);
}
