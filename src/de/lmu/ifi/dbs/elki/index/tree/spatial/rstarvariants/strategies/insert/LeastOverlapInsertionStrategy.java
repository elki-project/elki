package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * The choose subtree method proposed by the R*-Tree.
 * 
 * <p>
 * N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger:<br />
 * The R*-tree: an efficient and robust access method for points and rectangles<br />
 * in: Proceedings of the 1990 ACM SIGMOD International Conference on Management
 * of Data, Atlantic City, NJ, May 23-25, 1990
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger", title = "The R*-tree: an efficient and robust access method for points and rectangles", booktitle = "Proceedings of the 1990 ACM SIGMOD International Conference on Management of Data, Atlantic City, NJ, May 23-25, 1990", url = "http://dx.doi.org/10.1145/93597.98741")
public class LeastOverlapInsertionStrategy implements InsertionStrategy {
  /**
   * Static instance.
   */
  public static final LeastOverlapInsertionStrategy STATIC = new LeastOverlapInsertionStrategy();

  /**
   * Constructor.
   */
  public LeastOverlapInsertionStrategy() {
    super();
  }

  @Override
  public <A> int choose(A options, ArrayAdapter<? extends SpatialComparable, A> getter, SpatialComparable obj, boolean leaf) {
    if(leaf) {
      return chooseLeastOverlap(options, getter, obj);
    }
    else {
      return chooseLeastEnlargement(options, getter, obj);
    }
  }

  /**
   * Choose subtree by least overlap, then area increase, then area. See the
   * R*-Tree publication for details and experiments.
   * 
   * @param options Options to choose from
   * @param getter Array adapter for options
   * @param obj Insertion object
   * @return Subtree index in array.
   */
  protected <A> int chooseLeastOverlap(A options, ArrayAdapter<? extends SpatialComparable, A> getter, SpatialComparable obj) {
    final int size = getter.size(options);
    assert (size > 0) : "Choose from empty set?";
    // R*-Tree: overlap increase for leaves.
    int best = -1;
    double least_overlap = Double.POSITIVE_INFINITY;
    double least_areainc = Double.POSITIVE_INFINITY;
    double least_area = Double.POSITIVE_INFINITY;
    // least overlap increase, on reduced candidate set:
    for(int i = 0; i < size; i++) {
      // Existing object and extended rectangle:
      SpatialComparable entry = getter.get(options, i);
      HyperBoundingBox mbr = SpatialUtil.union(entry, obj);
      // Compute relative overlap increase.
      double overlap_wout = 0.0;
      double overlap_with = 0.0;
      for(int k = 0; k < size; k++) {
        if(i != k) {
          SpatialComparable other = getter.get(options, k);
          overlap_wout += SpatialUtil.relativeOverlap(entry, other);
          overlap_with += SpatialUtil.relativeOverlap(mbr, other);
        }
      }
      double inc_overlap = overlap_with - overlap_wout;
      if(inc_overlap < least_overlap) {
        final double area = SpatialUtil.volume(entry);
        final double inc_area = SpatialUtil.volume(mbr) - area;
        // Volume increase and overlap increase:
        least_overlap = inc_overlap;
        least_areainc = inc_area;
        least_area = area;
        best = i;
      }
      else if(inc_overlap == least_overlap) {
        final double area = SpatialUtil.volume(entry);
        final double inc_area = SpatialUtil.volume(mbr) - area;
        if(inc_area < least_areainc || (inc_area == least_areainc && area < least_area)) {
          least_overlap = inc_overlap;
          least_areainc = inc_area;
          least_area = area;
          best = i;
        }
      }
    }
    assert (best > -1);
    return best;
  }

  /**
   * Choose subtree by least enlargement, with the additional criterion of
   * volume increase for ties (see R*-Tree publication).
   * 
   * @param options Options to choose from
   * @param getter Array adapter for options
   * @param obj Insertion object
   * @return Subtree index in array.
   */
  protected <A> int chooseLeastEnlargement(A options, ArrayAdapter<? extends SpatialComparable, A> getter, SpatialComparable obj) {
    final int size = getter.size(options);
    assert (size > 0) : "Choose from empty set?";
    // As in R-Tree, with a slight modification for ties
    double leastEnlargement = Double.POSITIVE_INFINITY;
    double minArea = -1;
    int best = -1;
    for(int i = 0; i < size; i++) {
      SpatialComparable entry = getter.get(options, i);
      final double area = SpatialUtil.volume(entry);
      double enlargement = SpatialUtil.volumeUnion(entry, obj) - area;
      if(enlargement < leastEnlargement) {
        leastEnlargement = enlargement;
        best = i;
        minArea = area;
      }
      else if(enlargement == leastEnlargement && area < minArea) {
        // Tie handling proposed by R*:
        best = i;
        minArea = area;
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
    @Override
    protected LeastOverlapInsertionStrategy makeInstance() {
      return LeastOverlapInsertionStrategy.STATIC;
    }
  }
}