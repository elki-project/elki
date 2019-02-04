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
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorInterface;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Pseudo clustering using annotated models.
 * 
 * This "algorithm" puts elements into the same cluster when they agree in their
 * model. I.e. it just uses a predefined clustering, and is mostly useful for
 * testing and evaluation (e.g. comparing the result of a real algorithm to the
 * reference result / golden standard used by the generator).
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @assoc - - - Model
 */
@Title("Clustering by model")
@Description("Cluster points by a (pre-assigned!) model. For comparing results with a reference clustering.")
@Priority(Priority.SUPPLEMENTARY - 5)
public class ByModelClustering extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ByModelClustering.class);

  /**
   * Pattern to recognize noise clusters with.
   */
  private Pattern noisepattern = null;

  /**
   * Constructor.
   * 
   * @param noisepattern Noise pattern
   */
  public ByModelClustering(Pattern noisepattern) {
    super();
    this.noisepattern = noisepattern;
  }

  /**
   * Constructor without parameters
   */
  public ByModelClustering() {
    this(null);
  }

  /**
   * Run the actual clustering algorithm.
   * 
   * @param relation The data input we use
   */
  public Clustering<Model> run(Relation<Model> relation) {
    // Build model mapping
    HashMap<Model, ModifiableDBIDs> modelMap = new HashMap<>();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      Model model = relation.get(iditer);
      ModifiableDBIDs modelids = modelMap.get(model);
      if(modelids == null) {
        modelids = DBIDUtil.newHashSet();
        modelMap.put(model, modelids);
      }
      modelids.add(iditer);
    }

    Clustering<Model> result = new Clustering<>("By Model Clustering", "bymodel-clustering");
    for(Entry<Model, ModifiableDBIDs> entry : modelMap.entrySet()) {
      final Model model = entry.getKey();
      final ModifiableDBIDs ids = entry.getValue();
      final String name = (model instanceof GeneratorInterface) ? ((GeneratorInterface) model).getName() : model.toString();
      Cluster<Model> c = new Cluster<>(name, ids, model);
      if(noisepattern != null && noisepattern.matcher(name).find()) {
        c.setNoise(true);
      }
      result.addToplevelCluster(c);
    }
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(Model.TYPE);
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
     * Parameter to specify the pattern to recognize noise clusters with.
     */
    public static final OptionID NOISE_ID = new OptionID("bymodel.noise", "Pattern to recognize noise models by their label.");

    /**
     * Pattern to recognize noise clusters with
     */
    protected Pattern noisepat;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter noisepatP = new PatternParameter(NOISE_ID) //
          .setOptional(true);
      if(config.grab(noisepatP)) {
        noisepat = noisepatP.getValue();
      }
    }

    @Override
    protected ByModelClustering makeInstance() {
      return new ByModelClustering(noisepat);
    }
  }
}
