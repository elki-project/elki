package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;

/**
 * Class representing selected Database-IDs and/or a selection range.
 * 
 * @author Heidi Kolb
 * @author Erich Schubert
 */
public class RangeSelection extends DBIDSelection {
  /**
   * Selection range
   */
  private DoubleDoublePair[] ranges = null;

  /**
   * Constructor.
   * 
   * @param selectedIds selected IDs
   */
  public RangeSelection(DBIDs selectedIds) {
    super(selectedIds);
  }
  
  /**
   * Constructor.
   * 
   * @param selection
   * @param ranges
   */
  public RangeSelection(DBIDs selection, DoubleDoublePair[] ranges) {
    super(selection);
    this.ranges = ranges;
  }

  /**
   * Get the selection range.
   * 
   * @return Selected range. May be null!
   */
  public DoubleDoublePair[] getRanges() {
    return ranges;
  }

  /**
   * Get a single selection range.
   * 
   * @return Selected range. May be null!
   */
  public DoubleDoublePair getRange(int dim) {
    return ranges[dim];
  }

  /**
   * Update a selection range.
   * Note that to notify listeners, you still need to update the context!
   * 
   * @param dim Dimension
   * @param min Minimum
   * @param max Maximum
   */
  /*public void setRange(int dim, double min, double max) {
    ranges[dim] = new DoubleDoublePair(min, max);
  }*/
}