package elki.clustering.neighborhood;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.neighborhood.helper.ClosedNeighborhoodSetGenerator;
import elki.clustering.neighborhood.helper.NearestNeighborClosedNeighborhoodSetGenerator;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;


public class Enforce<O> implements ClusteringAlgorithm<Clustering<Model>> {

    private final ClusteringAlgorithm<Clustering<Model>> baseAlgorithm;
    private final ClosedNeighborhoodSetGenerator<O> closedNeighborhoodSetGenerator;

    public Enforce(ClusteringAlgorithm<Clustering<Model>> baseAlgorithm, ClosedNeighborhoodSetGenerator<O> closedNeighborhoodSetGenerator) {
        this.baseAlgorithm = baseAlgorithm;
        this.closedNeighborhoodSetGenerator = closedNeighborhoodSetGenerator;
    }

    public Clustering<Model>  run(Database db){
        Clustering<Model> baseResult = baseAlgorithm.autorun(db);

        Relation<O> relation = db.getRelation(closedNeighborhoodSetGenerator.getInputTypeRestriction());
        DBIDs[] closedNeighborhoods = closedNeighborhoodSetGenerator.getClosedNeighborhoods(relation);

        int clusterAmount = baseResult.getAllClusters().size();
        ModifiableDBIDs[] finalCluster = new ModifiableDBIDs[clusterAmount];
        for(int i = 0; i < clusterAmount; i++){
            finalCluster[i] = DBIDUtil.newArray();
        }

        for (DBIDs closedNeighborhood : closedNeighborhoods) {
            int[] clusterCounter = new int[clusterAmount];
            int clusterIndex = 0;

            for (It<Cluster<Model>> cluster = baseResult.iterToplevelClusters(); cluster.valid(); cluster.advance()) {
                DBIDs clusterDBIDs = cluster.get().getIDs();
                for (DBIDIter cnsElement = closedNeighborhood.iter(); cnsElement.valid(); cnsElement.advance()) {
                    if (clusterDBIDs.contains(cnsElement)) {
                        clusterCounter[clusterIndex]++;
                    }
                }
                clusterIndex++;
            }

            int modeClusterIndex = findMaxIndex(clusterCounter);
            finalCluster[modeClusterIndex].addDBIDs(closedNeighborhood);
        }

        Clustering<Model> clustering = new Clustering<>();

        for(int i = 0; i < clusterAmount; i++){
            if(finalCluster[i].size() > 0) {
                clustering.addToplevelCluster(new Cluster<>(finalCluster[i]));
            }
        }

        return clustering;
    }


    private int findMaxIndex(int[] values){
        int maxIndex = -1;
        int maxValue = Integer.MIN_VALUE;
        for(int i = 0; i< values.length; i++){
            if(values[i] > maxValue){
                maxIndex = i;
                maxValue = values[i];
            }
        }
        return maxIndex;
    }

    @Override
    public TypeInformation[] getInputTypeRestriction() {
        return new TypeInformation[0];
    }

    public static class Par<V extends NumberVector> implements Parameterizer {

        public static final OptionID CNS_TYPE = new OptionID("closedneighborhoodset.neighborhoodrelation", "Type of neighborhood - knn/kmn");
        protected ClosedNeighborhoodSetGenerator<V> closedNeighborhoodSetGenerator;

        public static ClusteringAlgorithm<Clustering<Model>> baseAlgorithm;

        @Override
        public void configure(Parameterization config){

            new ObjectParameter<ClosedNeighborhoodSetGenerator<V>>(CNS_TYPE, ClosedNeighborhoodSetGenerator.class, NearestNeighborClosedNeighborhoodSetGenerator.class)
                    .grab(config, x -> closedNeighborhoodSetGenerator = x);

            new ObjectParameter<ClusteringAlgorithm<Clustering<Model>>>(Utils.ALGORITHM_ID, ClusteringAlgorithm.class)
                    .grab(config, x -> baseAlgorithm = x);

        }

        @Override
        public ClusteringAlgorithm<Clustering<Model>> make() {
            return new Enforce<>(baseAlgorithm, closedNeighborhoodSetGenerator);
        }
    }



}



