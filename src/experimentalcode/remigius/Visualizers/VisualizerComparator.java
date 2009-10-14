package experimentalcode.remigius.Visualizers;

import java.util.Comparator;

/**
 * Compares Visualizers by level. <br>
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * 
 * @author Remigius Wojdanowski
 */
public class VisualizerComparator implements Comparator<Visualizer> {
  @Override
  public int compare(Visualizer o1, Visualizer o2) {
    Integer level1 = o1.getMetadata().get(Visualizer.META_LEVEL, Integer.class);
    Integer level2 = o2.getMetadata().get(Visualizer.META_LEVEL, Integer.class);
    if (level1 != null && level2 != null) {
      return level1 - level2;
    }
    return 0;
  }
}
