package elki.helper;

import elki.database.ids.DBIDRef;
import elki.database.query.QueryBuilder;
import elki.database.relation.Relation;
import elki.distance.Distance;

public class MutualNeighborQueryBuilder<O> {

    Relation<O> relation;
    Distance<? super O> distance;
    boolean precomputed = false;
    boolean useRKNN = false;

    public MutualNeighborQueryBuilder(Relation<O> relation, Distance<? super O> distance){
        this.relation = relation;
        this.distance = distance;
    }

    public MutualNeighborQueryBuilder<O> precomputed(){
        precomputed = true;
        return this;
    }

    public MutualNeighborQueryBuilder<O> useRKNN(){
        useRKNN = true;
        return this;
    }

    public MutualNeighborQuery<DBIDRef> byDBID(int maxK){
        QueryBuilder<O> qb = new QueryBuilder<>(relation, distance).precomputed();
        if(precomputed){
            qb = qb.precomputed();
        }
        if(useRKNN) {
            return new MutualNeighborQueryViaKNNrKNNbyDBID(qb.kNNByDBID(maxK), qb.rKNNByDBID(maxK));
        }else{
            return new MutualNeighborQueryViaKNNbyDBID(qb.kNNByDBID(maxK));
        }
    }
}
