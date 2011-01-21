package de.lmu.ifi.dbs.elki.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;

/**
 * This class derives a database partitioning based on a clustering result.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.database.Database oneway - - partitions
 */
public class PartitionsFromClustering {
  private String label_prefix = "C";

  /**
   * Use an existing clustering to partition a database.
   * 
   * @param <O> Database object type
   * @param <R> Clustering class
   * @param <L> Label class
   * @param olddb Original database
   * @param clustering Clustering to use for partitioning
   * @param classLabel ClassLabel class to use.
   * @return map from classlabels to database partitions.
   * @throws UnableToComplyException thrown on invalid data
   */
  public <O extends DatabaseObject, R extends Clustering<Model>, L extends ClassLabel> Map<L,Database<O>> makeDatabasesFromClustering(Database<O> olddb, R clustering, Class<L> classLabel) throws UnableToComplyException {
    // we need at least one cluster
    if (clustering.getToplevelClusters().size() <= 0) {
      throw new UnableToComplyException(ExceptionMessages.CLUSTERING_EMPTY);
    }

    // prepare a map for the partitioning call.
    Map<Integer, DBIDs> partitions = new HashMap<Integer, DBIDs>();
    int clusterID = 1;
    for(Cluster<Model> c : clustering.getAllClusters()) {
      DBIDs col = c.getIDs();
      partitions.put(clusterID, col);
      clusterID++;
    }
    Map<Integer,Database<O>> newdb = olddb.partition(partitions);
    
    Map<L, Database<O>> map = new HashMap<L, Database<O>>();

    // build result map
    for(Entry<Integer, Database<O>> e : newdb.entrySet()) {
      L label = ClassGenericsUtil.instantiate(classLabel, classLabel.getName());
      label.init(label_prefix + e.getKey().toString());
      map.put(label, e.getValue());
    }

    return map;
  }

  /**
   * Use an existing clustering to partition a database.
   * 
   * @param <O> Database object type
   * @param <R> Clustering class
   * @param <M> Model type
   * @param olddb Original database
   * @param clustering Clustering to use for partitioning
   * @return map from clusters to database partitions.
   * @throws UnableToComplyException thrown on invalid data
   */
  public <O extends DatabaseObject, R extends Clustering<M>, M extends Model> Map<Cluster<M>,Database<O>> makeDatabasesFromClustering(Database<O> olddb, R clustering) throws UnableToComplyException {
    // we need at least one cluster
    if (clustering.getToplevelClusters().size() <= 0) {
      throw new UnableToComplyException(ExceptionMessages.CLUSTERING_EMPTY);
    }

    // prepare a map for the partitioning call.
    Map<Integer, DBIDs> partitions = new HashMap<Integer, DBIDs>();
    Map<Integer, Cluster<M>> clusters = new HashMap<Integer, Cluster<M>>();
    int clusterID = 1;
    for(Cluster<M> c : clustering.getAllClusters()) {
      DBIDs col = c.getIDs();
      partitions.put(clusterID, col);
      clusters.put(clusterID, c);
      clusterID++;
    }
    Map<Integer,Database<O>> newdb = olddb.partition(partitions);
    
    Map<Cluster<M>, Database<O>> map = new HashMap<Cluster<M>, Database<O>>();

    // build result map
    for(Entry<Integer, Database<O>> e : newdb.entrySet()) {
      map.put(clusters.get(e.getKey()), e.getValue());
    }

    return map;
  }
}