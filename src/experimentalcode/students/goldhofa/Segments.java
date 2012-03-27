package experimentalcode.students.goldhofa;

import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;

/**
 * Creates segments of two or more clusterings.
 * 
 * <p>
 * Segments are the equally paired database objects of all given (2+)
 * clusterings. Given a contingency table, an object Segment represents the
 * table's cells where an intersection of classes and labels are given. Pair
 * Segments are created by converting an object Segment into its pair
 * representation. Converting all object Segments into pair Segments results in
 * a larger number of pair Segments, if any fragmentation (no perfect match of
 * clusters) within the contingency table has occurred (multiple cells on one
 * row or column). Thus for ever object Segment exists a corresponding pair
 * Segment. Additionally pair Segments represent pairs that are only in one
 * Clustering which occurs for each split of a clusterings cluster by another
 * clustering. Here, these pair Segments are referenced as fragmented Segments.
 * Within the visualization they describe (at least two) pair Segments that have
 * a corresponding object Segment.
 * </p>
 * <p>
 * Objects are combined by a string (segmentID), so no database objects have to
 * be saved. <br />
 * EDIT: extending the visualization now stores all DBIDs described by their
 * objectSegment for a faster selection of objects on the cost of memory.
 * </p>
 * <p>
 * Segments are created by adding each DB object via the <i>addObject()</i>
 * method and afterwards converted to pairs by <i>convertToPairSegments()</i>.
 * </p>
 * 
 * @author Sascha Goldhofer
 */
public class Segments {
  /**
   * Clusterings
   */
  private List<Clustering<?>> clusterings;

  /**
   * Clusters
   */
  private List<List<Cluster<Model>>> clusters;

  /**
   * Number of clusterings in comparison
   */
  private int clusteringsCount;

  /**
   * Number of Clusters for each clustering
   */
  private int[] numclusters;

  /**
   * Total number of objects
   */
  private int totalObjects;

  /**
   * Total number of pairs
   */
  private int totalPairs;

  /**
   * The actual segments
   */
  private TreeMap<Segment, Segment> segments;

  /**
   * List of pair segments and their involved object segments (helper)
   */
  private TreeMap<Segment, SortedSet<Segment>> fragmentedSegments;

  /**
   * Paircount of clusters by clusterings
   */
  private ArrayList<TIntIntHashMap> clusterPaircount;

  /**
   * Initialize segments. Add DB objects via addObject method.
   * 
   * @param clusterings List of clusterings in comparison
   * @param baseResult used to retrieve db objects
   */
  public Segments(List<Clustering<?>> clusterings, HierarchicalResult baseResult) {
    super();
    this.clusterings = clusterings;
    this.clusteringsCount = clusterings.size();
    segments = new TreeMap<Segment, Segment>();
    fragmentedSegments = new TreeMap<Segment, SortedSet<Segment>>();

    numclusters = new int[clusteringsCount];
    clusters = new ArrayList<List<Cluster<Model>>>(clusteringsCount);

    clusterPaircount = new ArrayList<TIntIntHashMap>(clusteringsCount);

    // save count of clusters
    int clusteringIndex = 0;
    for(Clustering<?> clr : clusterings) {
      clusterPaircount.add(new TIntIntHashMap());

      List<Cluster<Model>> curClusters = ((Clustering<Model>) clr).getAllClusters();
      clusters.add(curClusters);
      numclusters[clusteringIndex] = curClusters.size();

      clusteringIndex++;
    }

    createSegments(baseResult);
  }

  protected void createSegments(HierarchicalResult baseResult) {
    final Database db = ResultUtil.findDatabase(baseResult);
    // TODO: Intersect clusters instead?
    for(DBID id : db.getRelation(TypeUtil.DBID).iterDBIDs()) {
      addObject(id);
    }
    convertToPairSegments();
  }

  /**
   * Add a database object to its segment. Creates a new segmentID if required.
   * 
   * @param tag occurrence in clusterings
   */
  protected void addObject(DBID objectID) {
    Segment temp = findSegmentIDForObject(objectID);
    // Do we have this segment already?
    Segment tag = segments.get(temp);
    if(tag == null) {
      tag = temp;
      tag.objIds = DBIDUtil.newHashSet();
      segments.put(tag, tag);
    }

    tag.objIds.add(objectID);
  }

  /**
   * Get the SegmentID of a database object.
   * 
   * @param objectID
   * @return
   */
  private Segment findSegmentIDForObject(DBID objectID) {
    Segment result = new Segment(clusteringsCount);
    findSegmentIDForObject(objectID, result, 0);
    return result;
  }

  /**
   * Determines the SegmentID of a database object, by iterating through all
   * clusterings and storing the index of the cluster the object is found in.
   * 
   * @param objectID
   * @param tag
   * @param clusteringIndex
   */
  private void findSegmentIDForObject(DBID objectID, Segment tag, int clusteringIndex) {
    if(clusteringIndex < clusteringsCount) {
      // search object in clusters of this clustering
  
      int currentCluster = 1;
      // TODO Abbruch wenn Objekt gefunden (while-Schleife)
      for(Cluster<?> cluster : clusters.get(clusteringIndex)) {
        // if object in this cluster
  
        if(cluster.getIDs().contains(objectID)) {
          tag.set(clusteringIndex, currentCluster);
          findSegmentIDForObject(objectID, tag, clusteringIndex + 1);
          return;
        }
  
        currentCluster++;
      }
  
      // if object is unclustered in this clustering, tag accordingly and search
      // in next clusterings
      tag.set(clusteringIndex, Segment.UNCLUSTERED);
      findSegmentIDForObject(objectID, tag, clusteringIndex + 1);
    }
  }

  public boolean hasSegmentIDs(Segment id) {
    return segments.containsKey(id);
  }

  public DBIDs getSegmentDBIDs(Segment id) {
    return segments.get(id).objIds;
  }

  public String getClusteringDescription(int clusteringID) {
    return clusterings.get(clusteringID).getLongName();
  }

  /**
   * Return to a given segment with unpaired objects, the corresponding segments
   * that result in an unpaired segment. So, one cluster of a clustering is
   * split by another clustering in multiple segments, resulting in a segment
   * with unpaired objects, describing the missing pairs between the split
   * cluster / between the segments.
   * 
   * Basically we compare only two clusterings at once. If those clusterings do
   * not have the whole cluster in common, we have at least three segments (two
   * cluster), one of them containing the unpaired segment. A segmentID 3-0,
   * describes a cluster 3 in clustering 1 (index 0) and all clusters 3-x in
   * clustering 2. So we search for all segments 3-x (0 being a wildcard).
   * 
   * @param unpairedSegment
   * @return Segments describing the set of objects that result in an unpaired
   *         segment
   */
  public ArrayList<Segment> getPairedSegments(Segment unpairedSegment) {
    ArrayList<Segment> pairedSegments = new ArrayList<Segment>();

    // get the clustering index, with missing object pairs
    int unpairedClusteringIndex = unpairedSegment.getUnpairedClusteringIndex();

    // if this is not an unpairedSegment return an empty list
    // - optional return given segment in list?
    if(unpairedClusteringIndex <= -1) {
      return pairedSegments;
    }

    // search the segments. Index at "unpairedClustering" being the wildcard.
    for(Segment segment : segments.keySet()) {
      // if mismatch except at unpaired Clustering index => exclude.
      boolean match = true;
      for(int i = 0; i < clusteringsCount; i++) {
        if(i != unpairedClusteringIndex) {
          // mismatch
          if(segment.get(i) != unpairedSegment.get(i)) {
            match = false;
          }
          // do not add wildcard
          else if(segment.get(unpairedClusteringIndex) == 0) {
            match = false;
          }
        }
      }

      if(match == true) {
        // add segment to list
        pairedSegments.add(segment);
      }
    }

    return pairedSegments;
  }

  /**
   * Retrieve all DB objects of a Segment by its SegmentID.
   * 
   * @param id SegmentID (Clusterings and their Cluster)
   * @return DBIDs contained in SegmentID
   */
  public DBIDs getDBIDs(Segment id) {
    // find first clustering
    int startClustering = 0;
    boolean found = false;
    while(found == false) {
      if(id.get(startClustering) != 0) {
        found = true;
      }
      else {
        startClustering++;
      }
    }

    // fetch all DB objects of the first clustering and its selected clusterID.
    // This includes the selection to find.
    DBIDs currentIDs = clusters.get(startClustering).get(id.get(startClustering) - 1).getIDs();
    HashSetModifiableDBIDs objectIDs = DBIDUtil.newHashSet(currentIDs); // copy

    // iterate over remaining clusterings and find intersecting objects
    for(int i = startClustering + 1; i < clusteringsCount; i++) {
      if(id.get(i) != 0) {
        objectIDs.retainAll(clusters.get(i).get(id.get(i) - 1).getIDs());
      }
    }

    // and return selection
    return objectIDs;
  }

  /**
   * @param segmentIDString string representation of the segmentID
   * @return the segmentID given by its string representation
   */
  public Segment uniqueSegmentID(Segment temp) {
    Segment found = segments.get(temp);
    return (found != null) ? found : temp;
  }

  /**
   * Converts the created objectSegments into pairSegments
   */
  public void convertToPairSegments() {
    // create all pair segments
    createFragmentedSegments();

    // All Cluster Pairs
    Segment tagID = getFragmentedSegment((SortedSet<Segment>) segments.keySet());
    fragmentedSegments.put(tagID, (SortedSet<Segment>) segments.keySet());

    // add all objectSegments with their pairs
    for(Segment segment : segments.keySet()) {
      int pairs = segment.getPairCount();

      // FIXME: Ein Objekt geclustered => Fehlende Paarbezüge
      if(pairs != 0) {
        // cluster pair count
        addPairsToCluster(segment, pairs);

        // total pairs count
        this.totalPairs += pairs;
      }
    }

    // getPaircount of new segments
    calculatePairs();
  }

  /**
   * Calculates pairs of pair segments
   */
  private void calculatePairs() {
    // calculated Segments
    ArrayList<String> processedSegments = new ArrayList<String>();

    // for all new segments
    for(Segment segment : fragmentedSegments.descendingKeySet()) {
      // segments of current pairSegment
      SortedSet<Segment> currentSegments = fragmentedSegments.get(segment);

      // Paircount of the segment
      int pairs = getMissingPaircount(fragmentedSegments.get(segment));

      // as Array
      Segment[] current = new Segment[currentSegments.size()];
      int index = 0;
      for(Segment s : currentSegments) {
        current[index] = s;
        index++;
      }

      // Find Pairs that are already counted and
      // substract them from paircount
      for(int i = 0; i < current.length - 1; i++) {
        for(int k = i + 1; k < current.length; k++) {
          String id = current[i].toString() + current[k].toString();

          if(processedSegments.contains(id)) {
            int seg1 = current[i].getObjectCount();
            int seg2 = current[k].getObjectCount();

            pairs -= (objectCountToPairCount(seg1 + seg2) - (objectCountToPairCount(seg1) + objectCountToPairCount(seg2)));
          }
          else {
            // Add Segments to processed Segments
            processedSegments.add(id);
          }
        }
      }

      // cluster pair count
      addPairsToCluster(segment, pairs);

      // total pairs count
      this.totalPairs += pairs;
    }
  }

  private void addPairsToCluster(Segment segment, int pairs) {
    for(int i = 0; i < clusteringsCount; i++) {
      int cluster = segment.get(i);
      if(cluster != 0) {
        if(clusterPaircount.get(i).containsKey(cluster)) {
          int current = clusterPaircount.get(i).get(cluster);
          clusterPaircount.get(i).put(cluster, current + pairs);
        }
        else {
          clusterPaircount.get(i).put(cluster, pairs);
        }
      }
    }
  }

  /**
   * Get pairs of object segments
   * 
   * @param segments List of segments to pair
   * @return pair count of segments
   */
  private int getMissingPaircount(Set<Segment> segments) {
    int totalObjects = 0;
    int segmentPairs = 0;
    for(Segment segment : segments) {
      totalObjects += segment.getObjectCount();
      segmentPairs += segment.getPairCount();
    }
    return objectCountToPairCount(totalObjects) - segmentPairs;
  }

  public int[] getPaircount(int firstClustering, boolean firstClusterNoise, int secondClustering, boolean secondClusterNoise) {
    int inBoth = 0;
    int inFirst = 0;
    int inSecond = 0;

    for(Segment segment : segments.keySet()) {
      if(segment.get(firstClustering) != 0) {
        if(segment.get(secondClustering) != 0) {
          inBoth += segment.getPairCount();
        }
        else {
          inFirst += segment.getPairCount();
        }
      }
      else if(segment.get(secondClustering) != 0) {
        inSecond += segment.getPairCount();
      }
    }
    return new int[] { inBoth, inFirst, inSecond };
  }

  /**
   * Build new segments depending on pairs
   */
  private void createFragmentedSegments() {
    // for every clustering
    for(int clr = 0; clr < clusteringsCount; clr++) {
      // and for every cluster
      for(int c = 0; c < numclusters[clr]; c++) {
        // get common segments
        SortedSet<Segment> commonSegments = getObjectSegments(clr, c + 1);

        if(commonSegments.size() > 1) {
          Segment tag = getFragmentedSegment(commonSegments);
          fragmentedSegments.put(tag, commonSegments);

          // find segments that have more cluster in common
          createFragmentedSegments(clr + 1, clusteringsCount - 1, commonSegments);
        }
      }
    }
  }

  private void createFragmentedSegments(int fromThisClustering, int clustersToFind, Set<Segment> segments) {
    if(clustersToFind == 0) {
      return;
    }

    // for next clusterings
    for(int currentClustering = fromThisClustering; currentClustering < clusteringsCount; currentClustering++) {
      // search cluster
      for(int c = 0; c < numclusters[currentClustering]; c++) {
        SortedSet<Segment> match = new TreeSet<Segment>();

        // get all segments that match cluster
        for(Segment segment : segments) {
          if(segment.get(currentClustering) == (c + 1)) {
            match.add(segment);
          }
        }

        if(match.size() > 1) {
          Segment tag = getFragmentedSegment(match);
          fragmentedSegments.put(tag, match);

          // Find an additional match
          createFragmentedSegments(currentClustering + 1, clustersToFind - 1, match);
        }
      }
    }
  }

  /**
   * Builds new Segment on basis of involved object segments
   * 
   * @param segments object segments participating in new pair segment
   * @return segment id
   */
  private Segment getFragmentedSegment(SortedSet<Segment> segments) {
    Segment pairSegment = new Segment(clusteringsCount);

    for(int i = 0; i < clusteringsCount; i++) {
      int currentCluster = -1;

      for(Segment segment : segments) {
        if(currentCluster == -1) {
          currentCluster = segment.get(i);
        }
        else {
          if(segment.get(i) != currentCluster) {
            currentCluster = 0;
          }
        }
      }
      pairSegment.set(i, currentCluster);
    }
    return pairSegment;
  }

  /**
   * Returns all specified segments
   * 
   * @param clustering clustering to search for
   * @param clusterID cluster to find
   * @return list of matching segments
   */
  private SortedSet<Segment> getObjectSegments(int clustering, int clusterID) {
    SortedSet<Segment> matchedSegments = new TreeSet<Segment>();

    for(Segment tag : segments.keySet()) {
      if(tag.get(clustering) == clusterID) {
        matchedSegments.add(tag);
      }
    }
    return matchedSegments;
  }

  /**
   * Calculates pair count of a set of objects
   * 
   * @param objectCount count of objects
   * @return pair count
   */
  private int objectCountToPairCount(int objectCount) {
    return objectCount * (objectCount - 1) / 2;
  }

  /**
   * get size of object segments or pair segments if calculated
   * 
   * @return size of segments
   */
  public int size() {
    return segments.size();
  }

  /**
   * Get total number of pairs with or without the unclustered pairs.
   * 
   * @param withUnclusteredPairs if false, segment with unclustered pairs is
   *        removed
   * @return
   */
  public int getPairCount(boolean withUnclusteredPairs) {
    if(withUnclusteredPairs) {
      return totalPairs;
    }
    else {
      // create the SegmentID
      Segment unclusteredPairs = new Segment(clusteringsCount);
      if(segments.containsKey(unclusteredPairs)) {
        return totalPairs - segments.get(unclusteredPairs).getPairCount();
      }
      else {
        return totalPairs;
      }
    }
  }

  public int getPairCount(int clustering, int cluster) {
    return clusterPaircount.get(clustering).get(cluster);
  }

  /**
   * Get segments as list with or without the segment representing pairs that
   * are unclustered.
   * 
   * @return
   */
  public TreeMap<Segment, Segment> getSegments() {
    return segments;
  }

  public int getClusterings() {
    return clusteringsCount;
  }

  public int[] getClusters() {
    return numclusters;
  }

  /**
   * Return the sum of all clusters
   * 
   * @return
   */
  public int getTotalClusterCount() {
    int clusterCount = 0;

    for(int i = 0; i < numclusters.length; i++) {
      clusterCount += numclusters[i];
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

    for(int i = 0; i < numclusters.length; i++) {
      if(maxClusters < numclusters[i]) {
        maxClusters = numclusters[i];
      }
    }

    return maxClusters;
  }

  public void print(Logging logger) {
    logger.verbose("Object Segments");
    logger.verbose("---");

    int totalCount = 0;
    for(Segment key : segments.keySet()) {
      totalCount += key.getObjectCount();
      logger.verbose(key.toString() + ": " + key.getObjectCount());
    }
    logger.verbose("----------------------------------");
    logger.verbose("sum: " + totalCount + " objects");

    int totalPairs = objectCountToPairCount(totalCount);
    totalCount = 0;
    {
      logger.verbose("");
      logger.verbose("Pair Segments");
      logger.verbose("---");

      for(Segment key : segments.keySet()) {
        totalCount += key.getPairCount();
        logger.verbose(key + ": " + key.getPairCount());
      }
      logger.verbose("----------------------------------");
      logger.verbose("sum: " + this.totalPairs + " of " + totalPairs + " pairs");
    }
  }
}