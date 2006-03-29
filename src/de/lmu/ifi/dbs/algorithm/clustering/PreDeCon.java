package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.preprocessing.FourCPreprocessor;
import de.lmu.ifi.dbs.preprocessing.PreDeConPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;

/**
 * Provides the PreDeCon algorithm.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PreDeCon extends ProjectedDBSCAN<PreDeConPreprocessor>
{

    /**
     * Provides the PreDeCon algorithm.
     */
    public PreDeCon()
    {
        super();
    }


    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("PreDeCon", "Computing Correlation Connected Clusters", "4C identifies local subgroups of data objects sharing a uniform correlation. The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).", "Christian B\u00F6hm, Karin Kailing, Peer Kr\u00F6ger, Arthur Zimek: Computing Clusters of Correlation Connected Objects, Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004, 455-466");
    }


    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.algorithm.clustering.ProjectedDBSCAN#preprocessorClass()
     */
    @Override
    public Class<PreDeConPreprocessor> preprocessorClass()
    {
        return PreDeConPreprocessor.class;
    }

    
}
