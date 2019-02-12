/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.evaluation.clustering.pairsegments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Creates segments of two or more clusterings.
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
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Sascha Goldhofer, Hans-Peter Kriegel, Erich Schubert,
 * Arthur Zimek<br>
 * Evaluation of Clusterings – Metrics and Visual Support<br>
 * Proc. 28th International Conference on Data Engineering (ICDE 2012)
 * 
 * @author Sascha Goldhofer
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @composed - - - Segment
 */
@Reference(authors = "Elke Achtert, Sascha Goldhofer, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", //
    title = "Evaluation of Clusterings - Metrics and Visual Support", //
    booktitle = "Proc. 28th International Conference on Data Engineering (ICDE 2012)", //
    url = "https://doi.org/10.1109/ICDE.2012.128", //
    bibkey = "DBLP:conf/icde/AchtertGKSZ12")
public class Segments extends BasicResult implements Iterable<Segment> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(Segments.class);

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
  private long actualPairs;

  /**
   * The actual segments
   */
  private TreeMap<Segment, Segment> segments;

  /**
   * Initialize segments. Add DB objects via addObject method.
   * 
   * @param clusterings List of clusterings in comparison
   */
  public Segments(List<Clustering<?>> clusterings) {
    super("cluster pair segments", "pair-segments");
    this.clusterings = clusterings;
    this.clusteringsCount = clusterings.size();
    segments = new TreeMap<>(); // TODO: replace with array list

    numclusters = new int[clusteringsCount];
    clusters = new ArrayList<>(clusteringsCount);

    // save count of clusters
    int clusteringIndex = 0;
    for(Clustering<?> clr : clusterings) {
      List<? extends Cluster<?>> curClusters = clr.getAllClusters();
      clusters.add(curClusters);
      numclusters[clusteringIndex] = curClusters.size();

      clusteringIndex++;
    }

    recursivelyFill(clusters);
    for(Segment seg : segments.keySet()) {
      actualPairs += seg.pairsize;
    }
  }

  private void recursivelyFill(List<List<? extends Cluster<?>>> cs) {
    final int numclusterings = cs.size();
    Iterator<? extends Cluster<?>> iter = cs.get(0).iterator();
    int[] path = new int[numclusterings];
    for(int cnum = 0; iter.hasNext(); cnum++) {
      Cluster<?> clust = iter.next();
      path[0] = cnum;
      if(numclusterings > 1) {
        SetDBIDs idset = DBIDUtil.ensureSet(clust.getIDs());
        recursivelyFill(cs, 1, idset, idset, path, true);
      }
      else {
        // Add to results.
        makeOrUpdateSegment(path, clust.getIDs(), (clust.size() * (clust.size() - 1)) >>> 1);
      }

      totalObjects += clust.size();
    }
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
      for(DBIDIter iter2 = clust.getIDs().iter(); iter2.valid(); iter2.advance()) {
        if(ndelta1.remove(iter2)) {
          nfirstp.add(iter2);
        }
        else {
          ndelta2.add(iter2);
        }
        if(second.contains(iter2)) {
          nsecond.add(iter2);
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
          // In fact, nfirstp should equal nsecond here
          int selfpairs = DBIDUtil.intersectionSize(nfirstp, nsecond);
          if(objectsegment) {
            makeOrUpdateSegment(path, nfirstp, (nfirstp.size() * nsecond.size()) - selfpairs);
          }
          else {
            makeOrUpdateSegment(path, null, (nfirstp.size() * nsecond.size()) - selfpairs);
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
          int selfpairs = DBIDUtil.intersection(ndelta1, nsecond).size();
          makeOrUpdateSegment(path, null, (ndelta1.size() * nsecond.size()) - selfpairs);
        }
      }
      // FIXME: this part doesn't work right yet for over 2 clusterings!
      // It used to work in revision 9236, eventually go back to this code!
      if(ndelta2.size() > 0 && objectsegment) {
        int[] npath = new int[path.length];
        Arrays.fill(npath, Segment.UNCLUSTERED);
        npath[depth] = cnum;
        if(depth < numclusterings - 1) {
          recursivelyFill(cs, depth + 1, ndelta2, nsecond, npath, false);
        }
        else {
          // Add to results.
          int selfpairs = DBIDUtil.intersection(ndelta2, nsecond).size();
          makeOrUpdateSegment(npath, null, (ndelta2.size() * nsecond.size()) - selfpairs);
        }
      }
    }
  }

  private void makeOrUpdateSegment(int[] path, DBIDs ids, int pairsize) {
    Segment seg = segments.get(new Segment(path));
    if(seg == null) {
      seg = new Segment(path.clone());
      segments.put(seg, seg);
    }
    if(ids != null) {
      if(seg.getDBIDs() != null) {
        LOG.warning("Expected segment to not have IDs.");
      }
      seg.objIDs = ids;
    }
    seg.pairsize += pairsize;
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
  public List<Segment> getPairedSegments(Segment unpairedSegment) {
    ArrayList<Segment> pairedSegments = new ArrayList<>();
    // search the segments. Index at "unpairedClustering" being the wildcard.
    segments: for(Segment segment : this) {
      // if mismatch except at unpaired Clustering index => exclude.
      for(int i = 0; i < clusteringsCount; i++) {
        // mismatch, but lso do not add wildcard
        if((unpairedSegment.get(i) != Segment.UNCLUSTERED && segment.get(i) != unpairedSegment.get(i)) //
            || segment.get(i) == Segment.UNCLUSTERED) {
          continue segments;
        }
      }
      // add segment to list
      pairedSegments.add(segment);
    }
    return pairedSegments;
  }

  /**
   * @param temp Temporary segment to be unified
   * @return the segmentID given by its string representation
   */
  public Segment unifySegment(Segment temp) {
    Segment found = segments.get(temp);
    return (found != null) ? found : temp;
  }

  /**
   * Get the number of segments
   * 
   * @return Number of segments
   */
  public int size() {
    return segments.size();
  }

  /**
   * Get total number of pairs with or without the unclustered pairs.
   * 
   * @param withUnclusteredPairs if false, segment with unclustered pairs is
   *        removed
   * @return pair count, with or without unclusted (non-existant) pairs
   */
  public long getPairCount(boolean withUnclusteredPairs) {
    return withUnclusteredPairs ? (totalObjects * (totalObjects - 1L)) >> 1 : actualPairs;
  }

  /**
   * Get the number of clusterings
   * 
   * @return number of clusterings compared
   */
  public int getClusterings() {
    return clusteringsCount;
  }

  /**
   * Return the sum of all clusters
   * 
   * @return sum of all cluster counts
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
   * @return highest cluster count
   */
  public int getHighestClusterCount() {
    int maxClusters = 0;

    for(int i = 0; i < numclusters.length; i++) {
      maxClusters = Math.max(maxClusters, numclusters[i]);
    }
    return maxClusters;
  }

  @Override
  public Iterator<Segment> iterator() {
    return segments.keySet().iterator();
  }
}
