/**
 * 
 */
package experimentalcode.frankenb.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class DistanceList implements Iterable<Pair<Integer, Double>> {

  private static final Comparator<Pair<Integer, Double>> COMPARATOR = new Comparator<Pair<Integer, Double>>() {

    @Override
    public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
      return o1.getSecond().compareTo(o2.getSecond());
    }
    
  };
  
  private TreeSet<Pair<Integer, Double>> distances = new TreeSet<Pair<Integer, Double>>(COMPARATOR);
  private Set<Integer> containedIds = new HashSet<Integer>(); 
  
  private int id;
  private int k;
  
  public DistanceList(int id, int k) {
    this.id = id;
    this.k = k;
  }
  
  /**
   * @return the k
   */
  public int getK() {
    return this.k;
  }
  
  public void addDistance(int otherId, double distance) {
    if (containedIds.contains(otherId)) return;
    distances.add(new Pair<Integer, Double>(otherId, distance));
    containedIds.add(otherId);
    trim();
  }
  
  /**
   * @return the id
   */
  public int getId() {
    return this.id;
  }
  
  /**
   * @return the distances
   */
  protected SortedSet<Pair<Integer, Double>> getDistances() {
    return this.distances;
  }
  
  /**
   * Adds distances of the other list until the k nearest neighbor. If
   * this list already contains more than the k nearest neighbors it will be
   * trimmed to the k nearest neigbors as a side effect of this method call.
   * 
   * @param other
   * @param k
   */
  public void addAll(DistanceList other) {
    if (this.distances.size() == 0 && other.distances.size() == 0) return;
    this.distances.addAll(other.distances);
    
    trim();
  }
  
  private void trim() {
    
    if (this.distances.size() <= k) return;

    List<Pair<Integer, Double>> tmpList = new ArrayList<Pair<Integer, Double>>();
    Pair<Integer, Double> lastDistance = new Pair<Integer, Double>(0, Double.MAX_VALUE);
    int times = (this.distances.size() - k) + 1;
    for (int i = 0; i < times; ++i) {
      Pair<Integer, Double> acDistance = this.distances.last();
      if (acDistance.second != lastDistance.second) {
        tmpList.clear();
      }
      this.distances.remove(acDistance);
      tmpList.add(acDistance);
    }
    
    this.distances.addAll(tmpList);
    
  }
  
  public int getSize() {
    return this.distances.size();
  }

  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<Pair<Integer, Double>> iterator() {
    return distances.iterator();
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (Pair<Integer, Double> distance : this.distances) {
      sb.append(distance.toString());
      sb.append("\n");
    }
    return sb.toString();
  }
  
}
