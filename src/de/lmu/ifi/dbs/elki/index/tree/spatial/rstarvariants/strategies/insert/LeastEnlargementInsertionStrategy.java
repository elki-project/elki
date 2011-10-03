package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * The default R-Tree insertion strategy: find rectangle with least volume
 * enlargement.
 * 
 * <p>
 * Antonin Guttman:<br/>
 * R-Trees: A Dynamic Index Structure For Spatial Searching<br />
 * in Proceedings of the 1984 ACM SIGMOD international conference on Management
 * of data.
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "Antonin Guttman", title = "R-Trees: A Dynamic Index Structure For Spatial Searching", booktitle = "Proceedings of the 1984 ACM SIGMOD international conference on Management of data", url = "http://dx.doi.org/10.1145/971697.602266")
public class LeastEnlargementInsertionStrategy implements InsertionStrategy {
  /**
   * Static instance.
   */
  public static final LeastEnlargementInsertionStrategy STATIC = new LeastEnlargementInsertionStrategy();

  /**
   * Constructor.
   */
  public LeastEnlargementInsertionStrategy() {
    super();
  }

  @Override
  public <A> int choose(A options, ArrayAdapter<? extends SpatialComparable, A> getter, SpatialComparable obj, int height, int depth) {
    final int size = getter.size(options);
    assert (size > 0) : "Choose from empty set?";
    double leastEnlargement = Double.POSITIVE_INFINITY;
    int best = -1;
    for(int i = 0; i < size; i++) {
      SpatialComparable entry = getter.get(options, i);
      double enlargement = SpatialUtil.volumeUnion(entry, obj) - SpatialUtil.volume(entry);
      if(enlargement < leastEnlargement) {
        leastEnlargement = enlargement;
        best = i;
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
    protected LeastEnlargementInsertionStrategy makeInstance() {
      return LeastEnlargementInsertionStrategy.STATIC;
    }
  }
}