/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.clustering.trivial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
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
 * @since 0.2
 * 
 * @assoc - - - de.lmu.ifi.dbs.elki.data.ClassLabel
 */
@Title("Hierarchical clustering by label")
@Description("Cluster points by a (pre-assigned!) label. For comparing results with a reference clustering.")
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering")
@Priority(Priority.SUPPLEMENTARY - 5)
public class ByLabelHierarchicalClustering extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ByLabelHierarchicalClustering.class);

  /**
   * Constructor without parameters
   */
  public ByLabelHierarchicalClustering() {
    super();
  }

  @Override
  public Clustering<Model> run(Database database) {
    // Prefer a true class label
    try {
      Relation<ClassLabel> relation = database.getRelation(TypeUtil.CLASSLABEL);
      return run(relation);
    } catch (NoSupportedDataTypeException e) {
      // Otherwise, try any labellike.
      return run(database.getRelation(getInputTypeRestriction()[0]));
    }
  }

  /**
   * Run the actual clustering algorithm.
   * 
   * @param relation The data input to use
   */
  public Clustering<Model> run(Relation<?> relation) {
    HashMap<String, DBIDs> labelmap = new HashMap<>();
    ModifiableDBIDs noiseids = DBIDUtil.newArray();
    Clustering<Model> clustering = new Clustering<>("By Label Hierarchical Clustering", "bylabel-clustering");

    for (DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final Object val = relation.get(iditer);
      if (val == null) {
        noiseids.add(iditer);
        continue;
      }
      String label = val.toString();

      assign(labelmap, label, iditer);
    }

    ArrayList<Cluster<Model>> clusters = new ArrayList<>(labelmap.size());
    for (Entry<String, DBIDs> entry : labelmap.entrySet()) {
      DBIDs ids = entry.getValue();
      if (ids instanceof DBID) {
        noiseids.add((DBID) ids);
        continue;
      }
      Cluster<Model> clus = new Cluster<Model>(entry.getKey(), ids, ClusterModel.CLUSTER);
      clusters.add(clus);
    }

    for (Cluster<Model> cur : clusters) {
      boolean isrootcluster = true;
      for (Cluster<Model> oth : clusters) {
        if (oth != cur && oth.getName().startsWith(cur.getName())) {
          clustering.addChildCluster(oth, cur);
          if (LOG.isDebuggingFiner()) {
            LOG.debugFiner(oth.getName() + " is a child of " + cur.getName());
          }
          isrootcluster = false;
        }
      }
      if (isrootcluster) {
        clustering.addToplevelCluster(cur);
      }
    }
    // Collected noise IDs.
    if (noiseids.size() > 0) {
      Cluster<Model> c = new Cluster<Model>("Noise", noiseids, ClusterModel.CLUSTER);
      c.setNoise(true);
      clustering.addToplevelCluster(c);
    }
    return clustering;
  }

  /**
   * Assigns the specified id to the labelMap according to its label
   * 
   * @param labelMap the mapping of label to ids
   * @param label the label of the object to be assigned
   * @param id the id of the object to be assigned
   */
  private void assign(HashMap<String, DBIDs> labelMap, String label, DBIDRef id) {
    if (labelMap.containsKey(label)) {
      DBIDs exist = labelMap.get(label);
      if (exist instanceof DBID) {
        ModifiableDBIDs n = DBIDUtil.newHashSet();
        n.add((DBID) exist);
        n.add(id);
        labelMap.put(label, n);
      } else {
        assert (exist instanceof HashSetModifiableDBIDs);
        assert (exist.size() > 1);
        ((ModifiableDBIDs) exist).add(id);
      }
    } else {
      labelMap.put(label, DBIDUtil.deref(id));
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.GUESSED_LABEL);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
