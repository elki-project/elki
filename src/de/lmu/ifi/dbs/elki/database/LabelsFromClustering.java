package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;

/**
 * This class generates a labeled database based on an existing clustering result.
 * 
 * @author Erich Schubert
 *
 */
public class LabelsFromClustering {
  private String label_prefix = "C";

  /**
   * Retrieve a cloned database that
   * - does not contain noise points
   * - has labels assigned based on the given clustering
   * 
   * Useful for e.g. training a classifier based on a clustering.
   * 
   * @param <O> Database object type
   * @param <R> Clustering Result
   * @param <M> Cluster Model class
   * @param <L> Label class
   * @param olddb database the original objects come from
   * @param clustering clustering to work on
   * @param classLabel label class to use.
   * @return new database
   * @throws UnableToComplyException thrown on invalid data.
   */
  public <O extends DatabaseObject, R extends Clustering<M>, M extends Model, L extends ClassLabel> Database<O> makeDatabaseFromClustering(Database<O> olddb, R clustering, Class<L> classLabel) throws UnableToComplyException {
    // we need at least one cluster
    if (clustering.getToplevelClusters().size() <= 0) {
      throw new UnableToComplyException(ExceptionMessages.CLUSTERING_EMPTY);
    }
    
    // we don't want to keep noise, and we need a cloned database anyway.
    // the easiest way to do this is using the partition() method.
    Map<Integer, List<Integer>> partitions = new HashMap<Integer, List<Integer>>();
    ArrayList<Integer> nonnoise = new ArrayList<Integer>(olddb.size());
    for(Cluster<M> c : clustering.getAllClusters()) {
      nonnoise.addAll(c.getIDs());
    }
    partitions.put(1, nonnoise);
    Database<O> newdb = olddb.partition(partitions).get(1);

    // assign cluster labels
    int clusterID = 1;
    for(Cluster<M> c : clustering.getAllClusters()) {
      L label = ClassGenericsUtil.instantiate(classLabel, classLabel.getName());
      label.init(label_prefix + Integer.toString(clusterID));
      for(Integer id : c) {
        newdb.setClassLabel(id, label);
      }
      clusterID++;
    }

    return newdb;
  }
}
