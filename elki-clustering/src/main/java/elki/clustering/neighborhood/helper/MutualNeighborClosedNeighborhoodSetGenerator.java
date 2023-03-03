package elki.clustering.neighborhood.helper;

import elki.data.type.TypeInformation;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.helper.MutualNeighborQuery;
import elki.helper.MutualNeighborQueryBuilder;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

import java.util.ArrayList;

public class MutualNeighborClosedNeighborhoodSetGenerator<O> extends AbstractClosedNeighborhoodSetGenerator<O> {

    private final int k;
    private final int kPlus;
    private final Distance<? super O> distance;

    public MutualNeighborClosedNeighborhoodSetGenerator(int k, Distance<? super O> distance){
        this.k = k;
        this.kPlus = k+1;
        this.distance = distance;
    }
    @Override
    public StaticDBIDs[] getClosedNeighborhoods(Relation<O> relation) {


        MutualNeighborQuery<DBIDRef> kmnQuery = new MutualNeighborQueryBuilder<>(relation, distance).precomputed().byDBID(kPlus);

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
            DFSaddComponents(element, kmnQuery, visited, connectedComponents.get(currentComponentIndex++));
        }

        StaticDBIDs[] finalComponents = new StaticDBIDs[currentComponentIndex];

        for(int i = 0; i < currentComponentIndex; i++){
            finalComponents[i] = DBIDUtil.makeUnmodifiable(connectedComponents.get(i));
        }

        return finalComponents;
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
        return distance.getInputTypeRestriction();
    }

    private void DFSaddComponents(DBIDRef element, MutualNeighborQuery<DBIDRef> kmn, WritableDataStore<Boolean> visited, ModifiableDBIDs component){
        if(!visited.get(element)){
            visited.put(element, true);
            component.add(element);

            for(DBIDIter neighbors = kmn.getMutualNeighbors(element, kPlus).iter(); neighbors.valid(); neighbors.advance()){
                DFSaddComponents(neighbors, kmn, visited, component);
            }
        }
    }

    public static class Par<O> extends AbstractClosedNeighborhoodSetGenerator.Par<O>{

        OptionID K_NEIGHBORS = new OptionID("closedNeighborhoodSet.k", "The amount of neighbors to consider to create the closed neighborhood set.");
        private int k;

        @Override
        public void configure(Parameterization config){
            super.configure(config);

            new IntParameter(K_NEIGHBORS, 2)
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT)
                    .grab(config, p-> k = p);

        }

        @Override
        public Object make() {
            return new MutualNeighborClosedNeighborhoodSetGenerator<>(k, distance) ;
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
            return new MutualNeighborClosedNeighborhoodSetGenerator<>(k, distance);
        }

        @Override
        public TypeInformation getInputTypeRestriction() {
            return distance.getInputTypeRestriction();
        }
    }
}
