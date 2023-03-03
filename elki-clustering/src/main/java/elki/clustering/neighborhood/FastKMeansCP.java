package elki.clustering.neighborhood;

import elki.clustering.kmeans.AbstractKMeans;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.neighborhood.helper.ClosedNeighborhoodSetGenerator;
import elki.clustering.neighborhood.helper.MutualNeighborClosedNeighborhoodSetGenerator;
import elki.clustering.neighborhood.model.CNSrepresentor;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.math.linearalgebra.VMath;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FastKMeansCP<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {

    private static final Logging LOG = Logging.getLogger(FastKMeansCP.class);
    private final ClosedNeighborhoodSetGenerator<V> closedNeighborhoodSetGenerator;


    public FastKMeansCP(int kCluster, int maxiter, KMeansInitialization initializer, ClosedNeighborhoodSetGenerator<V> closedNeighborhoodSetGenerator){
        super(kCluster, maxiter, initializer);
        this.closedNeighborhoodSetGenerator = closedNeighborhoodSetGenerator;
    }

    @Override
    public Clustering<KMeansModel> run(Relation<V> rel){
        Instance instance = new Instance(rel, distance, initialMeans(rel));
        instance.run(maxiter);
        return instance.buildResult();
    }

    @Override
    protected Logging getLogger() {
        return LOG;
    }



    protected class Instance extends AbstractKMeans.Instance {

        protected DBIDs[] CNSs;
        protected CNSrepresentor[] cnsRepresentors;
        protected List<List<CNSrepresentor>> CnsClusters;

        protected Map<CNSrepresentor, Integer> cnsAssignment;


        /**
         * Constructor.
         *
         * @param relation Relation to process
         * @param df Distance function
         * @param means    Initial mean
         */
        public Instance(Relation<V> relation, NumberVectorDistance<?> df, double[][] means) {
            super(relation, df, means);
            CNSs = closedNeighborhoodSetGenerator.getClosedNeighborhoods(relation);
            cnsRepresentors = initalizeCNSrepresentors(CNSs);
            CnsClusters = new ArrayList<>(k);
            for(int i = 0; i < k; i++){
                CnsClusters.add(new ArrayList<>());
            }
            cnsAssignment = new HashMap<>(CNSs.length);

        }

        @Override
        protected int iterate(int iteration) {
            means = iteration == 1 ? means : weightedMeans(CnsClusters, means);
            return assignToNearestCluster(cnsRepresentors, means);
        }

        @Override
        protected Logging getLogger() {
            return LOG;
        }

        /**
         * Creates a representative for each closed neighborhood set.
         * @param closedNeighborhoodSets closed neighborhood sets to operate on
         * @return representative consisting of mean and sizer of set
         */
        private CNSrepresentor[] initalizeCNSrepresentors(DBIDs[] closedNeighborhoodSets){

            int dim = RelationUtil.dimensionality(relation);

            CNSrepresentor[] representors = new CNSrepresentor[closedNeighborhoodSets.length];

            for(int currentCNS = 0; currentCNS < closedNeighborhoodSets.length; currentCNS++  ){
                double[] mean = new double[dim];
                int CNSsize =  closedNeighborhoodSets[currentCNS].size();
                for(DBIDIter element = closedNeighborhoodSets[currentCNS].iter(); element.valid(); element.advance()){
                    VMath.plusEquals(mean, relation.get(element).toArray());
                }
                VMath.timesEquals(mean, 1.0 / CNSsize);

                representors[currentCNS] = new CNSrepresentor(mean, CNSsize, closedNeighborhoodSets[currentCNS]);
            }

            return  representors;
        }

        protected double[][] weightedMeans(List<List<CNSrepresentor>> clusters, double[][]means){
            final int k = means.length;
            double[][] newMeans = new double[k][];
            for(int clusterIndex = 0; clusterIndex < k; clusterIndex++){
                List<CNSrepresentor> cluster = clusters.get(clusterIndex);
                if(cluster.size() == 0){
                    newMeans[clusterIndex] = means[clusterIndex];
                    continue;
                }

                int amountElements = cluster.get(0).size;
                double[] sum = VMath.times(cluster.get(0).cnsMean, cluster.get(0).size);

                for(int i = 1; i < cluster.size(); i++ ){
                    VMath.plusEquals(sum, VMath.times(cluster.get(i).cnsMean, cluster.get(i).size));
                    amountElements += cluster.get(i).size;
                }
                newMeans[clusterIndex] = VMath.timesEquals(sum, 1.0/ amountElements);
            }
            return newMeans;
        }

        protected int assignToNearestCluster(CNSrepresentor[] representatives, double[][] means) {
            int changed = 0;

            for(List<CNSrepresentor> cluster : CnsClusters){
                cluster.clear();
            }

            for(CNSrepresentor representative: representatives) {
                NumberVector cnsMean = DoubleVector.wrap(representative.cnsMean);

                double minDist = distance.distance(cnsMean, DoubleVector.wrap(means[0]));
                int minIndex = 0;
                for (int i = 1; i < k; i++) {
                    double dist = distance.distance(cnsMean, DoubleVector.wrap(means[i]));
                    if (dist < minDist) {
                        minIndex = i;
                        minDist = dist;
                    }
                }
                varsum[minIndex] += isSquared ? minDist : (minDist * minDist);
                CnsClusters.get(minIndex).add(representative);
                if( !((Integer)minIndex).equals(cnsAssignment.put(representative, minIndex)) ) {
                    changed += representative.size;
                }
            }

            return changed;
        }

        @Override
        public Clustering<KMeansModel> buildResult() {
            for(int i = 0; i < CnsClusters.size(); i++){
                for(CNSrepresentor cns : CnsClusters.get(i)){
                    clusters.get(i).addDBIDs(cns.cnsElements);
                }
            }

            return super.buildResult();
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
        public FastKMeansCP<V> make() {
            return new FastKMeansCP<>( k, maxiter, initializer, closedNeighborhoodSetGenerator);
        }
    }


}

