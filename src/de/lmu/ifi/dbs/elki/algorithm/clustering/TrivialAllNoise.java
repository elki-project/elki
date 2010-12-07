package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Trivial pseudo-clustering that just considers all points to be noise.
 * 
 * Useful for evaluation and testing.
 * 
 * @param <O> Object type
 * 
 * @author Erich Schubert
 */
@Title("Trivial all-noise clustering")
@Description("Returns a 'trivial' clustering which just considers all points as noise points.")
public class TrivialAllNoise<O extends DatabaseObject> extends AbstractAlgorithm<O, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>, O> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(TrivialAllNoise.class);
  
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public TrivialAllNoise() {
    super();
  }

  /**
   * Run the actual clustering algorithm.
   * 
   * @param database The database to process
   */
  @Override
  protected Clustering<Model> runInTime(Database<O> database) throws IllegalStateException {
    Clustering<Model> result = new Clustering<Model>("All-in-noise trivial Clustering", "allinnoise-clustering");
    Cluster<Model> c = new Cluster<Model>(database.getIDs(), true, ClusterModel.CLUSTER);
    result.addCluster(c);
    return result;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return Clustering Algorithm
   */
  public static <O extends DatabaseObject> TrivialAllNoise<O> parameterize(Parameterization config) {
    if(config.hasErrors()) {
      return null;
    }
    return new TrivialAllNoise<O>();
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}