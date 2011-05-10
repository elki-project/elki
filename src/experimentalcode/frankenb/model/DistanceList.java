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
      int result = o1.getSecond().compareTo(o2.getSecond());
      if (result == 0) {
        result = o1.getFirst().compareTo(o2.getFirst());
      }
      return result;
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
  
  public Pair<Integer, Double> getLast() {
    return distances.last();
  }
  
  public Pair<Integer, Double> getFirst() {
    return distances.first();
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
   * trimmed to the k nearest neighbors as a side effect of this method call.
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

    Pair<Integer, Double> item = null;
    int counter = 0;
    int maxSteps = this.distances.size() - k;
    List<Pair<Integer, Double>> toRemove = new ArrayList<Pair<Integer, Double>>();
    for (Iterator<Pair<Integer, Double>> descendingIterator = this.distances.descendingIterator(); descendingIterator.hasNext() && counter <= maxSteps;) {
      item = descendingIterator.next();
      
      if (counter++ == maxSteps) {
        List<Pair<Integer, Double>> dontRemove = new ArrayList<Pair<Integer, Double>>();
        for (Pair<Integer, Double> aItem : toRemove) {
          if (aItem.second.equals(item.second)) {
            dontRemove.add(aItem);
          }
        }
        toRemove.removeAll(dontRemove);
      } else {
        toRemove.add(item);
      }
    }
    
    this.distances.removeAll(toRemove);
  }
  
  public int getSize() {
    return this.distances.size();
  }

  @Override
  public Iterator<Pair<Integer, Double>> iterator() {
    return distances.iterator();
  }
  
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
