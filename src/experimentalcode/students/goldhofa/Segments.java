package experimentalcode.students.goldhofa;

import java.util.TreeMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;


/**
 * 
 * @author goldhofer
 *
 */
public class Segments {
  
  /**
   * if segments are converted into pairSegments
   */
  private Boolean isPairSegments = false;
  
  /**
   * Clusterings
   */
  private List<Clustering<?>> clusterings;
  
  /**
   * Number of clusterings in comparison
   */
  private int clusteringsCount;
  
  /**
   * Number of Clusters for each clustering
   */
  private int[] clusters;
  
  /**
   * Total number of pairs
   */
  private int pairs;
  
  /**
   * List of object based segments
   */
  private TreeMap<SegmentID, Integer> objectSegments;
  
  /**
   * List of pair based segments
   */
  private TreeMap<SegmentID, Integer> pairSegments;
  
  /**
   * List of pair segments and their invovled object segments (helper)
   */
  private TreeMap<SegmentID, SortedSet<SegmentID>> fragmentedSegments;
  
  /**
   * Paircount of clusters by clusterings
   */
  private ArrayList<TreeMap<Integer, Integer>> clusterPaircount;
  
  
  /**
   * 
   * @param clusterings   List of clusterings in comparison
   */
  public Segments(List<Clustering<?>> clusterings) {
    
    this.clusterings = clusterings;
    
    objectSegments = new TreeMap<SegmentID, Integer>();
    fragmentedSegments = new TreeMap<SegmentID, SortedSet<SegmentID>>();
    pairSegments = new TreeMap<SegmentID, Integer>();
    
    this.clusteringsCount = clusterings.size();
    clusters = new int[this.clusteringsCount];
    
    clusterPaircount = new ArrayList<TreeMap<Integer, Integer>>(this.clusteringsCount);
    
    // save count of clusters
    int clusteringIndex = 0;
    for (Clustering<?> clr : clusterings) {
      
      clusterPaircount.add(new TreeMap<Integer, Integer>());
      
      clusters[clusteringIndex] = clr.getAllClusters().size();     
      clusteringIndex++;
    }
  }
  
  /**
   * Add new Object to Segments
   * 
   * @param tag   occurrence in clusterings
   * @param id    object id in DB
   */
  public void addObject(DBID objectID) {
    
    SegmentID tag = getSegmentID(objectID);
    
    if (objectSegments.containsKey(tag)) {
      
      objectSegments.put(tag, objectSegments.get(tag)+1);
      
    } else {
      
      objectSegments.put(tag, 1);
    }
  }
  
  public DBIDs getDBIDs(SegmentID id) {
    
    ModifiableDBIDs objectIDs = DBIDUtil.newHashSet();
    DBIDs currentIDs;
    
    int startClustering = 0;
    boolean found = false;
    while (found == false) {
      
      if (id.get(startClustering) != 0) {
        found = true;
      } else {        
        startClustering++;
      }
    }
    
    currentIDs = clusterings.get(startClustering).getAllClusters().get(id.get(startClustering)-1).getIDs();

    for (DBID dbID : currentIDs) {
      objectIDs.add(dbID);
    }
    
    for (int i=startClustering+1; i<id.size(); i++) {
      
      if (id.get(i) != 0) {        
        
        intersect( objectIDs, i, id.get(i)-1 );
        
      }/* else {
        
        substract(objectIDs, i, id.get(i));
      }*/
    }
    
    return objectIDs;
  }
  
  private DBIDs intersect(DBIDs ids, int clusteringID, int clusterID) {
    
    ModifiableDBIDs intersection = DBIDUtil.newHashSet();
    
    DBIDs currentIDs = clusterings.get(clusteringID).getAllClusters().get(clusterID).getIDs();
   
    for (DBID currentID : currentIDs) {
      
      if (ids.contains(currentID)) {
        intersection.add(currentID);
      }
    }
    
    return intersection;
  }
  
  /*
  private DBIDs substract(DBIDs ids, int clusteringID, int clusterID) {
    
    ModifiableDBIDs substraction = DBIDUtil.newHashSet();
    
    // entferne alle objekte in denen zwei objekte in einem cluster vorkommen
    
    DBIDs currentIDs = clusterings.get(clusteringID).getAllClusters().get(0).getIDs();
    /*
    for (DBID id : ids) {
    
        if (currentIDs.contains(id)) System.out.println("found");
        else System.out.println("        not found");
    }
/*   
    for (DBID currentID : currentIDs) {
      
      if ( ! ids.contains(currentID.getIntegerID())) {
        substraction.add(currentID.getIntegerID());
      }
    }
 
    return ids;
  }
  */
  
  public SegmentID getSegmentID(DBID objectID) {
    
    SegmentID result = new SegmentID();
    getSegment(objectID, result, 0);
    
    return result;
  }
  
  private void getSegment(DBID objectID, SegmentID tag, int clusteringIndex) {
    
    if (clusteringIndex < clusterings.size()) {
      
      // search object in clusters of this clustering
      
      int currentCluster = 1;
      Boolean objectFound = false;
      
      // TODO Abbruch wenn Objekt gefunden (while-Schleife)
      for (Cluster<?> cluster : clusterings.get(clusteringIndex).getAllClusters()) {
         
        // if object in this cluster
        
        if (cluster.getIDs().contains(objectID)) {
          
          // search in next clusterings
          
          objectFound = true;
          getSegment(objectID, tag.add(currentCluster), clusteringIndex+1);
        }
         
         currentCluster++;
      }
      
      // if object is unclustered in this clustering, tag accordingly and search in next clusterings
      if ( ! objectFound) {
        
        getSegment(objectID, tag.add(0), clusteringIndex+1);
      }
    }
  }
  
  /**
   * Converts segments of added objects into pair segmentation
   */
  public void convertToPairSegments() {
    
    if (isPairSegments) return;

    // create all pair segments
    createFragmentedSegments();
    
    // All Cluster Pairs    
    SegmentID tagID = getFragmentedSegment( (SortedSet<SegmentID>) objectSegments.keySet());
    fragmentedSegments.put(tagID,(SortedSet<SegmentID>) objectSegments.keySet());

    // add all objectSegments with their pairs
    for (SegmentID segment : objectSegments.keySet()) {
      
      int pairs = asPairs(objectSegments.get(segment));
      pairSegments.put(segment, pairs);
      
      // cluster pair count
      addPairsToCluster(segment, pairs);
      
      // total pairs count
      this.pairs += pairs;
    }
    
    // getPaircount of new segments
    calculatePairs();
    
    isPairSegments = true;
    
    
    // CHECK
    /*
    for (SegmentID id : fragmentedSegments.keySet()) {
      
      
      System.out.println("");
      System.out.println(id);
      System.out.println("--");
      
      for (SegmentID involved : fragmentedSegments.get(id)) {
        
        System.out.println(involved);
      }
      
      System.out.println("--");      
    }
    */
  }
  
  
  /**
   * Calculates pairs of pair segments
   */
  private void calculatePairs() {
    
    // calculated Segments
    ArrayList<String> processedSegments = new ArrayList<String>();
    
    // for all new segments
    for (SegmentID segment : fragmentedSegments.descendingKeySet()) {
      
      // segments of current pairSegment
      SortedSet<SegmentID> currentSegments = fragmentedSegments.get(segment);
      
      // Paircount of the segment
      int pairs = getPaircount(fragmentedSegments.get(segment));
      
      // as Array
      SegmentID[] current = new SegmentID[currentSegments.size()];
      int index=0;
      for (SegmentID s : currentSegments) {
        current[index] = s;
        index++;
      }
      
      // Find Pairs that are already counted and
      // substract them from paircount
      for (int i=0; i<current.length-1; i++) {        
        for (int k=i+1; k<current.length; k++) {
          
          String id = current[i].toString()+current[k].toString();
          
          if (processedSegments.contains(id)) {
            
            int seg1 = objectSegments.get(current[i]);
            int seg2 = objectSegments.get(current[k]);
            
            pairs -= (asPairs(seg1+seg2) - (asPairs(seg1)+asPairs(seg2)));
            
          } else {
            
            // Add Segments to processed Segments
            processedSegments.add(id);
          }
        }
      }
      
      pairSegments.put(segment, pairs);
      
      // cluster pair count
      addPairsToCluster(segment, pairs);
      
      // total pairs count
      this.pairs += pairs;
    }
  }
  
  private void addPairsToCluster(SegmentID segment, int pairs) {
    
    for (int i=0; i<segment.size(); i++) {
      
      int cluster = segment.get(i);
      if (cluster != 0) {
        
        if (clusterPaircount.get(i).containsKey(cluster)) {
          
          int current = clusterPaircount.get(i).get(cluster);
          clusterPaircount.get(i).put(cluster, current+pairs);  
          
        } else {
          
          clusterPaircount.get(i).put(cluster, pairs);
        }
      }
    }    
  }
  
  
  /**
   * Get pairs of object segments
   * 
   * @param segments  List of segments to pair
   * @return pair count of segments
   */
  private int getPaircount(Set<SegmentID> segments) {
    
    int totalObjects = 0;
    int segmentPairs = 0;
    for (SegmentID segment : segments) {
      
      int objects = this.objectSegments.get(segment);
      totalObjects += objects;
      segmentPairs += asPairs(objects); 
    }
    
    return asPairs(totalObjects) - segmentPairs;
  }
  
  
  /**
   * Build new segments depending on pairs
   */
  private void createFragmentedSegments() {
    
    // for every clustering
    for (int clr=0; clr<clusteringsCount; clr++) {
      
      // and for every cluster
      for (int c=0; c<clusters[clr]; c++) {
        
        // get common segments
        SortedSet<SegmentID> commonSegments = getObjectSegments(clr, c+1);
        
        if (commonSegments.size() > 1) {
          
          SegmentID tag = getFragmentedSegment(commonSegments);          
          fragmentedSegments.put(tag, commonSegments);
          
          // find segments that have more cluster in common
          createFragmentedSegments(clr+1, clusteringsCount-1, commonSegments);
        }
      }  
    }    
  }
  
  private void createFragmentedSegments(int fromThisClustering, int clustersToFind, Set<SegmentID> segments) {
    
    if (clustersToFind == 0) return;
    
    // for next clusterings
    for (int currentClustering=fromThisClustering; currentClustering<clusteringsCount; currentClustering++) {
      
      // search cluster
      for(int c=0; c<clusters[currentClustering]; c++) {

        SortedSet<SegmentID> match = new TreeSet<SegmentID>();
        
        // get all segments that match cluster
        for (SegmentID segment: segments) {
          
          if (segment.get(currentClustering) == (c+1)) match.add(segment);
        }
        
        if (match.size() > 1) {
          
          SegmentID tag = getFragmentedSegment(match);          
          fragmentedSegments.put(tag, match);
          
          // Find an additional match
          createFragmentedSegments(currentClustering+1, clustersToFind-1, match);
        }
      }
    }
  }
  
  
  /**
   * Builds new Segment on basis of involved object segments
   * 
   * @param segments  object segments participating in new pair segment
   * @return segment id
   */
  private SegmentID getFragmentedSegment(SortedSet<SegmentID> segments) {
    
    SegmentID pairSegment = new SegmentID();
    
    for (int i=0; i<clusteringsCount; i++) {
      
      int currentCluster = -1;
      
      for (SegmentID segment : segments) {
        
        if (currentCluster == -1) currentCluster = segment.get(i);
        else {
          
          if (segment.get(i) != currentCluster ) {
            currentCluster = 0;            
          }          
        }        
      }
      
      pairSegment.add(currentCluster);
    }
    
    return pairSegment;
  }
  
  
  /**
   * Returns all specified segments
   * 
   * @param clustering  clustering to search for
   * @param clusterID   cluster to find
   * @return list of matching segments
   */
  private SortedSet<SegmentID> getObjectSegments(int clustering, int clusterID) {
    
    SortedSet<SegmentID> matchedSegments = new TreeSet<SegmentID>();

    for (SegmentID tag : objectSegments.keySet()) {
      
      if (tag.get(clustering) == clusterID) matchedSegments.add(tag);
    }
    
    return matchedSegments;    
  }
  
  
  /**
   * Calculates pair count of a set of objects
   * 
   * @param objectCount   count of objects
   * @return pair count
   */
  private int asPairs(int objectCount) { return objectCount*(objectCount-1)/2; }
  
  
  /**
   * get size of object segments or pair segments if calculated
   * 
   * @return size of segments
   */
  public int size() {
    
    if (isPairSegments) return pairSegments.size();
    else return objectSegments.size();
  }
  
  public int getPairCount() {
    
    return pairs;
  }
  
  public int getPairCount(int clustering, int cluster) {
    
    return clusterPaircount.get(clustering).get(cluster);
  }
  
  public TreeMap<SegmentID, Integer> getSegments() {
    
    return pairSegments;
  }
  
  public int getClusterings() {
    
    return clusteringsCount;
  }
  
  public int[] getClusters() {
    
    return clusters;
  }
  
  /**
   * Return the sum of all clusters
   * 
   * @return
   */
  public int getTotalClusterCount() {
    
    int clusterCount = 0;
    
    for (int i=0; i<clusters.length; i++) {
      
      clusterCount += clusters[i];
    }
    
    return clusterCount;
  }
  
  /**
   * Returns the highest number of Clusters in the clusterings
   * 
   * @return
   */
  public int getHighestClusterCount() {
    
    int maxClusters = 0;
    
    for (int i=0; i<clusters.length; i++) {
      
      if (maxClusters < clusters[i]) maxClusters = clusters[i];
    }
    
    return maxClusters;
  }
  
  public void print(Logging logger) {
    
    logger.verbose("");
    logger.verbose("Object Segments");
    logger.verbose("---");
    
    int totalCount = 0;
    for (SegmentID key : objectSegments.keySet()) {
      
      totalCount += objectSegments.get(key);
      logger.verbose(key.toString() +": "+objectSegments.get(key)); 
    }
    logger.verbose("----------------------------------");
    logger.verbose("sum: "+totalCount+" objects");
    
    
    int totalPairs = asPairs(totalCount);
    totalCount = 0;
    if (isPairSegments) {
      
      logger.verbose("");
      logger.verbose("Pair Segments");
      logger.verbose("---");
      
      for (SegmentID key : pairSegments.keySet()) {
        
        totalCount += pairSegments.get(key);
        logger.verbose(key +": "+pairSegments.get(key)); 
      }
      logger.verbose("----------------------------------");
      logger.verbose("sum: "+this.pairs+" of "+totalPairs+" pairs");
    }
  }
}
