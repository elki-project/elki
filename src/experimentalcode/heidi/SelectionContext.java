package experimentalcode.heidi;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

public class SelectionContext {

  /**
   * Selected IDs
   */
  private static ArrayModifiableDBIDs selection = DBIDUtil.newArray();

  /**
   * Selected minimal and maximal values for each dimension
   */
  private static ArrayList<Double> minValues;

  private static ArrayList<Double> maxValues;

  /**
   * Mask to show what dimensions are set in minValues and maxValues
   */
  // TODO: BitSet?
  private static ArrayList<Integer> mask;

public static void init(VisualizerContext context){
  int dim = context.getDatabase().dimensionality();

  minValues = new ArrayList<Double>(dim);
  maxValues = new ArrayList<Double>(dim);
  mask = new ArrayList<Integer>(dim);
  for(int d = 0; d < dim; d++) {
    minValues.add(d, 0.);
    maxValues.add(d, 0.);
    mask.add(d, 0);
  }
}
  /**
   * Getter for the selected IDs
   * 
   * @return ArrayList<Integer>
   */
  public static ArrayModifiableDBIDs getSelection() {
    return selection;
  }
  /**
   * Clears the selection
   * 
   * @param selection
   */
  public static void clearSelection() {
    selection.clear();
  }

  /**
   * Sets the selection, fires a redraw event and requests an redraw of the
   * dot-markers
   * 
   * @param selection
   */
  public static void setSelection(ArrayModifiableDBIDs sel) {
    selection = sel;
  }

  public static ArrayList<Double> getMaxValues() {
    return minValues;
  }

  public static ArrayList<Double> getMinValues() {
    return maxValues;
  }

  public static void setMinValues(ArrayList<Double> minV) {
    minValues = minV;
  }

  public static void setMaxValues(ArrayList<Double> maxV) {
    maxValues = maxV;
  }
  public static void resetMask(VisualizerContext context) {
    mask.clear();
    int dim = context.getDatabase().dimensionality();
    for(int d = 0; d < dim; d++) {
      mask.add(d, 0);
    }
  }

  public static ArrayList<Integer> getMask() {
    return mask;
  }

  public static void setMask(ArrayList<Integer> m) {
    mask = m;
  }

}
