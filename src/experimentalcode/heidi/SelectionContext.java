package experimentalcode.heidi;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;

/**
 * Class representing selected Database-IDs and/or a selection range.
 * 
 * @author Heidi Kolb
 */
public class SelectionContext {
  /**
   * Selected IDs
   */
  private DBIDs selectedIds = DBIDUtil.EMPTYDBIDS;

  /**
   * Selection range
   */
  private DoubleDoublePair[] ranges = null;

  /**
   * Initializes this context
   * 
   * @param context The context
   */
  public void init(VisualizerContext<? extends DatabaseObject> context) {
    // FIXME: Remove init()?
    //int dim = context.getDatabase().dimensionality();
    //ranges = new DoubleDoublePair[dim];
  }

  // Should be moved to VisualizerContext (as constant
  // VisualizerContext.SELECTION):
  public static final String SELECTION = "selection";

  // Should be moved to VisualizerContext (as context.getSelection()):
  public static SelectionContext getSelection(VisualizerContext<? extends DatabaseObject> context) {
    SelectionContext sel = context.getGenerics(SELECTION, SelectionContext.class);
    // Note: Alternative - but worse semantics.
    // Note: caller should handle null
    // if (sel == null) {
    // sel = new SelectionContext();
    // sel.init(context);
    // }
    return sel;
  }

  // Should be moved to VisualizerContext (as context.setSelection(selection))
  public static void setSelection(VisualizerContext<? extends DatabaseObject> context, SelectionContext selContext) {
    context.put(SELECTION, selContext);
    context.fireContextChange(new SelectionChangedEvent(context));
  }

  /**
   * Getter for the selected IDs
   * 
   * @return DBIDs
   */
  public DBIDs getSelectedIds() {
    return selectedIds;
  }

  /**
   * Clears the selection
   */
  public void clearSelectedIds() {
    selectedIds = DBIDUtil.EMPTYDBIDS;
  }

  /**
   * Sets the selected DBIDs
   * 
   * @param sel The new selection
   */
  public void setSelectedIds(DBIDs sel) {
    selectedIds = sel;
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
   * Set the selection range.
   * 
   * @param ranges New selection range.
   */
  public void setRanges(DoubleDoublePair[] ranges) {
    this.ranges = ranges;
    // TODO: fire selectionChanged event?
  }

  /**
   * Clear the selection ranges.
   */
  public void clearRanges() {
    setRanges(null);
  }
}