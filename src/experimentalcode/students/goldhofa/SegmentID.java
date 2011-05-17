package experimentalcode.students.goldhofa;

import java.util.ArrayList;
import java.lang.Comparable;

/**
 * Identifies a CircleSegment by its clusterings and cluster. Can be stored as and
 * retrieved from a String.
 * 
 * A segmentID String consists of the cluster id of each clustering, ordered by clustering
 * and separated by a character. Thus a segment ID describes the common pairs in all
 * clusterings and cluster.
 * 
 * i.e. clusteringID 0 & clusterID 2, clusteringID 1 & clusterID 0 => segmentID: 2-0
 */
public class SegmentID implements Comparable<SegmentID> {
  
  private static final String SEPARATOR = "-"; 
  
  public ArrayList<Integer> ids;
  
  public SegmentID() {
    
    ids = new ArrayList<Integer>(2);
  }
  
  public SegmentID(int clusterings) {
    
    ids = new ArrayList<Integer>(clusterings);
  }
  
  /**
   * Creates a SegmentID by its String representation, extracting
   * clustering and corresponding clusterIDs.
   * 
   * @param segmentID   String representation of SegmentID
   */
  public SegmentID(String segmentID) {
    
    String[] id = segmentID.split(SEPARATOR);
    
    ids = new ArrayList<Integer>(id.length);
    
    for (int i=0; i<id.length; i++) {
      
      add(Integer.valueOf(id[i]).intValue());
    }
  }
  
  public SegmentID add(int nextClusteringCluster) {
    
    ids.add(nextClusteringCluster);
    
    return this;
  }
  
  public int size() {
    
    return ids.size();
  }
  
  /**
   * Checks if the segment has a cluster with unpaired objects.
   * Unpaired clusters are represented by "0" (0 = all).
   * 
   * @return
   */
  public boolean isUnpaired() {
    
    for (int id: ids) {
      if (id == 0) return true;
    }
    
    return false;
  }
  
  /**
   * Returns the index of the first clustering having an unpaired cluster,
   * or -1 no unpaired cluster exists.  
   * 
   * @return  clustering id or -1
   */
  public int getUnpairedClusteringIndex() {
    
    int index = 0;
    for (int id: ids) {
      if (id == 0) return index;
      index++;
    }
    
    return -1;
  }
  
  /**
   * Get the cluster IDs of the segment as array. index representing clustering.
   * 
   * @return IDs of cluster by clustering
   */
  public ArrayList<Integer> getIDs() {
    
    return this.ids;
  }
  
  public int get(int clusteringsIndex) {
    
    return ids.get(clusteringsIndex);
  }
  
  public int get(String clusteringsIndex) {
    
    return ids.get(Integer.valueOf(clusteringsIndex).intValue());
  }
  
  @Override
  public String toString() {
    
    String string = "";
    
    for (Integer id : ids) {
      string += id+SEPARATOR;
    }
    
    return string.substring(0, string.length()-SEPARATOR.length());
  }
  
  @Override
  public boolean equals(Object obj) {
    
    if(!(SegmentID.class.isInstance(obj))) {
      
      return false;
    }
    
    SegmentID other = (SegmentID) obj;
        
    if (this.compareTo(other) == 0) {
      
      return true;
      
    } else {
     
      return false;
    }
  }

  // TODO performance?
  @Override
  public int hashCode() {

    int result = 0;
    for (Integer id : ids) {
      result += id.hashCode();
    }

    return result;
  }
  
  @Override
  public int compareTo(SegmentID sid) {
    
    if (this.size() < sid.size()) {
      return -1;
    }
    if (this.size() > sid.size()) {
      return 1;
    }
    
    for (int i=0; i<this.size(); i++) {
      
      if (this.get(i) < sid.ids.get(i)) {
        
        return -1;
        
      } else if (this.get(i) > sid.ids.get(i)) {
        
        return 1;
      }
    }
    
    return 0;
  }
}
