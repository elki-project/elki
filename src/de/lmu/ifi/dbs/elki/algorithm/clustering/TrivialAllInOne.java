package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;

/**
 * Trivial pseudo-clustering that just considers all points to be one big
 * cluster.
 * 
 * Useful for evaluation and testing.
 * 
 * @author Erich Schubert
 * 
 * @param <O>
 */
@Title("Trivial all-in-one clustering")
@Description("Returns a 'tivial' clustering which just considers all points to be one big cluster.")
public class TrivialAllInOne<O extends DatabaseObject> extends AbstractAlgorithm<O, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>, O> {
  /**
   * Constructor.
   */
  public TrivialAllInOne() {
    super(new EmptyParameterization());
  }

  /**
   * Holds the result of the algorithm.
   */
  private Clustering<Model> result;

  /**
   * Return clustering result
   */
  public Clustering<Model> getResult() {
    return result;
  }

  /**
   * Run the actual clustering algorithm.
   * 
   * @param database The database to process
   */
  @Override
  protected Clustering<Model> runInTime(Database<O> database) throws IllegalStateException {
    result = new Clustering<Model>();
    DatabaseObjectGroup group = new DatabaseObjectGroupCollection<List<Integer>>(database.getIDs());
    Cluster<Model> c = new Cluster<Model>(group, ClusterModel.CLUSTER);
    result.addCluster(c);
    return result;
  }
}