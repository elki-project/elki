package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Pseudo clustering using labels.
 * 
 * This "algorithm" puts elements into the same cluster when they agree in their
 * labels. I.e. it just uses a predefined clustering, and is mostly useful for
 * testing and evaluation (e.g. comparing the result of a real algorithm to a
 * reference result / golden standard).
 * 
 * If an assignment of an object to multiple clusters is desired, the labels of
 * the object indicating the clusters need to be separated by blanks and the
 * flag {@link #MULTIPLE_FLAG} needs to be set.
 * 
 * TODO: handling of data sets with no labels?
 * 
 * TODO: Noise handling (e.g. allow the user to specify a noise label pattern?)
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
@Title("Clustering by label")
@Description("Cluster points by a (pre-assigned!) label. For comparing results with a reference clustering.")
public class ByLabelClustering<O extends DatabaseObject> extends AbstractAlgorithm<O, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>, O> {

  /**
   * OptionID for {@link #MULTIPLE_FLAG}
   */
  public static final OptionID MULTIPLE_ID = OptionID.getOrCreateOptionID("bylabelclustering.multiple", "Flag to indicate that only subspaces with large coverage " + "(i.e. the fraction of the database that is covered by the dense units) " + "are selected, the rest will be pruned.");

  /**
   * Flag to indicate that multiple cluster assignment is possible. If an
   * assignment to multiple clusters is desired, the labels indicating the
   * clusters need to be separated by blanks.
   * 
   * <p>
   * Key: {@code -clique.prune}
   * </p>
   */
  private final Flag MULTIPLE_FLAG = new Flag(MULTIPLE_ID);

  /**
   * Holds the value of {@link #MULTIPLE_FLAG}.
   */
  private boolean multiple;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public ByLabelClustering(Parameterization config) {
    super(config);
    if(config.grab(MULTIPLE_FLAG)) {
      multiple = MULTIPLE_FLAG.getValue();
    }
  }

  /**
   * Constructor without parameters
   */
  public ByLabelClustering() {
    this(new EmptyParameterization());
  }

  /**
   * Run the actual clustering algorithm.
   * 
   * @param database The database to process
   */
  @Override
  protected Clustering<Model> runInTime(Database<O> database) throws IllegalStateException {
    HashMap<String, Collection<Integer>> labelMap = multiple ? multipleAssignment(database) : singleAssignment(database);

    Clustering<Model> result = new Clustering<Model>();
    for(Entry<String, Collection<Integer>> entry : labelMap.entrySet()) {
      Collection<Integer> ids = labelMap.get(entry.getKey());
      DatabaseObjectGroup group = new DatabaseObjectGroupCollection<Collection<Integer>>(ids);
      Cluster<Model> c = new Cluster<Model>(entry.getKey(), group, ClusterModel.CLUSTER);
      result.addCluster(c);
    }
    return result;
  }

  /**
   * Assigns the objects of the database to single clusters according to their
   * labels.
   * 
   * @param database the database storing the objects
   * @return a mapping of labels to ids
   */
  private HashMap<String, Collection<Integer>> singleAssignment(Database<O> database) {
    HashMap<String, Collection<Integer>> labelMap = new HashMap<String, Collection<Integer>>();

    for(Integer id : database) {
      String label = DatabaseUtil.getClassOrObjectLabel(database, id);
      assign(labelMap, label, id);
    }
    return labelMap;
  }

  /**
   * Assigns the objects of the database to multiple clusters according to their
   * labels.
   * 
   * @param database the database storing the objects
   * @return a mapping of labels to ids
   */
  private HashMap<String, Collection<Integer>> multipleAssignment(Database<O> database) {
    HashMap<String, Collection<Integer>> labelMap = new HashMap<String, Collection<Integer>>();

    for(Integer id : database) {
      String[] labels = DatabaseUtil.getClassOrObjectLabel(database, id).split(" ");
      for(String label : labels) {
        assign(labelMap, label, id);
      }
    }
    return labelMap;
  }

  /**
   * Assigns the specified id to the labelMap according to its label
   * 
   * @param labelMap the mapping of label to ids
   * @param label the label of the object to be assigned
   * @param id the id of the object to be assigned
   */
  private void assign(HashMap<String, Collection<Integer>> labelMap, String label, Integer id) {
    if(labelMap.containsKey(label)) {
      labelMap.get(label).add(id);
    }
    else {
      Collection<Integer> n = new java.util.Vector<Integer>();
      n.add(id);
      labelMap.put(label, n);
    }
  }

  /**
   * Sets the multiple flag to indicate that a multiple cluster assignment is
   * possible.
   * 
   * @param multiple the flag to be set
   */
  public void setMultiple(boolean multiple) {
    this.MULTIPLE_FLAG.setValue(multiple);
    this.multiple = multiple;
  }
}
