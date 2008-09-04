package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.HierarchicalClusters;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.HierarchicalPlainCluster;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.Description;

/**
 * Pseudo clustering using labels.
 * 
 * This "algorithm" puts elements into the same cluster when they agree in their labels.
 * I.e. it just uses a predefined clustering, and is mostly useful for testing and evaluation
 * (e.g. comparing the result of a real algorithm to a reference result / golden standard).
 * 
 * TODO: allow hierarchical clusterings by splitting label into words?
 * TODO: handling of data sets with no labels?
 * TODO: Noise handling (e.g. allow the user to specify a noise label pattern?)
 * 
 * @author Erich Schubert
 *
 * @param <O>
 */
public class ByLabelHierarchicalClustering<O extends DatabaseObject> extends AbstractAlgorithm<O> {
  /**
   * Holds the result of the algorithm.
   */
  private HierarchicalClusters<HierarchicalPlainCluster<O>,O> result;

  /**
   * Return clustering result
   */
  public HierarchicalClusters<HierarchicalPlainCluster<O>,O> getResult() {
    return result;
  }

  /**
   * Obtain a description of the algorithm
   */
  public Description getDescription() {
    return new Description("ByLabelClustering", "Clustering by label",
        "Cluster points by a (pre-assigned!) label. For comparing results with a reference clustering.", "");
  }

  /**
   * Run the actual clustering algorithm.
   * 
   * @param database The database to process
   */
  @Override
  protected void runInTime(Database<O> database) throws IllegalStateException {
    HashMap<String, Set<Integer>> labelmap = new HashMap<String, Set<Integer>>(); 
    
    for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      Integer id = iter.next();
      String label = null;
      
      // try class label first
      ClassLabel classlabel = database.getAssociation(AssociationID.CLASS, id);
      if (classlabel != null) label = classlabel.toString();
      
      // fall back to other labels
      if (label == null)
        label = database.getAssociation(AssociationID.LABEL, id);
      
      if (labelmap.containsKey(label))
        labelmap.get(label).add(id);
      else {
        Set<Integer> n = new java.util.HashSet<Integer>();
        n.add(id);
        labelmap.put(label,n);
      }
    }

    ArrayList<HierarchicalPlainCluster<O>> clusters = new ArrayList<HierarchicalPlainCluster<O>>(labelmap.size());
    int i = 0;
    for (Entry<String, Set<Integer>> entry : labelmap.entrySet()) {
      clusters.add(new HierarchicalPlainCluster<O>(entry.getValue(), new ArrayList<HierarchicalPlainCluster<O>>(), new ArrayList<HierarchicalPlainCluster<O>>(), entry.getKey(), 0, 0));
      i++;
    }
    for (HierarchicalPlainCluster<O> cur : clusters) {
      for (HierarchicalPlainCluster<O> oth : clusters)
        if (oth != cur) {
          if (oth.getLabel().startsWith(cur.getLabel())) {
            oth.addParent(cur);
            cur.addChild(oth);
            //System.err.println(oth.getLabel() + " is a child of " + cur.getLabel());
          }
        }
      // TODO: levels
    }
    ArrayList<HierarchicalPlainCluster<O>> rootclusters = new ArrayList<HierarchicalPlainCluster<O>>();
    for (HierarchicalPlainCluster<O> cur : clusters)
      if (cur.getParents().size() == 0)
        rootclusters.add(cur);
    assert(rootclusters.size() > 0);
    result = new HierarchicalClusters<HierarchicalPlainCluster<O>,O>(rootclusters, database);
  }
}
