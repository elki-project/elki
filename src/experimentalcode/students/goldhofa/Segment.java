package experimentalcode.students.goldhofa;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;


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
  private static final String SEPARATOR = "-";

  // number of segment. if -1 this segment id is a unclustered pair segment,
  // having no corresponding object segment
  private int index = -1;
  
  public int segmentObjects = 0;
  
  public int segmentPairs = 0;
  
  public ModifiableDBIDs objIds = null;

  // Cluster ids
  public int[] ids;

  public void setIndex(int index) {
    this.index = index;
  }

  public int getIndex() {
    return index;
  }

  public Segment(int clusterings) {
    ids = new int[clusterings];
  }

  /**
   * Creates a SegmentID by its String representation, extracting clustering and
   * corresponding clusterIDs.
   * 
   * @param segmentID String representation of SegmentID
   */
  public Segment(String segmentID) {
    String[] id = segmentID.split(SEPARATOR);
    ids = new int[id.length];
    for(int i = 0; i < id.length; i++) {
      set(i, Integer.valueOf(id[i]).intValue());
    }
  }

  public void set(int i, int currentCluster) {
    ids[i] = currentCluster;
  }

  public int get(int idx) {
    return ids[idx];
  }

  public int size() {
    return ids.length;
  }

  /**
   * Checks if the segment has a cluster with unpaired objects. Unpaired
   * clusters are represented by "0" (0 = all).
   * 
   * @return
   */
  public boolean isUnpaired() {
    for(int id : ids) {
      if(id == 0) {
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
    for(int id : ids) {
      if(id != 0) {
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
    for(int id : ids) {
      if(id == 0) {
        return index;
      }
      index++;
    }

    return -1;
  }

  @Override
  public String toString() {
    String string = "";
    for(int id : ids) {
      string += id + SEPARATOR;
    }
    return string.substring(0, string.length() - SEPARATOR.length());
  }

  @Override
  public boolean equals(Object obj) {
    if(!(Segment.class.isInstance(obj))) {
      return false;
    }

    Segment other = (Segment) obj;
    return Arrays.equals(ids, other.ids);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(ids);
  }

  @Override
  public int compareTo(Segment sid) {
    if(this.size() < sid.size()) {
      return -1;
    }
    if(this.size() > sid.size()) {
      return 1;
    }

    for(int i = 0; i < this.size(); i++) {
      if(this.ids[i] < sid.ids[i]) {
        return -1;
      }
      else if(this.ids[i] > sid.ids[i]) {
        return 1;
      }
    }
    return 0;
  }
}