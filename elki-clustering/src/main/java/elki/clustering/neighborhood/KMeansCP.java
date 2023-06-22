package elki.clustering.neighborhood;

import elki.clustering.kmeans.AbstractKMeans;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.neighborhood.helper.ClosedNeighborhoodSetGenerator;
import elki.clustering.neighborhood.helper.MutualNeighborClosedNeighborhoodSetGenerator;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.logging.statistics.Duration;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import java.util.Arrays;

/**
 * Create neighborhood consistent clusterings by calculating closed neighborhood sets and run lloyd-iteration for these sets.
 */
public class KMeansCP<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {

    private static final Logging LOG = Logging.getLogger(KMeansCP.class);
    private final ClosedNeighborhoodSetGenerator<V> closedNeighborhoodSetGenerator;

    public KMeansCP(int kCluster, int maxiter, KMeansInitialization initializer, ClosedNeighborhoodSetGenerator<V> closedNeighborhoodSetGenerator) {
        super(kCluster, maxiter, initializer);
        this.closedNeighborhoodSetGenerator = closedNeighborhoodSetGenerator;
    }

    @Override
    protected Logging getLogger() {
        return LOG;
    }

    @Override
    public Clustering<KMeansModel> run(Relation<V> rel) {
        Instance instance = new Instance(rel, distance, initialMeans(rel));
        instance.run(maxiter);
        return instance.buildResult();
    }

    protected DBIDs[] getCNS(Relation<V> relation){
        Duration cnsTime = LOG.newDuration(closedNeighborhoodSetGenerator.getClass().getName() + ".time").begin();
        DBIDs[] dbids = closedNeighborhoodSetGenerator.getClosedNeighborhoods(relation);
        LOG.statistics(cnsTime.end());
        return dbids;
    }


    protected class Instance extends AbstractKMeans.Instance {

        protected DBIDs[] CNSs;

        /**
         * Constructor.
         *
         * @param relation Relation to process
         * @param df Distance function
         * @param means    Initial mean
         */
        public Instance(Relation<V> relation, NumberVectorDistance<?> df, double[][] means) {
            super(relation, df, means);
            CNSs = getCNS(relation);
        }

        @Override
        protected int iterate(int iteration) {
            means =  iteration == 1 ? means : means(clusters, means, relation);
            return assignCNSToBestCluster();
        }

        protected int assignCNSToBestCluster(){
            int changed = 0;
            Arrays.fill(varsum, 0.);

            for(ModifiableDBIDs cluster : clusters){
                cluster.clear();
            }

            for(DBIDs cns : CNSs){
                int minIndex = 0;
                double minDist = sumOfCNSDistance(cns, means[0]);
                for(int i = 1; i < k; i++){
                    double currentDistance = sumOfCNSDistance(cns, means[i]);
                    if(currentDistance < minDist){
                        minDist = currentDistance;
                        minIndex = i;
                    }
                }
                varsum[minIndex] += isSquared ? minDist : (minDist * minDist);
                clusters.get(minIndex).addDBIDs(cns);

                changed += assignCNS(cns, minIndex);
            }
            return changed;

        }

        protected int assignCNS(DBIDs cns, int clusterIndex){
            int changed = 0;
            for(DBIDIter element = cns.iter(); element.valid(); element.advance()) {
                if (assignment.putInt(element, clusterIndex) != clusterIndex){
                    changed++;
                }
            }
            return  changed;
        }

        protected double sumOfCNSDistance(DBIDs cns, double[] mean){
            double distanceSum = 0;

            for(DBIDIter element = cns.iter(); element.valid();element.advance()){
                distanceSum += distance(relation.get(element), mean);
            }
            return distanceSum;
        }

        @Override
        protected Logging getLogger() {
            return LOG;
        }
    }

    public static class Par<V extends NumberVector> extends AbstractKMeans.Par<V> {

        protected ClosedNeighborhoodSetGenerator<V> closedNeighborhoodSetGenerator;

        @Override
        public void configure(Parameterization config){
            super.configure(config);

            new ObjectParameter<ClosedNeighborhoodSetGenerator<V>>(ClosedNeighborhoodSetGenerator.CNS_GENERATOR_ID, ClosedNeighborhoodSetGenerator.class, MutualNeighborClosedNeighborhoodSetGenerator.class)
                    .grab(config, x -> closedNeighborhoodSetGenerator = x);

            config.descend(Utils.DISTANCE_FUNCTION_ID);

        }

        @Override
        public KMeansCP<V> make() {
            return new KMeansCP<>( k, maxiter, initializer, closedNeighborhoodSetGenerator);
        }
    }
}
