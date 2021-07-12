package elki.clustering.hierarchical.betula.initialization;

import elki.clustering.hierarchical.betula.CFInterface;
import elki.data.NumberVector;

public class ND2 implements CFIDistance {

    @Override
    public double squaredDistance(NumberVector clusterCenter, CFInterface candidate) {
        final int dim = clusterCenter.getDimensionality();
        assert (dim == candidate.getDimensionality());
        double sum = 0;
        double div = 1. / candidate.getWeight();
        for(int d = 0; d < dim; d++) {
            double delta = candidate.centroid(d) - clusterCenter.doubleValue(d);
            sum += (delta * delta);
        }
        sum += div * candidate.SoD();
        return sum > 0 ? sum * candidate.getWeight() : 0;
    }

    @Override
    public double squaredDistance(double[] clusterCenter, CFInterface candidate) {
        final int dim = clusterCenter.length;
        assert (dim == candidate.getDimensionality());
        double sum = 0;
        double div = 1. / candidate.getWeight();
        for(int d = 0; d < dim; d++) {
            double delta = candidate.centroid(d) - clusterCenter[d];
            sum += (delta * delta);
        }
        sum += div * candidate.SoD();
        return sum > 0 ? sum * candidate.getWeight() : 0;
    }

    @Override
    public double squaredDistance(CFInterface clusterCenter, CFInterface candidate) {
        final int dim = clusterCenter.getDimensionality();
        assert (dim == candidate.getDimensionality());
        double div1 = 1. / clusterCenter.getWeight();
        double div2 = 1. / candidate.getWeight();
        double sum = 0;
        for(int d = 0; d < dim; d++) {
            double delta = clusterCenter.centroid(d) - candidate.centroid(d);
            sum += (delta * delta);
        }
        sum += div1 * clusterCenter.SoD() + div2 * candidate.SoD();
        return sum > 0 ? sum * candidate.getWeight() : 0;
    }

}
