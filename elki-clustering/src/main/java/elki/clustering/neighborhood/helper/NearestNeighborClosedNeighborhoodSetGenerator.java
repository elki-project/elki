package elki.clustering.neighborhood.helper;

import elki.data.type.TypeInformation;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.rknn.RKNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;

import java.util.ArrayList;

public class NearestNeighborClosedNeighborhoodSetGenerator<O> implements ClosedNeighborhoodSetGenerator<O> {

    int k;
    int kPlus;
    Distance<? super O> distance;

    /**
     * Generate a closed neighborhood set for the KNN neighborhood relation.
     * This is a weak-connected component in the corresponding neighborhood graph.
     * @param k number of neighbors (excluding the origin)
     * @param distance rank neighbors based on this distance
     */
    NearestNeighborClosedNeighborhoodSetGenerator(int k, Distance<? super O> distance){
       this.k = k;
       this.distance = distance;
       this.kPlus = k+1;
    }


    public StaticDBIDs[] getClosedNeighborhoods(Relation<O> relation) {
        KNNSearcher<DBIDRef> knnQuery = new QueryBuilder<>(relation, distance).precomputed().kNNByDBID(kPlus);
        RKNNSearcher<DBIDRef> rKnnQuery = new QueryBuilder<>(relation, distance).precomputed().rKNNByDBID(kPlus);

        WritableDataStore<Boolean> visited = DataStoreFactory.FACTORY.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB, Boolean.class);
        ArrayList<ModifiableDBIDs> connectedComponents = new ArrayList<>();

        for (DBIDIter element = relation.iterDBIDs(); element.valid(); element.advance()){
            visited.put(element, false);
        }

        int currentComponentIndex = 0;
        for(DBIDIter element = relation.iterDBIDs(); element.valid(); element.advance()){
            if(visited.get(element)){
                continue;
            }
            connectedComponents.add(currentComponentIndex, DBIDUtil.newArray());
            DFSaddComponents(element, knnQuery, rKnnQuery, visited, connectedComponents.get(currentComponentIndex++));
        }

        StaticDBIDs[] finalComponents = new StaticDBIDs[currentComponentIndex];

        for(int i = 0; i < currentComponentIndex; i++){
            finalComponents[i] = DBIDUtil.makeUnmodifiable(connectedComponents.get(i));
        }

        return finalComponents;
    }

    private void DFSaddComponents(DBIDRef element, KNNSearcher<DBIDRef> knn, RKNNSearcher<DBIDRef> rknn, WritableDataStore<Boolean> visited, ModifiableDBIDs component){
        if(!visited.get(element)){

            visited.put(element, true);
            component.add(element);

            for(DBIDIter iter = knn.getKNN(element, kPlus).iter(); iter.valid(); iter.advance()){
                DFSaddComponents(iter, knn, rknn, visited, component);
            }
            for(DBIDIter iter = rknn.getRKNN(element, kPlus).iter(); iter.valid(); iter.advance()){
                DFSaddComponents(iter, knn, rknn, visited, component);
            }

        }
    }

    public static class Factory<O> implements ClosedNeighborhoodSetGenerator.Factory<O> {
        private final int k;

        /**
         * distance function to use
         */
        private final Distance<? super O> distance;

        /**
         * Factory Constructor
         */
        public Factory(int k, Distance<? super O> distance) {
            super();
            this.k = k;
            this.distance = distance;
        }

        @Override
        public ClosedNeighborhoodSetGenerator<O> instantiate() {
            return new NearestNeighborClosedNeighborhoodSetGenerator<>(k, distance);
        }

        @Override
        public TypeInformation getInputTypeRestriction() {
            return distance.getInputTypeRestriction();
        }
    }
}
