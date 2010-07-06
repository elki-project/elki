package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.util.Comparator;

import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;

/**
 * Compares Visualizers by level. <br>
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * 
 * @author Remigius Wojdanowski
 */
public class VisualizationInfoComparator implements Comparator<VisualizationInfo> {
  @Override
  public int compare(VisualizationInfo o1, VisualizationInfo o2) {
    // sort by levels first
    Integer level1 = o1.getVisualizer().getMetadata().get(Visualizer.META_LEVEL, Integer.class);
    Integer level2 = o2.getVisualizer().getMetadata().get(Visualizer.META_LEVEL, Integer.class);
    if(level1 != null && level2 != null && level1 != level2) {
      return level1 - level2;
    }
    // sort by name otherwise.
    String name1 = o1.getVisualizer().getMetadata().get(Visualizer.META_NAME, String.class);
    String name2 = o2.getVisualizer().getMetadata().get(Visualizer.META_NAME, String.class);
    if(name1 != null && name2 != null && name1 != name2) {
      return name1.compareTo(name2);
    }
    return 0;
  }
}
