package experimentalcode.students.goldhofa;

import java.util.ArrayList;
import java.lang.Comparable;

public class SegmentID implements Comparable<SegmentID> {
  
  private static final String SEPARATOR = "-"; 
  
  public ArrayList<Integer> ids;
  
  public SegmentID() {
    
    ids = new ArrayList<Integer>(2);
  }
  
  public SegmentID(int clusterings) {
    
    ids = new ArrayList<Integer>(clusterings);
  }
  
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
