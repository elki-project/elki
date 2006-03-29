package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.preprocessing.FourCPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;

/**
 * Provides the 4C algorithm.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourC extends ProjectedDBSCAN<FourCPreprocessor>
{

    /**
     * Provides the 4C algorithm.
     */
    public FourC()
    {
        super();
    }


    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("4C", "Computing Correlation Connected Clusters", "4C identifies local subgroups of data objects sharing a uniform correlation. The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).", "Christian B�hm, Karin Kailing, Peer Kr�ger, Arthur Zimek: Computing Clusters of Correlation Connected Objects, Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004, 455-466");
    }


    @Override
    public Class<FourCPreprocessor> preprocessorClass()
    {
        return FourCPreprocessor.class;
    }

    
}
