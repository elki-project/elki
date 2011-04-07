package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;

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
 * flag {@link #MULTIPLE_ID} needs to be set.
 * 
 * TODO: handling of data sets with no labels?
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.data.ClassLabel
 * 
 * @param <O> Object type
 */
@Title("Clustering by label")
@Description("Cluster points by a (pre-assigned!) label. For comparing results with a reference clustering.")
public class ByLabelClustering<O extends DatabaseObject> extends AbstractAlgorithm<O, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>, O> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ByLabelClustering.class);

  /**
   * Flag to indicate that multiple cluster assignment is possible. If an
   * assignment to multiple clusters is desired, the labels indicating the
   * clusters need to be separated by blanks.
   */
  public static final OptionID MULTIPLE_ID = OptionID.getOrCreateOptionID("bylabelclustering.multiple", "Flag to indicate that only subspaces with large coverage " + "(i.e. the fraction of the database that is covered by the dense units) " + "are selected, the rest will be pruned.");

  /**
   * Flag to indicate that multiple cluster assignment is possible. If an
   * assignment to multiple clusters is desired, the labels indicating the
   * clusters need to be separated by blanks.
   */
  public static final OptionID NOISE_ID = OptionID.getOrCreateOptionID("bylabelclustering.noise", "Pattern to recognize noise classes by their label.");

  /**
   * Holds the value of {@link #MULTIPLE_ID}.
   */
  private boolean multiple;

  /**
   * Holds the value of {@link #NOISE_ID}.
   */
  private Pattern noisepattern = null;

  /**
   * Constructor.
   * 
   * @param multiple Allow multiple cluster assignments
   * @param noisepattern Noise pattern
   */
  public ByLabelClustering(boolean multiple, Pattern noisepattern) {
    super();
    this.multiple = multiple;
    this.noisepattern = noisepattern;
  }

  /**
   * Constructor without parameters
   */
  public ByLabelClustering() {
    this(false, null);
  }

  /**
   * Run the actual clustering algorithm.
   * 
   * @param database The database to process
   */
  @Override
  protected Clustering<Model> runInTime(Database<O> database) throws IllegalStateException {
    HashMap<String, ModifiableDBIDs> labelMap = multiple ? multipleAssignment(database) : singleAssignment(database);

    Clustering<Model> result = new Clustering<Model>("By Label Clustering", "bylabel-clustering");
    for(Entry<String, ModifiableDBIDs> entry : labelMap.entrySet()) {
      ModifiableDBIDs ids = labelMap.get(entry.getKey());
      Cluster<Model> c = new Cluster<Model>(entry.getKey(), ids, ClusterModel.CLUSTER);
      if(noisepattern != null && noisepattern.matcher(entry.getKey()).find()) {
        c.setNoise(true);
      }
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
  private HashMap<String, ModifiableDBIDs> singleAssignment(Database<O> database) {
    HashMap<String, ModifiableDBIDs> labelMap = new HashMap<String, ModifiableDBIDs>();

    for(DBID id : database) {
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
  private HashMap<String, ModifiableDBIDs> multipleAssignment(Database<O> database) {
    HashMap<String, ModifiableDBIDs> labelMap = new HashMap<String, ModifiableDBIDs>();

    for(DBID id : database) {
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
  private void assign(HashMap<String, ModifiableDBIDs> labelMap, String label, DBID id) {
    if(labelMap.containsKey(label)) {
      labelMap.get(label).add(id);
    }
    else {
      ModifiableDBIDs n = DBIDUtil.newHashSet();
      n.add(id);
      labelMap.put(label, n);
    }
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends DatabaseObject> extends AbstractParameterizer {
    protected boolean multiple;

    protected Pattern noisepat;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag multipleF = new Flag(MULTIPLE_ID);
      if(config.grab(multipleF)) {
        multiple = multipleF.getValue();
      }

      PatternParameter noisepatP = new PatternParameter(NOISE_ID, true);
      if(config.grab(noisepatP)) {
        noisepat = noisepatP.getValue();
      }
    }

    @Override
    protected ByLabelClustering<O> makeInstance() {
      return new ByLabelClustering<O>(multiple, noisepat);
    }
  }
}