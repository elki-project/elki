package elki.clustering.kmeans.covertree;

import java.util.Arrays;

import elki.clustering.kmeans.covertree.KMeansCoverTree.Node;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.math.MathUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

public class SExpCoverTreeKMeans<V extends NumberVector> extends FastCoverTreeKMeans<V> {

    int switchover;

    public SExpCoverTreeKMeans(int k, int maxiter, KMeansInitialization initializer, boolean varstat, double expansion, int trunc, int switchover) {
        super(k, maxiter, initializer, varstat, expansion, trunc);
        this.switchover = switchover;
    }

    /**
     * The logger for this class.
     */
    private static final Logging LOG = Logging.getLogger(FastCoverTreeKMeans.class);

    @Override
    public Clustering<KMeansModel> run(Relation<V> relation) {
        KMeansCoverTree<V> tree = new KMeansCoverTree<V>(relation, EuclideanDistance.STATIC, expansion, trunc, true);
        tree.initialize();
        Instance instance = new Instance(relation, distance, initialMeans(relation), tree, switchover);
        instance.run(maxiter);
        instance.generateCover();
        instance.printLog();
        return instance.buildResult(varstat, relation);
    }

    @Override
    protected Logging getLogger() {
        return LOG;
    }

    protected static class Instance extends SHamCoverTreeKMeans.Instance {

        int[] cnum[];

        public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means, KMeansCoverTree<? extends NumberVector> tree, int switchover) {
            super(relation, df, means, tree, switchover);
            cnum = new int[k][k - 1];
        }

        @Override
        protected int iterate(int iteration) {
            if(iteration == 1) {
                int changed = initialAssignToNearestCluster();
                return changed;
            }
            if(iteration < switchover) {
                meansFromSumsCT(means, nodeManager.getSums(), means);
                int changed = assignToNearestCluster();
                assert (testSizes());
                assert (nodeManager.testTree(tree.getRoot(), false));
                return changed;
            }
            if(iteration == switchover) {
                meansFromSumsCT(means, nodeManager.getSums(), means);
                int changed = assignToClusterBounds();
                assert (testSizes());
                assert (nodeManager.testTree(tree.getRoot(), false));
                return changed;
            }
            meansFromSums(newmeans, nodeManager.getSums(), means);
            movedDistance(means, newmeans, sep);
            updateBounds(sep);
            copyMeans(newmeans, means);
            return assignPointsToNearestCluster();
        }

        protected int assignPointsToNearestCluster() {
            recomputeSeperation(sep, scdist);
            nearestMeans(scdist, cnum);
            int changed = 0;
            for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
                final int orig = assignment.intValue(it);
                // Compute the current bound:
                final double z = lower.doubleValue(it);
                final double sa = sep[orig];
                double u = upper.doubleValue(it);
                if(u <= z || u <= sa) {
                    continue;
                }
                // Update the upper bound
                NumberVector fv = relation.get(it);
                double curd2 = distance(fv, means[orig]);
                upper.putDouble(it, u = isSquared ? Math.sqrt(curd2) : curd2);
                if(u <= z || u <= sa) {
                    continue;
                }
                double r = u + 0.5 * sa; // Our cdist are scaled 0.5
                // Find closest center, and distance to two closest centers
                double min1 = curd2, min2 = Double.POSITIVE_INFINITY;
                int cur = orig;
                for(int i = 0; i < k - 1; i++) {
                    final int c = cnum[orig][i]; // Optimized ordering
                    if(scdist[orig][c] > r) {
                        break;
                    }
                    double dist = distance(fv, means[c]);
                    if(dist < min1) {
                        cur = c;
                        min2 = min1;
                        min1 = dist;
                    }
                    else if(dist < min2) {
                        min2 = dist;
                    }
                }
                // Object has to be reassigned.
                if(cur != orig) {
                    assignment.putInt(it, cur);
                    plusMinusEquals(nodeManager.getSums()[cur], nodeManager.getSums()[orig], fv);
                    ++changed;
                    upper.putDouble(it, min1 == curd2 ? u : isSquared ? Math.sqrt(min1) : min1);
                }
                lower.putDouble(it, min2 == curd2 ? u : isSquared ? Math.sqrt(min2) : min2);
            }
            return changed;
        }

        @Override
        protected Logging getLogger() {
            return LOG;
        }

    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par<V extends NumberVector> extends AbstractCoverTreeKMeans.Par<V> {

        public static final OptionID SWITCH_ID = new OptionID("covertree.switch", "Switches from covertree to Hamerly.");

        int switchover;

        @Override
        protected boolean needsMetric() {
            return true;
        }

        protected void getParameterSwitch(Parameterization config) {
            new IntParameter(SWITCH_ID, 5) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
                    .grab(config, x -> switchover = x);
        }

        @Override
        public void configure(Parameterization config) {
            super.configure(config);
            getParameterSwitch(config);
        }

        @Override
        public SExpCoverTreeKMeans<V> make() {
            return new SExpCoverTreeKMeans<>(k, maxiter, initializer, varstat, expansion, trunc, switchover);
        }
    }

}
