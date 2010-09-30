package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Trivial pseudo-clustering that just considers all points to be one big
 * cluster.
 * 
 * Useful for evaluation and testing.
 * 
 * @param <O> Object type
 * 
 * @author Erich Schubert
 */
@Title("Trivial all-in-one clustering")
@Description("Returns a 'tivial' clustering which just considers all points to be one big cluster.")
public class TrivialAllInOne<O extends DatabaseObject> extends AbstractAlgorithm<O, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>, O> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(TrivialAllInOne.class);
  
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public TrivialAllInOne() {
    super();
  }

  /**
   * Run the actual clustering algorithm.
   * 
   * @param database The database to process
   */
  @Override
  protected Clustering<Model> runInTime(Database<O> database) throws IllegalStateException {
    Clustering<Model> result = new Clustering<Model>("All-in-one trivial Clustering", "allinone-clustering");
    Cluster<Model> c = new Cluster<Model>(database.getIDs(), ClusterModel.CLUSTER);
    result.addCluster(c);
    return result;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return Clustering Algorithm
   */
  public static <O extends DatabaseObject> TrivialAllInOne<O> parameterize(Parameterization config) {
    if(config.hasErrors()) {
      return null;
    }
    return new TrivialAllInOne<O>();
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}