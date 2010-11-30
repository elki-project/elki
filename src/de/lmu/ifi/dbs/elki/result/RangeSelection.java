package de.lmu.ifi.dbs.elki.result;

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
}