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

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

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
 * flag {@link Parameterizer#MULTIPLE_ID} needs to be set.
 * 
 * TODO: handling of data sets with no labels?
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @assoc - - - de.lmu.ifi.dbs.elki.data.ClassLabel
 */
@Title("Clustering by label")
@Description("Cluster points by a (pre-assigned!) label. For comparing results with a reference clustering.")
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering")
@Priority(Priority.SUPPLEMENTARY)
public class ByLabelClustering extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ByLabelClustering.class);

  /**
   * Allow multiple cluster assignment.
   */
  private boolean multiple;

  /**
   * Pattern to recognize noise clusters by.
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

  @Override
  public Clustering<Model> run(Database database) {
    // Prefer a true class label
    try {
      Relation<ClassLabel> relation = database.getRelation(TypeUtil.CLASSLABEL);
      return run(relation);
    }
    catch(NoSupportedDataTypeException e) {
      // Otherwise, try any labellike.
      return run(database.getRelation(getInputTypeRestriction()[0]));
    }
  }

  /**
   * Run the actual clustering algorithm.
   * 
   * @param relation The data input we use
   */
  public Clustering<Model> run(Relation<?> relation) {
    HashMap<String, DBIDs> labelMap = multiple ? multipleAssignment(relation) : singleAssignment(relation);

    ModifiableDBIDs noiseids = DBIDUtil.newArray();
    Clustering<Model> result = new Clustering<>("By Label Clustering", "bylabel-clustering");
    for(Entry<String, DBIDs> entry : labelMap.entrySet()) {
      DBIDs ids = entry.getValue();
      if(ids.size() <= 1) {
        noiseids.addDBIDs(ids);
        continue;
      }
      // Build a cluster
      Cluster<Model> c = new Cluster<Model>(entry.getKey(), ids, ClusterModel.CLUSTER);
      if(noisepattern != null && noisepattern.matcher(entry.getKey()).find()) {
        c.setNoise(true);
      }
      result.addToplevelCluster(c);
    }
    // Collected noise IDs.
    if(noiseids.size() > 0) {
      Cluster<Model> c = new Cluster<Model>("Noise", noiseids, ClusterModel.CLUSTER);
      c.setNoise(true);
      result.addToplevelCluster(c);
    }
    return result;
  }

  /**
   * Assigns the objects of the database to single clusters according to their
   * labels.
   * 
   * @param data the database storing the objects
   * @return a mapping of labels to ids
   */
  private HashMap<String, DBIDs> singleAssignment(Relation<?> data) {
    HashMap<String, DBIDs> labelMap = new HashMap<>();

    for(DBIDIter iditer = data.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final Object val = data.get(iditer);
      String label = (val != null) ? val.toString() : null;
      assign(labelMap, label, iditer);
    }
    return labelMap;
  }

  /**
   * Assigns the objects of the database to multiple clusters according to their
   * labels.
   * 
   * @param data the database storing the objects
   * @return a mapping of labels to ids
   */
  private HashMap<String, DBIDs> multipleAssignment(Relation<?> data) {
    HashMap<String, DBIDs> labelMap = new HashMap<>();

    for(DBIDIter iditer = data.iterDBIDs(); iditer.valid(); iditer.advance()) {
      String[] labels = data.get(iditer).toString().split(" ");
      for(String label : labels) {
        assign(labelMap, label, iditer);
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
  private void assign(HashMap<String, DBIDs> labelMap, String label, DBIDRef id) {
    if(labelMap.containsKey(label)) {
      DBIDs exist = labelMap.get(label);
      if(exist instanceof DBID) {
        ModifiableDBIDs n = DBIDUtil.newHashSet();
        n.add((DBID) exist);
        n.add(id);
        labelMap.put(label, n);
      }
      else {
        assert (exist instanceof HashSetModifiableDBIDs);
        assert (exist.size() > 1);
        ((ModifiableDBIDs) exist).add(id);
      }
    }
    else {
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

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Flag to indicate that multiple cluster assignment is possible. If an
     * assignment to multiple clusters is desired, the labels indicating the
     * clusters need to be separated by blanks.
     */
    public static final OptionID MULTIPLE_ID = new OptionID("bylabelclustering.multiple", "Flag to indicate that only subspaces with large coverage " + "(i.e. the fraction of the database that is covered by the dense units) " + "are selected, the rest will be pruned.");

    /**
     * Parameter to specify the pattern to recognize noise clusters by.
     */
    public static final OptionID NOISE_ID = new OptionID("bylabelclustering.noise", "Pattern to recognize noise classes by their label.");

    /**
     * Allow multiple cluster assignment.
     */
    protected boolean multiple;

    /**
     * Pattern to recognize noise clusters by.
     */
    protected Pattern noisepat;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag multipleF = new Flag(MULTIPLE_ID);
      if(config.grab(multipleF)) {
        multiple = multipleF.getValue();
      }

      PatternParameter noisepatP = new PatternParameter(NOISE_ID) //
          .setOptional(true);
      if(config.grab(noisepatP)) {
        noisepat = noisepatP.getValue();
      }
    }

    @Override
    protected ByLabelClustering makeInstance() {
      return new ByLabelClustering(multiple, noisepat);
    }
  }
}
