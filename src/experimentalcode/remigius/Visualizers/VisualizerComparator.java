package experimentalcode.remigius.Visualizers;

import java.util.Comparator;

public class VisualizerComparator implements Comparator<Visualizer> {

  @Override
  public int compare(Visualizer o1, Visualizer o2) {
    if(o1.getLevel() < o2.getLevel()) {
      return -1;
    }
    else if(o1.getLevel() > o2.getLevel()) {
      return 1;
    }
    else {
      if(o1.equals(o2)) {
        return 0;
      }
      else {
        return 1;
      }
    }
  }
}
