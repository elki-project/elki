package experimentalcode.students.goldhofa;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;


/**
 * Identifies a CircleSegment by its clusterings and cluster. Can be stored as
 * and retrieved from a String.
 * 
 * A segmentID String consists of the cluster id of each clustering, ordered by
 * clustering and separated by a character. Thus a segment ID describes the
 * common pairs in all clusterings and cluster.
 * 
 * i.e. clusteringID 0 & clusterID 2, clusteringID 1 & clusterID 0 => segmentID:
 * 2-0
 */
public class Segment implements Comparable<Segment> {
  private static final String SEPARATOR = "/";

  /**
   * Object is not clustered
   */
  public static final int UNCLUSTERED = -1;

  public DBIDs firstIDs = null;

  public DBIDs secondIDs = null;
  
  public long pairsize = 0;

  // Cluster ids
  protected int[] clusterIds;

  public Segment(int clusterings) {
    clusterIds = new int[clusterings];
  }
  
  public long getPairCount() {
    return pairsize;
  }

  /**
   * Creates a SegmentID by its String representation, extracting clustering and
   * corresponding clusterIDs.
   * 
   * @param segmentID String representation of SegmentID
   */
  public Segment(String segmentID) {
    String[] id = segmentID.split(SEPARATOR);
    clusterIds = new int[id.length];
    for(int i = 0; i < id.length; i++) {
      set(i, Integer.valueOf(id[i]).intValue());
    }
  }

  public Segment(int[] clone) {
    clusterIds = clone;
  }

  public void set(int i, int currentCluster) {
    clusterIds[i] = currentCluster;
  }

  public int get(int idx) {
    return (clusterIds[idx] > 0) ? (clusterIds[idx] - 1) : UNCLUSTERED;
  }

  /**
   * Checks if the segment has a cluster with unpaired objects. Unpaired
   * clusters are represented by "0" (0 = all).
   * 
   * @return
   */
  public boolean isUnpaired() {
    for(int id : clusterIds) {
      if(id < 0) { //== UNCLUSTERED) {
        return true;
      }
    }

    return false;
  }

  /**
   * Check if this segment contains the pairs that are never clustered by any of
   * the clusterings (all 0).
   * 
   * @return
   */
  public boolean isNone() {
    for(int id : clusterIds) {
      if(id > 0) { // != UNCLUSTERED) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns the index of the first clustering having an unpaired cluster, or -1
   * no unpaired cluster exists.
   * 
   * @return clustering id or -1
   */
  public int getUnpairedClusteringIndex() {
    int index = 0;
    for(int id : clusterIds) {
      if(id < 0) { //== UNCLUSTERED) {
        return index;
      }
      index++;
    }

    return -1;
  }

  @Override
  public String toString() {
    StringBuffer string = new StringBuffer();
    for(int id : clusterIds) {
      if (string.length() > 0) {
        string.append(SEPARATOR);
      }
      string.append(id);
    }
    return string.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if(!(Segment.class.isInstance(obj))) {
      return false;
    }

    Segment other = (Segment) obj;
    return Arrays.equals(clusterIds, other.clusterIds);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(clusterIds);
  }

  @Override
  public int compareTo(Segment sid) {
    for(int i = 0; i < clusterIds.length; i++) {
      if(this.clusterIds[i] < sid.clusterIds[i]) {
        return -1;
      }
      else if(this.clusterIds[i] > sid.clusterIds[i]) {
        return 1;
      }
    }
    return 0;
  }
}