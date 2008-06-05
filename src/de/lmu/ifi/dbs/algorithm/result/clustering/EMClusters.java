package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;

/**
 * // todo arthur comment
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Result
 */
public class EMClusters<V extends RealVector<V, ?>> extends Clusters<V> {

    public EMClusters(Integer[][] clusters, Database<V> db) {
        super(clusters, db);
    }

    @Override
    public boolean isRequiredAssociation(AssociationID id) {
        return super.isRequiredAssociation(id) || id == AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X;
    }
}
