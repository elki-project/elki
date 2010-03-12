package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;

/**
 * Pseudo clustering using labels.
 * 
 * This "algorithm" puts elements into the same cluster when they agree in their
 * labels. I.e. it just uses a predefined clustering, and is mostly useful for
 * testing and evaluation (e.g. comparing the result of a real algorithm to a
 * reference result / golden standard).
 * 
 * TODO: handling of data sets with no labels? TODO: Noise handling (e.g. allow
 * the user to specify a noise label pattern?)
 * 
 * @author Erich Schubert
 * 
 * @param <O>
 */
@Title("Clustering by label")
@Description("Cluster points by a (pre-assigned!) label. For comparing results with a reference clustering.")
public class ByLabelClustering<O extends DatabaseObject> extends AbstractAlgorithm<O, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>, O> {
  /**
   * Simpler constructor, that doesn't support time() and verbose()
   */
  public ByLabelClustering() {
    super(new EmptyParameterization());
  }

  /**
   * Run the actual clustering algorithm.
   * 
   * @param database The database to process
   */
  @Override
  protected Clustering<Model> runInTime(Database<O> database) throws IllegalStateException {
    HashMap<String, Collection<Integer>> labelmap = new HashMap<String, Collection<Integer>>();

    for(Integer id : database) {
      String label = null;

      // try class label first
      ClassLabel classlabel = database.getAssociation(AssociationID.CLASS, id);
      if(classlabel != null) {
        label = classlabel.toString();
      }

      // fall back to other labels
      if(label == null) {
        label = database.getAssociation(AssociationID.LABEL, id);
      }

      if(labelmap.containsKey(label)) {
        labelmap.get(label).add(id);
      }
      else {
        Collection<Integer> n = new java.util.Vector<Integer>();
        n.add(id);
        labelmap.put(label, n);
      }
    }

    Clustering<Model> result = new Clustering<Model>();
    for(Entry<String, Collection<Integer>> entry : labelmap.entrySet()) {
      Collection<Integer> ids = labelmap.get(entry.getKey());
      DatabaseObjectGroup group = new DatabaseObjectGroupCollection<Collection<Integer>>(ids);
      Cluster<Model> c = new Cluster<Model>(entry.getKey(), group, ClusterModel.CLUSTER);
      result.addCluster(c);
    }
    return result;
  }
}
