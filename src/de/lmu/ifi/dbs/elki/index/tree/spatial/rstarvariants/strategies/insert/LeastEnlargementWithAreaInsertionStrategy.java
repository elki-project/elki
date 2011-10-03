package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * A slight modification of the default R-Tree insertion strategy: find rectangle with least volume
 * enlargement, but choose least area on ties.
 * 
 * Proposed for non-leaf entries in:
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
public class LeastEnlargementWithAreaInsertionStrategy implements InsertionStrategy {
  /**
   * Static instance.
   */
  public static final LeastEnlargementWithAreaInsertionStrategy STATIC = new LeastEnlargementWithAreaInsertionStrategy();

  /**
   * Constructor.
   */
  public LeastEnlargementWithAreaInsertionStrategy() {
    super();
  }

  @Override
  public <A> int choose(A options, ArrayAdapter<? extends SpatialComparable, A> getter, SpatialComparable obj, int height, int depth) {
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
    protected LeastEnlargementWithAreaInsertionStrategy makeInstance() {
      return LeastEnlargementWithAreaInsertionStrategy.STATIC;
    }
  }
}