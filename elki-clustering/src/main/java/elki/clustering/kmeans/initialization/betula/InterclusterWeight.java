package elki.clustering.kmeans.initialization.betula;

import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.documentation.Reference;

/**
 * Initialization via n2 * D2²(cf1, cf2), which supposedly is closes to the idea
 * of k-means++ initialization.
 * <p>
 * References:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems
 * 
 * @author Andreas Lang
 */
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems")
public class InterclusterWeight implements CFInitWeight {
  @Override
  public double squaredWeight(ClusterFeature existing, ClusterFeature candidate) {
    // Optimized n2 * D2²(cf1, cf2)
    return candidate.sumdev() + candidate.getWeight() * //
        (existing.sumdev() / existing.getWeight() + SquaredEuclideanDistance.STATIC.distance(existing, candidate));
  }
}
