package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Trivial pseudo-clustering that just considers all points to be noise.
 * 
 * Useful for evaluation and testing.
 * 
 * @author Erich Schubert
 */
@Title("Trivial all-noise clustering")
@Description("Returns a 'trivial' clustering which just considers all points as noise points.")
public class TrivialAllNoise extends AbstractAlgorithm<Object> implements ClusteringAlgorithm<Clustering<Model>> {
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
  public Clustering<Model> run(Database database) throws IllegalStateException {
    Clustering<Model> result = new Clustering<Model>("All-in-noise trivial Clustering", "allinnoise-clustering");
    Cluster<Model> c = new Cluster<Model>(database.getDBIDs(), true, ClusterModel.CLUSTER);
    result.addCluster(c);
    return result;
  }

  @Override
  public SimpleTypeInformation<Object> getInputTypeRestriction() {
    return TypeUtil.ANY;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}