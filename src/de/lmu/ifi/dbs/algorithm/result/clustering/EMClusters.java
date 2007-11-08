package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;

/**
 * @author Arthur Zimek
 */
public class EMClusters<V extends RealVector<V, ?>> extends Clusters<V>
{

    public EMClusters(Integer[][] clusters, Database<V> db)
    {
        super(clusters, db);
    }
    
    public boolean isRequiredAssociation(AssociationID id)
    {
        return super.isRequiredAssociation(id) || id == AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X;
    }
}
