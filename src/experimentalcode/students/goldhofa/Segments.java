package experimentalcode.students.goldhofa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;

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
 * @author Erich Schubert
 */
public class Segments {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(Segments.class);

  /**
   * Clusterings
   */
  private List<Clustering<?>> clusterings;

  /**
   * Clusters
   */
  private List<List<? extends Cluster<?>>> clusters;

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
   * Pairs actually present in the data set
   */
  private int actualPairs;

  /**
   * The actual segments
   */
  private TreeMap<Segment, Segment> segments;

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
    segments = new TreeMap<Segment, Segment>(); // TODO: replace with array list

    numclusters = new int[clusteringsCount];
    clusters = new ArrayList<List<? extends Cluster<?>>>(clusteringsCount);

    // save count of clusters
    int clusteringIndex = 0;
    for(Clustering<?> clr : clusterings) {
      List<? extends Cluster<?>> curClusters = ((Clustering<Model>) clr).getAllClusters();
      clusters.add(curClusters);
      numclusters[clusteringIndex] = curClusters.size();

      clusteringIndex++;
    }

    recursivelyFill(clusters);
    for(Segment seg : segments.keySet()) {
      actualPairs += seg.getPairCount();
    }
  }

  private void recursivelyFill(List<List<? extends Cluster<?>>> cs) {
    final int numclusterings = cs.size();
    Iterator<? extends Cluster<?>> iter = cs.get(0).iterator();
    int[] path = new int[numclusterings];
    for(int cnum = 0; iter.hasNext(); cnum++) {
      Cluster<?> clust = iter.next();
      path[0] = (cnum + 1);
      if(numclusterings > 1) {
        SetDBIDs idset = DBIDUtil.ensureSet(clust.getIDs());
        recursivelyFill(cs, 1, idset, idset, path, true);
      }
      else {
        // Add to results.
        makeOrUpdateSegment(path, clust.getIDs(), clust.getIDs(), clust.size() * (clust.size() - 1));
      }

      totalObjects += clust.size();
    }
  }

  private void makeOrUpdateSegment(int[] path, DBIDs first, DBIDs second, int pairsize) {
    Segment seg = segments.get(new Segment(path));
    if(seg == null) {
      seg = new Segment(path.clone());
      segments.put(seg, seg);
    }
    if(first != null || second != null) {
      if(seg.firstIDs != null) {
        logger.warning("Expected segment to not have IDs.");
      }
      if(seg.secondIDs != null) {
        logger.warning("Expected segment to not have IDs.");
      }
      seg.firstIDs = first;
      seg.secondIDs = second;
    }
    seg.pairsize += pairsize;
  }

  private void recursivelyFill(List<List<? extends Cluster<?>>> cs, int depth, SetDBIDs first, SetDBIDs second, int[] path, boolean objectsegment) {
    final int numclusterings = cs.size();
    Iterator<? extends Cluster<?>> iter = cs.get(depth).iterator();
    for(int cnum = 0; iter.hasNext(); cnum++) {
      Cluster<?> clust = iter.next();
      // Compute intersections with new cluster.
      // nfp := intersection( first, cluster )
      // Adding asymmetric differences to nd1, nd2.
      // nse := intersection( second, cluster )
      HashSetModifiableDBIDs nfirstp = DBIDUtil.newHashSet(first.size());
      HashSetModifiableDBIDs ndelta1 = DBIDUtil.newHashSet(first);
      HashSetModifiableDBIDs ndelta2 = DBIDUtil.newHashSet();
      HashSetModifiableDBIDs nsecond = DBIDUtil.newHashSet(second.size());
      for(DBID id : clust.getIDs()) {
        if(ndelta1.remove(id)) {
          nfirstp.add(id);
        }
        else {
          ndelta2.add(id);
        }
        if(second.contains(id)) {
          nsecond.add(id);
        }
      }
      if(nsecond.size() <= 0) {
        continue; // disjoint
      }
      if(nfirstp.size() > 0) {
        path[depth] = cnum;
        if(depth < numclusterings - 1) {
          recursivelyFill(cs, depth + 1, nfirstp, nsecond, path, objectsegment);
        }
        else {
          // Add to results.
          int selfpairs = DBIDUtil.intersection(nfirstp, nsecond).size();
          if(objectsegment) {
            makeOrUpdateSegment(path, nfirstp, nsecond, (nfirstp.size() * nsecond.size()) - selfpairs);
          }
          else {
            makeOrUpdateSegment(path, null, null, (nfirstp.size() * nsecond.size()) - selfpairs);
          }
        }
      }
      // Elements that were in first, but in not in the cluster
      if(ndelta1.size() > 0) {
        path[depth] = Segment.UNCLUSTERED;
        if(depth < numclusterings - 1) {
          recursivelyFill(cs, depth + 1, ndelta1, nsecond, path, false);
        }
        else {
          // Add to results.
          makeOrUpdateSegment(path, null, null, ndelta1.size() * nsecond.size());
        }
      }
      if(ndelta2.size() > 0) {
        int[] npath = new int[path.length];
        Arrays.fill(npath, Segment.UNCLUSTERED);
        npath[depth] = cnum;
        if(depth < numclusterings - 1) {
          recursivelyFill(cs, depth + 1, ndelta2, nsecond, npath, false);
        }
        else {
          // Add to results.
          makeOrUpdateSegment(npath, null, null, ndelta2.size() * nsecond.size());
        }
      }
    }
  }

  /**
   * Get the description of the nth clustering.
   * 
   * @param clusteringID Clustering number
   * @return long name of clustering
   */
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
    for(Segment segment : getSegments()) {
      // if mismatch except at unpaired Clustering index => exclude.
      boolean match = true;
      for(int i = 0; i < clusteringsCount; i++) {
        if(i == unpairedClusteringIndex) {
          continue;
        }
        // mismatch
        if(segment.get(i) != unpairedSegment.get(i)) {
          match = false;
          break;
        }
        // do not add wildcard
        else if(segment.get(unpairedClusteringIndex) == Segment.UNCLUSTERED) {
          match = false;
          break;
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
   * @param segmentIDString string representation of the segmentID
   * @return the segmentID given by its string representation
   */
  public Segment unifySegment(Segment temp) {
    Segment found = segments.get(temp);
    return (found != null) ? found : temp;
  }

  public int[] getPaircount(int firstClustering, boolean firstClusterNoise, int secondClustering, boolean secondClusterNoise) {
    int inBoth = 0;
    int inFirst = 0;
    int inSecond = 0;

    for(Segment segment : getSegments()) {
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
      return (totalObjects * (totalObjects - 1)); // / 2;
    }
    else {
      return actualPairs;
    }
  }

  /**
   * Get segments as list with or without the segment representing pairs that
   * are unclustered.
   * 
   * @return
   */
  public Collection<Segment> getSegments() {
    return segments.keySet();
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
}