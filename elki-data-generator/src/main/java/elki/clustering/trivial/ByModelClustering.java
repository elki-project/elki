/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.trivial;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import elki.clustering.ClusteringAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.data.synthetic.bymodel.GeneratorInterface;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.result.Metadata;
import elki.utilities.Priority;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Pseudo clustering using annotated models.
 * <p>
 * This "algorithm" puts elements into the same cluster when they agree in their
 * model. I.e. it just uses a predefined clustering, and is mostly useful for
 * testing and evaluation (e.g., comparing the result of a real algorithm to the
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
public class ByModelClustering implements ClusteringAlgorithm<Clustering<Model>> {
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

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(Model.TYPE);
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
      modelMap.computeIfAbsent(relation.get(iditer), x -> DBIDUtil.newHashSet()).add(iditer);
    }

    Clustering<Model> result = new ReferenceClustering<>();
    Metadata.of(result).setLongName("By Model Clustering");
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

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter to specify the pattern to recognize noise clusters with.
     */
    public static final OptionID NOISE_ID = new OptionID("bymodel.noise", "Pattern to recognize noise models by their label.");

    /**
     * Pattern to recognize noise clusters with
     */
    protected Pattern noisepat;

    @Override
    public void configure(Parameterization config) {
      new PatternParameter(NOISE_ID) //
          .setOptional(true) //
          .grab(config, x -> noisepat = x);
    }

    @Override
    public ByModelClustering make() {
      return new ByModelClustering(noisepat);
    }
  }
}
