package experimentalcode.students.goldhofa;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Triple;

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
   * if segments are converted into pairSegments
   */
  private boolean isPairSegments = false;

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
   * List of object based segments (segment, size)
   */
  private TreeMap<SegmentID, Integer> objectSegments;

  // store (segments, dbids) TODO: use objectSegments instead
  private TreeMap<SegmentID, ModifiableDBIDs> segmentDBIDs;

  /**
   * List of pair based segments and their paircount (segment, paircount)
   */
  private TreeMap<SegmentID, Integer> pairSegments;

  // TODO: flag noise clusters by clustering-cluster

  /**
   * List of pair segments and their involved object segments (helper)
   */
  private TreeMap<SegmentID, SortedSet<SegmentID>> fragmentedSegments;

  /**
   * Paircount of clusters by clusterings
   */
  private ArrayList<TreeMap<Integer, Integer>> clusterPaircount;

  /**
   * Initialize segments. Add DB objects via addObject method.
   * 
   * @param clusterings List of clusterings in comparison
   * @param baseResult used to retrieve db objects
   */
  public Segments(List<Clustering<?>> clusterings, HierarchicalResult baseResult) {
    this.clusterings = clusterings;
    // DBIDUtil.newArray()
    objectSegments = new TreeMap<SegmentID, Integer>();
    fragmentedSegments = new TreeMap<SegmentID, SortedSet<SegmentID>>();
    pairSegments = new TreeMap<SegmentID, Integer>();
    segmentDBIDs = new TreeMap<SegmentID, ModifiableDBIDs>();

    clusteringsCount = clusterings.size();
    clusters = new int[clusteringsCount];

    // noiseclusters = new int[clusteringsCount];
    // noiseindex = new int[clusteringsCount];

    clusterPaircount = new ArrayList<TreeMap<Integer, Integer>>(this.clusteringsCount);

    // save count of clusters
    int clusteringIndex = 0;
    for(Clustering<?> clr : clusterings) {
      clusterPaircount.add(new TreeMap<Integer, Integer>());

      clusters[clusteringIndex] = clr.getAllClusters().size();

      // determine noise cluster
      // noiseindex[clusteringIndex] = clusters[clusteringIndex];

      clusteringIndex++;
    }

    createSegments(baseResult);
  }

  protected void createSegments(HierarchicalResult baseResult) {
    final Database db = ResultUtil.findDatabase(baseResult);
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
    SegmentID tag = getSegmentID(objectID);

    if(objectSegments.containsKey(tag)) {
      objectSegments.put(tag, objectSegments.get(tag) + 1);
      ModifiableDBIDs dbids = segmentDBIDs.get(tag);
      dbids.add(objectID);

      // first segment created
    }
    else {
      // assign an index (temporary)
      tag.setIndex(objectSegments.size());
      // add to list
      objectSegments.put(tag, 1);
      ModifiableDBIDs dbids = DBIDUtil.newHashSet();
      dbids.add(objectID);
      segmentDBIDs.put(tag, dbids);
    }
  }

  public boolean hasSegmentIDs(SegmentID id) {
    return segmentDBIDs.containsKey(id);
  }

  public DBIDs getSegmentDBIDs(SegmentID id) {
    return segmentDBIDs.get(id);
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
  public ArrayList<SegmentID> getPairedSegments(SegmentID unpairedSegment) {
    ArrayList<SegmentID> pairedSegments = new ArrayList<SegmentID>();

    // get the clustering index, with missing object pairs
    int unpairedClusteringIndex = unpairedSegment.getUnpairedClusteringIndex();

    // if this is not an unpairedSegment return an empty list
    // - optional return given segment in list?
    if(unpairedClusteringIndex <= -1) {
      return pairedSegments;
    }

    // search the segments. Index at "unpairedClustering" being the wildcard.
    for(SegmentID segment : pairSegments.keySet()) {
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
  public DBIDs getDBIDs(SegmentID id) {

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
    DBIDs currentIDs = clusterings.get(startClustering).getAllClusters().get(id.get(startClustering) - 1).getIDs();
    ModifiableDBIDs objectIDs = DBIDUtil.newHashSet(currentIDs);

    // iterate over remaining clusterings and find intersecting objects
    for(int i = startClustering + 1; i < clusteringsCount; i++) {
      if(id.get(i) != 0) {
        objectIDs = intersect(objectIDs, i, id.get(i) - 1);
      }
    }

    // and return selection
    return objectIDs;
  }

  /**
   * Get intersection of DBIDs and a clusterings cluster
   * 
   * @param ids Current set of IDs
   * @param clusteringID id of clustering
   * @param clusterID id of clusterings cluster
   * @return intersection of common IDs
   */
  private ModifiableDBIDs intersect(DBIDs ids, int clusteringID, int clusterID) {
    // get all IDs of the clusterings cluster
    // TODO: getAllClusters() can be expensive (tree walk) or could be unstable
    DBIDs currentIDs = clusterings.get(clusteringID).getAllClusters().get(clusterID).getIDs();
    return DBIDUtil.intersection(ids, currentIDs);
  }

  /**
   * Get the SegmentID of a database object.
   * 
   * @param objectID
   * @return
   */
  public SegmentID getSegmentID(DBID objectID) {
    SegmentID result = new SegmentID(clusteringsCount);
    getSegment(objectID, result, 0);
    return result;
  }

  /**
   * @param segmentIDString string representation of the segmentID
   * @return the segmentID given by its string representation
   */
  public SegmentID getSegmentID(String segmentIDString) {
    // get already created segmentID in case any additional info was used
    // (temp: index)
    SegmentID temp = new SegmentID(segmentIDString);
    for(SegmentID segment : pairSegments.keySet()) {
      if(segment.compareTo(temp) == 0) {
        return segment;
      }
    }

    return temp;
  }

  /**
   * Determines the SegmentID of a database object, by iterating through all
   * clusterings and storing the index of the cluster the object is found in.
   * 
   * @param objectID
   * @param tag
   * @param clusteringIndex
   */
  private void getSegment(DBID objectID, SegmentID tag, int clusteringIndex) {
    if(clusteringIndex < clusterings.size()) {
      // search object in clusters of this clustering

      int currentCluster = 1;
      boolean objectFound = false;

      // TODO Abbruch wenn Objekt gefunden (while-Schleife)
      for(Cluster<?> cluster : clusterings.get(clusteringIndex).getAllClusters()) {
        // if object in this cluster

        if(cluster.getIDs().contains(objectID)) {
          int index = currentCluster;

          /*
           * // determine index if (cluster.isNoise()) {
           * 
           * index = noiseindex[clusteringIndex]; noiseindex[clusteringIndex]++;
           * }
           */

          // search in next clusterings

          objectFound = true;
          tag.set(clusteringIndex, index);
          getSegment(objectID, tag, clusteringIndex + 1);
        }

        currentCluster++;
      }

      // if object is unclustered in this clustering, tag accordingly and search
      // in next clusterings
      if(!objectFound) {
        tag.set(clusteringIndex, 0);
        getSegment(objectID, tag, clusteringIndex + 1);
      }
    }
  }

  /**
   * Converts the created objectSegments into pairSegments
   */
  public void convertToPairSegments() {
    if(isPairSegments) {
      return;
    }

    // create all pair segments
    createFragmentedSegments();

    // All Cluster Pairs
    SegmentID tagID = getFragmentedSegment((SortedSet<SegmentID>) objectSegments.keySet());
    fragmentedSegments.put(tagID, (SortedSet<SegmentID>) objectSegments.keySet());

    // add all objectSegments with their pairs
    for(SegmentID segment : objectSegments.keySet()) {
      int pairs = asPairs(objectSegments.get(segment));

      // FIXME: Ein Objekt geclustered => Fehlende Paarbezüge
      if(pairs != 0) {
        pairSegments.put(segment, pairs);

        // cluster pair count
        addPairsToCluster(segment, pairs);

        // total pairs count
        this.pairs += pairs;
      }
    }

    // getPaircount of new segments
    calculatePairs();

    isPairSegments = true;
  }

  /**
   * Calculates pairs of pair segments
   */
  private void calculatePairs() {
    // calculated Segments
    ArrayList<String> processedSegments = new ArrayList<String>();

    // for all new segments
    for(SegmentID segment : fragmentedSegments.descendingKeySet()) {
      // segments of current pairSegment
      SortedSet<SegmentID> currentSegments = fragmentedSegments.get(segment);

      // Paircount of the segment
      int pairs = getPaircount(fragmentedSegments.get(segment));

      // as Array
      SegmentID[] current = new SegmentID[currentSegments.size()];
      int index = 0;
      for(SegmentID s : currentSegments) {
        current[index] = s;
        index++;
      }

      // Find Pairs that are already counted and
      // substract them from paircount
      for(int i = 0; i < current.length - 1; i++) {
        for(int k = i + 1; k < current.length; k++) {
          String id = current[i].toString() + current[k].toString();

          if(processedSegments.contains(id)) {
            int seg1 = objectSegments.get(current[i]);
            int seg2 = objectSegments.get(current[k]);

            pairs -= (asPairs(seg1 + seg2) - (asPairs(seg1) + asPairs(seg2)));
          }
          else {
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
  private int getPaircount(Set<SegmentID> segments) {
    int totalObjects = 0;
    int segmentPairs = 0;
    for(SegmentID segment : segments) {
      int objects = this.objectSegments.get(segment);
      totalObjects += objects;
      segmentPairs += asPairs(objects);
    }

    return asPairs(totalObjects) - segmentPairs;
  }

  public Triple<Integer, Integer, Integer> getPaircount(int firstClustering, boolean firstClusterNoise, int secondClustering, boolean secondClusterNoise) {
    if(pairSegments == null) {
      return new Triple<Integer, Integer, Integer>(0, 0, 0);
    }

    int inBoth = 0;
    int inFirst = 0;
    int inSecond = 0;

    for(SegmentID segment : pairSegments.keySet()) {
      if(segment.get(firstClustering) != 0) {
        if(segment.get(secondClustering) != 0) {
          inBoth += pairSegments.get(segment);
        }
        else {
          inFirst += pairSegments.get(segment);
        }
      }
      else if(segment.get(secondClustering) != 0) {
        inSecond += pairSegments.get(segment);
      }
    }
    return new Triple<Integer, Integer, Integer>(inBoth, inFirst, inSecond);
  }

  /**
   * Build new segments depending on pairs
   */
  private void createFragmentedSegments() {
    // for every clustering
    for(int clr = 0; clr < clusteringsCount; clr++) {
      // and for every cluster
      for(int c = 0; c < clusters[clr]; c++) {
        // get common segments
        SortedSet<SegmentID> commonSegments = getObjectSegments(clr, c + 1);

        if(commonSegments.size() > 1) {
          SegmentID tag = getFragmentedSegment(commonSegments);
          fragmentedSegments.put(tag, commonSegments);

          // find segments that have more cluster in common
          createFragmentedSegments(clr + 1, clusteringsCount - 1, commonSegments);
        }
      }
    }
  }

  private void createFragmentedSegments(int fromThisClustering, int clustersToFind, Set<SegmentID> segments) {
    if(clustersToFind == 0) {
      return;
    }

    // for next clusterings
    for(int currentClustering = fromThisClustering; currentClustering < clusteringsCount; currentClustering++) {
      // search cluster
      for(int c = 0; c < clusters[currentClustering]; c++) {
        SortedSet<SegmentID> match = new TreeSet<SegmentID>();

        // get all segments that match cluster
        for(SegmentID segment : segments) {
          if(segment.get(currentClustering) == (c + 1)) {
            match.add(segment);
          }
        }

        if(match.size() > 1) {
          SegmentID tag = getFragmentedSegment(match);
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
  private SegmentID getFragmentedSegment(SortedSet<SegmentID> segments) {
    SegmentID pairSegment = new SegmentID(clusteringsCount);

    for(int i = 0; i < clusteringsCount; i++) {
      int currentCluster = -1;

      for(SegmentID segment : segments) {
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
  private SortedSet<SegmentID> getObjectSegments(int clustering, int clusterID) {
    SortedSet<SegmentID> matchedSegments = new TreeSet<SegmentID>();

    for(SegmentID tag : objectSegments.keySet()) {
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
  private int asPairs(int objectCount) {
    return objectCount * (objectCount - 1) / 2;
  }

  /**
   * get size of object segments or pair segments if calculated
   * 
   * @return size of segments
   */
  public int size() {
    if(isPairSegments) {
      return pairSegments.size();
    }
    else {
      return objectSegments.size();
    }
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
      return pairs;
    }
    else {
      // create the SegmentID
      SegmentID unclusteredPairs = new SegmentID(clusteringsCount);
      if(pairSegments.containsKey(unclusteredPairs)) {
        return pairs - pairSegments.get(unclusteredPairs);
      }
      else {
        return pairs;
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
   * @param withUnclusteredPairs if false, segment with unclustered pairs is
   *        removed
   * @return
   */
  public TreeMap<SegmentID, Integer> getSegments(boolean withUnclusteredPairs) {
    if(withUnclusteredPairs) {
      return pairSegments;
    }
    else {
      // create the SegmentID
      SegmentID unclusteredPairs = new SegmentID(clusteringsCount);

      @SuppressWarnings("unchecked")
      TreeMap<SegmentID, Integer> segments = (TreeMap<SegmentID, Integer>) pairSegments.clone();

      segments.remove(unclusteredPairs);
      return segments;
    }
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

    for(int i = 0; i < clusters.length; i++) {
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

    for(int i = 0; i < clusters.length; i++) {
      if(maxClusters < clusters[i]) {
        maxClusters = clusters[i];
      }
    }

    return maxClusters;
  }

  public void print(Logging logger) {
    logger.verbose("Object Segments");
    logger.verbose("---");

    int totalCount = 0;
    for(SegmentID key : objectSegments.keySet()) {
      totalCount += objectSegments.get(key);
      logger.verbose(key.toString() + ": " + objectSegments.get(key));
    }
    logger.verbose("----------------------------------");
    logger.verbose("sum: " + totalCount + " objects");

    int totalPairs = asPairs(totalCount);
    totalCount = 0;
    if(isPairSegments) {
      logger.verbose("");
      logger.verbose("Pair Segments");
      logger.verbose("---");

      for(SegmentID key : pairSegments.keySet()) {
        totalCount += pairSegments.get(key);
        logger.verbose(key + ": " + pairSegments.get(key));
      }
      logger.verbose("----------------------------------");
      logger.verbose("sum: " + this.pairs + " of " + totalPairs + " pairs");
    }
  }
}