package experimentalcode.heidi;

import java.util.ArrayList;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;

public class SelectionContext {

  /**
   * Selected IDs
   */
  private ArrayModifiableDBIDs selection = DBIDUtil.newArray();

  /**
   * Selected minimal and maximal values for each dimension
   */
  private ArrayList<Double> minValues;

  private ArrayList<Double> maxValues;

  /**
   * Mask to show what dimensions are set in minValues and maxValues
   */
  // TODO: BitSet?
  private BitSet mask;

  public void init(VisualizerContext<?> context) {
    int dim = context.getDatabase().dimensionality();

    minValues = new ArrayList<Double>(dim);
    maxValues = new ArrayList<Double>(dim);
    mask = new BitSet(dim);
    for(int d = 0; d < dim; d++) {
      minValues.add(d, 0.);
      maxValues.add(d, 0.);
      mask.set(d, false);
    }
  }

  // Should be moved to VisualizerContext (as constant
  // VisualizerContext.SELECTION):
  public static final String SELECTION = "selection";

  // Should be moved to VisualizerContext (as context.getSelection()):
  public static SelectionContext getSelection(VisualizerContext<?> context) {
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
  public static void setSelection(VisualizerContext<?> context, SelectionContext selContext) {
    context.put(SELECTION, selContext);
    context.fireContextChange(new SelectionChangedEvent(context));
  }

  /**
   * Getter for the selected IDs
   * 
   * @return ArrayList<Integer>
   */
  public ArrayModifiableDBIDs getSelection() {
    return selection;
  }

  /**
   * Clears the selection
   * 
   * @param selection
   */
  public void clearSelection() {
    selection.clear();

  }

  /**
   * Sets the selected DBIDs
   * 
   * @param selection
   */
  public void setSelection(ArrayModifiableDBIDs sel) {
    selection = sel;

  }

  public ArrayList<Double> getMaxValues() {
    return minValues;
  }

  public ArrayList<Double> getMinValues() {
    return maxValues;
  }

  public void setMinValues(ArrayList<Double> minV) {
    minValues = minV;
  }

  public void setMaxValues(ArrayList<Double> maxV) {
    maxValues = maxV;
  }

  public void resetMask(VisualizerContext<?> context) {
    mask.clear();
    int dim = context.getDatabase().dimensionality();
    for(int d = 0; d < dim; d++) {
      mask.set(d, false);
    }
  }

  public BitSet getMask() {
    return mask;
  }

  public void setMask(BitSet m) {
    mask = m;
  }

}
