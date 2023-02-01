package elki.helper;

import elki.database.ids.*;
import elki.database.query.knn.KNNSearcher;

public class MutualNeighborQueryViaKNNbyDBID implements MutualNeighborQuery<DBIDRef>{

    KNNSearcher<DBIDRef> knn;
    MutualNeighborQueryViaKNNbyDBID(KNNSearcher<DBIDRef> knn){
        this.knn = knn;
    }
    @Override
    public DBIDs getMutualNeighbors(DBIDRef query, int k) {
        KNNList neighbors = knn.getKNN(query, k);

        ArrayModifiableDBIDs mutualNearestNeighbors = DBIDUtil.newArray(k);

        for (DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()){

            if (!DBIDUtil.equal(query,neighbor) && knn.getKNN(neighbor, k).contains(query)) {
                mutualNearestNeighbors.add(neighbor);
            }
        }
        return mutualNearestNeighbors;
    }
}
