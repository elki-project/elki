package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.Comparator;

/**
 * Compares Visualizers by level. <br>
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * 
 * @author Remigius Wojdanowski
 * 
 * @apiviz.sterotype comparator
 * @apiviz.uses VisFactory
 */
public class VisualizerComparator implements Comparator<VisFactory<?>> {
  @Override
  public int compare(VisFactory<?> o1, VisFactory<?> o2) {
    // sort by levels first
    Integer level1 = o1.getMetadata().get(VisFactory.META_LEVEL, Integer.class);
    Integer level2 = o2.getMetadata().get(VisFactory.META_LEVEL, Integer.class);
    if(level1 != null && level2 != null && level1 != level2) {
      return level1 - level2;
    }
    // sort by name otherwise.
    String name1 = o1.getMetadata().get(VisFactory.META_NAME, String.class);
    String name2 = o2.getMetadata().get(VisFactory.META_NAME, String.class);
    if(name1 != null && name2 != null && name1 != name2) {
      return name1.compareTo(name2);
    }
    return 0;
  }
}
