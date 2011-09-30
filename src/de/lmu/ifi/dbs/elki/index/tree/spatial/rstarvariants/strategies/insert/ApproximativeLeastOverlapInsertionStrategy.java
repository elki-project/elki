package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert;

import java.util.Collections;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialAdapter;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparableAdapter;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

/**
 * The choose subtree method proposed by the R*-Tree with slightly better
 * performance for large leaf sizes (linear approximation).
 * 
 * <p>
 * N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger:<br />
 * The R*-tree: an efficient and robust access method for points and rectangles<br />
 * in: Proceedings of the 1990 ACM SIGMOD International Conference on Management
 * of Data, Atlantic City, NJ, May 23-25, 1990
 * </p>
 * 
 * @author Erich Schubert
 * @author Franz Graf
 * @author Marisa Petri
 */
@Reference(authors = "N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger", title = "The R*-tree: an efficient and robust access method for points and rectangles", booktitle = "Proceedings of the 1990 ACM SIGMOD International Conference on Management of Data, Atlantic City, NJ, May 23-25, 1990", url = "http://dx.doi.org/10.1145/93597.98741")
public class ApproximativeLeastOverlapInsertionStrategy extends LeastOverlapInsertionStrategy {
  /**
   * Number of candidates to consider
   */
  private int numCandidates = 32;

  /**
   * Constructor.
   */
  public ApproximativeLeastOverlapInsertionStrategy(int candidates) {
    super();
    this.numCandidates = candidates;
  }

  @Override
  public <E, I, A> int choose(A options, ArrayAdapter<E, A> getter, SpatialAdapter<? super E> a1, I obj, SpatialAdapter<? super I> a2, boolean leaf) {
    if(leaf) {
      return chooseLeastOverlapApproximately(options, getter, a1, obj, a2);
    }
    else {
      return chooseLeastEnlargement(options, getter, a1, obj, a2);
    }
  }

  /**
   * Choose subtree by least overlap, then area increase, then area. See the
   * R*-Tree publication for details and experiments.
   * 
   * @param options Options to choose from
   * @param getter Array adapter for options
   * @param a1 Spatial adapter for options
   * @param obj Insertion object
   * @param a2 Spatial adapter for insertion object
   * @return Subtree index in array.
   */
  protected <I, E, A> int chooseLeastOverlapApproximately(A options, ArrayAdapter<E, A> getter, SpatialAdapter<? super E> a1, I obj, SpatialAdapter<? super I> a2) {
    final int size = getter.size(options);
    assert (size > 0) : "Choose from empty set?";
    if(size <= numCandidates) {
      // Skip building the heap.
      return chooseLeastOverlap(options, getter, a1, obj, a2);
    }

    // Heap of candidates
    TopBoundedHeap<DoubleIntPair> candidates = new TopBoundedHeap<DoubleIntPair>(numCandidates, Collections.reverseOrder());
    for(int i = 0; i < size; i++) {
      // Existing object and extended rectangle:
      E entry = getter.get(options, i);
      HyperBoundingBox mbr = SpatialUtil.union(entry, a1, obj, a2);
      // Area increase
      final double inc_area = SpatialUtil.volume(mbr) - a1.getVolume(entry);
      candidates.add(new DoubleIntPair(inc_area, i));
    }

    // R*-Tree: overlap increase for leaves.
    int best = -1;
    double least_overlap = Double.POSITIVE_INFINITY;
    double least_areainc = Double.POSITIVE_INFINITY;
    double least_area = Double.POSITIVE_INFINITY;
    // least overlap increase, on reduced candidate set:
    while(!candidates.isEmpty()) {
      DoubleIntPair pair = candidates.poll();
      final double inc_area = pair.first;

      // Existing object and extended rectangle:
      E entry = getter.get(options, pair.second);
      HyperBoundingBox mbr = SpatialUtil.union(entry, a1, obj, a2);
      // Compute relative overlap increase.
      double overlap_wout = 0.0;
      double overlap_with = 0.0;
      for(int k = 0; k < size; k++) {
        if(pair.second != k) {
          E other = getter.get(options, k);
          overlap_wout += SpatialUtil.relativeOverlap(entry, a1, other, a1);
          overlap_with += SpatialUtil.relativeOverlap(mbr, SpatialComparableAdapter.STATIC, other, a1);
        }
      }
      double inc_overlap = overlap_with - overlap_wout;
      if(inc_overlap < least_overlap) {
        final double area = a1.getVolume(entry);
        // Volume increase and overlap increase:
        least_overlap = inc_overlap;
        least_areainc = inc_area;
        least_area = area;
        best = pair.second;
      }
      else if(inc_overlap == least_overlap) {
        final double area = a1.getVolume(entry);
        if(inc_area < least_areainc || (inc_area == least_areainc && area < least_area)) {
          least_overlap = inc_overlap;
          least_areainc = inc_area;
          least_area = area;
          best = pair.second;
        }
      }
    }
    assert (best > -1);
    return best;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Fast-insertion parameter. Optional.
     */
    public static OptionID INSERTION_CANDIDATES_ID = OptionID.getOrCreateOptionID("rtree.insertion-candidates", "defines how many children are tested for finding the child generating the least overlap when inserting an object.");

    /**
     * The number of candidates to use
     */
    int numCandidates = 32;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter insertionCandidatesP = new IntParameter(INSERTION_CANDIDATES_ID, new GreaterConstraint(0), numCandidates);
      if(config.grab(insertionCandidatesP)) {
        numCandidates = insertionCandidatesP.getValue();
      }
    }

    @Override
    protected ApproximativeLeastOverlapInsertionStrategy makeInstance() {
      return new ApproximativeLeastOverlapInsertionStrategy(numCandidates);
    }
  }
}