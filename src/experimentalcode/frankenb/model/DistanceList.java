/**
 * 
 */
package experimentalcode.frankenb.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

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
  
  private List<Pair<Integer, Double>> distances = new ArrayList<Pair<Integer, Double>>();
  private int id;
  
  private boolean sorted = false; 
  
  public DistanceList(int id) {
    this.id = id;
  }
  
  public void addDistance(int otherId, double distance) {
    distances.add(new Pair<Integer, Double>(otherId, distance));
    this.sorted = false;
  }
  
  public void sort() {
    if (sorted) return;
    
    Collections.sort(distances, COMPARATOR);
    this.sorted = true;
  }
  
  /**
   * @param sorted the sorted to set
   */
  protected void setSorted(boolean sorted) {
    this.sorted = sorted;
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
  protected List<Pair<Integer, Double>> getDistances() {
    return this.distances;
  }
  
  /**
   * @return the sorted
   */
  public boolean isSorted() {
    return this.sorted;
  }
  
  /**
   * Adds distances of the other list until the k nearest neighbor. If
   * this list already contains more than the k nearest neighbors it will be
   * trimmed to the k nearest neigbors as a side effect of this method call.
   * 
   * @param other
   * @param k
   */
  public void addAll(DistanceList other, int k) {
    if (this.distances.size() == 0 && other.distances.size() == 0) return;
    
    this.sort();
    other.sort();

    int thisPos = 0;
    int otherPos = 0;
    
    do {
      Pair<Integer, Double> thisItem = (thisPos >= this.distances.size() ? new Pair<Integer, Double>(0, Double.MAX_VALUE) : this.distances.get(thisPos));
      Pair<Integer, Double> otherItem = (otherPos >= other.distances.size() ? new Pair<Integer, Double>(0, Double.MAX_VALUE) : other.distances.get(otherPos));
      
      System.out.println(thisItem + " vs " + otherItem);
      
      if (thisItem.first == otherItem.first) {
        thisPos++;
        otherPos++;
      } else
      if (thisItem.second < otherItem.second) {
        thisPos++;
      } else 
      if (thisItem.second >= otherItem.second) {
        if (thisPos >= k - 1) {
          if (!Double.valueOf(thisItem.second).equals(otherItem.second)) break;
        }
        
        if (thisPos > this.distances.size() - 1) {
          this.distances.add(otherItem);
        } else {
          this.distances.add(thisPos, otherItem);
        }
        thisPos++;
        otherPos++;
      }
      
    } while (thisPos < this.distances.size() || otherPos < other.distances.size());
    
    if (this.distances.size() > k) {
      trim(k);
    }
    
  }
  
  private void trim(int k) {
    if (this.distances.size() < k) return;
    
    double lastDistance = this.distances.get(k - 1).second;
    int trimFrom = this.distances.size() - 1;
    
    for (int i = k; i < this.distances.size(); ++i) {
      if (lastDistance != this.distances.get(i).second) {
        // if the top most distance is not equal the last one we have found
        // our knn condition and can trim the rest of the collection
        trimFrom = i;
        break;
      }
      lastDistance = this.distances.get(i).second;
      trimFrom = i;
    }
    
    System.out.println("Removing from " + trimFrom + " to " + (this.distances.size()-1));
    int itemsToRemove = (this.distances.size() - trimFrom);
    for (int j = 0; j < itemsToRemove; ++j) {
      this.distances.remove(this.distances.size() - 1);
    }
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
