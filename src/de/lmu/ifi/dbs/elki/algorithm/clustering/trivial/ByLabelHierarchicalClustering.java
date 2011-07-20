package de.lmu.ifi.dbs.elki.algorithm.clustering.trivial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Pseudo clustering using labels.
 * 
 * This "algorithm" puts elements into the same cluster when they agree in their
 * labels. I.e. it just uses a predefined clustering, and is mostly useful for
 * testing and evaluation (e.g. comparing the result of a real algorithm to a
 * reference result / golden standard).
 * 
 * This variant derives a hierarchical result by doing a prefix comparison on
 * labels.
 * 
 * TODO: Noise handling (e.g. allow the user to specify a noise label pattern?)
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.data.ClassLabel
 */
@Title("Hierarchical clustering by label")
@Description("Cluster points by a (pre-assigned!) label. For comparing results with a reference clustering.")
public class ByLabelHierarchicalClustering extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ByLabelHierarchicalClustering.class);
  
  /**
   * Constructor without parameters
   */
  public ByLabelHierarchicalClustering() {
    super();
  }

  /**
   * Run the actual clustering algorithm.
   * 
   * @param database The database to process
   * @param relation The data input to use
   */
  public Clustering<Model> run(Database database, Relation<?> relation) throws IllegalStateException {
    HashMap<String, ModifiableDBIDs> labelmap = new HashMap<String, ModifiableDBIDs>();
    
    for(DBID id : relation.iterDBIDs()) {
      String label = relation.get(id).toString();

      if(labelmap.containsKey(label)) {
        labelmap.get(label).add(id);
      }
      else {
        ModifiableDBIDs n = DBIDUtil.newHashSet();
        n.add(id);
        labelmap.put(label, n);
      }
    }

    ArrayList<Cluster<Model>> clusters = new ArrayList<Cluster<Model>>(labelmap.size());
    for(Entry<String, ModifiableDBIDs> entry : labelmap.entrySet()) {
      Cluster<Model> clus = new Cluster<Model>(entry.getKey(), entry.getValue(), ClusterModel.CLUSTER, new ArrayList<Cluster<Model>>(), new ArrayList<Cluster<Model>>());
      clusters.add(clus);
    }

    for(Cluster<Model> cur : clusters) {
      for(Cluster<Model> oth : clusters) {
        if(oth != cur) {
          if(oth.getName().startsWith(cur.getName())) {
            oth.getParents().add(cur);
            cur.getChildren().add(oth);
            // System.err.println(oth.getLabel() + " is a child of " +
            // cur.getLabel());
          }
        }
      }
    }
    ArrayList<Cluster<Model>> rootclusters = new ArrayList<Cluster<Model>>();
    for(Cluster<Model> cur : clusters) {
      if(cur.getParents().size() == 0) {
        rootclusters.add(cur);
      }
    }
    assert (rootclusters.size() > 0);

    return new Clustering<Model>("By Label Hierarchical Clustering", "bylabel-clustering", rootclusters);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.GUESSED_LABEL);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}