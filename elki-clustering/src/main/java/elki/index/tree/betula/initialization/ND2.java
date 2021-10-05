package elki.index.tree.betula.initialization;

import elki.data.NumberVector;
import elki.index.tree.betula.features.ClusterFeature;

/**
 * D2 distance based similarity for initialization
 * 
 * @author Andreas Lang
 */
public class ND2 implements CFIDistance {
  @Override
  public double squaredDistance(NumberVector clusterCenter, ClusterFeature candidate) {
    final int dim = clusterCenter.getDimensionality();
    assert (dim == candidate.getDimensionality());
    double sum = 0;
    double div = 1. / candidate.getWeight();
    for(int d = 0; d < dim; d++) {
      double delta = candidate.centroid(d) - clusterCenter.doubleValue(d);
      sum += (delta * delta);
    }
    sum += div * candidate.sumdev();
    return sum > 0 ? sum * candidate.getWeight() : 0;
  }

  @Override
  public double squaredDistance(double[] clusterCenter, ClusterFeature candidate) {
    final int dim = clusterCenter.length;
    assert (dim == candidate.getDimensionality());
    double sum = 0;
    double div = 1. / candidate.getWeight();
    for(int d = 0; d < dim; d++) {
      double delta = candidate.centroid(d) - clusterCenter[d];
      sum += (delta * delta);
    }
    sum += div * candidate.sumdev();
    return sum > 0 ? sum * candidate.getWeight() : 0;
  }

  @Override
  public double squaredDistance(ClusterFeature clusterCenter, ClusterFeature candidate) {
    final int dim = clusterCenter.getDimensionality();
    assert (dim == candidate.getDimensionality());
    double div1 = 1. / clusterCenter.getWeight();
    double div2 = 1. / candidate.getWeight();
    double sum = 0;
    for(int d = 0; d < dim; d++) {
      double delta = clusterCenter.centroid(d) - candidate.centroid(d);
      sum += (delta * delta);
    }
    sum += div1 * clusterCenter.sumdev() + div2 * candidate.sumdev();
    return sum > 0 ? sum * candidate.getWeight() : 0;
  }
}
