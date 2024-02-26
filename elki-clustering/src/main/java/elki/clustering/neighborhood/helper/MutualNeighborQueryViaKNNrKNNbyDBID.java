package elki.clustering.neighborhood.helper;

import elki.database.ids.*;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.rknn.RKNNSearcher;


public class MutualNeighborQueryViaKNNrKNNbyDBID implements MutualNeighborQuery<DBIDRef> {
    KNNSearcher<DBIDRef> knn;
    RKNNSearcher<DBIDRef> rknn;

    public MutualNeighborQueryViaKNNrKNNbyDBID(KNNSearcher<DBIDRef> knn, RKNNSearcher<DBIDRef> rknn){
        this.knn = knn;
        this.rknn = rknn;
    }

    @Override
    public DBIDs getMutualNeighbors(DBIDRef query, int k){
        KNNList knns = knn.getKNN(query, k);
        DoubleDBIDList rknns = rknn.getRKNN(query, k);

        ModifiableDBIDs kmns = DBIDUtil.newArray(k);

        for (DoubleDBIDListIter neighbors = knns.iter(); neighbors.valid(); neighbors.advance()){
            if(DBIDUtil.equal(query, neighbors)){
                continue;
            }
            if(rknns.contains(neighbors)){
                kmns.add(neighbors);
            }
        }
        return kmns;
    }


}
