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
package elki.outlier.clustering;

import elki.Algorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.data.Clustering;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.evaluation.clustering.internal.NoiseHandling;
import elki.evaluation.clustering.internal.Silhouette;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.Metadata;
import elki.result.outlier.InvertedOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Outlier detection by using the Silhouette Coefficients.
 * <p>
 * Silhouette values are computed as by Rousseeuw and then used as outlier
 * scores. To cite this outlier detection approach,
 * please cite the ELKI version you used (use the
 * <a href="https://elki-project.github.io/publications">ELKI publication
 * list</a>
 * for citation information and BibTeX templates).
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - ClusteringAlgorithm
 *
 * @param <O> Object type
 */
@Reference(authors = "P. J. Rousseeuw", //
    title = "Silhouettes: A graphical aid to the interpretation and validation of cluster analysis", //
    booktitle = "Journal of Computational and Applied Mathematics, Volume 20", //
    url = "https://doi.org/10.1016/0377-0427(87)90125-7", //
    bibkey = "doi:10.1016/0377-04278790125-7")
public class SilhouetteOutlierDetection<O> implements OutlierAlgorithm {
  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Clustering algorithm to use
   */
  protected ClusteringAlgorithm<?> clusterer;

  /**
   * Option for noise handling.
   */
  protected NoiseHandling noiseOption = NoiseHandling.TREAT_NOISE_AS_SINGLETONS;

  /**
   * Constructor.
   * 
   * @param distance Distance function
   * @param clusterer Clustering algorithm
   * @param noiseOption Noise handling option.
   */
  public SilhouetteOutlierDetection(Distance<? super O> distance, ClusteringAlgorithm<?> clusterer, NoiseHandling noiseOption) {
    super();
    this.distance = distance;
    this.clusterer = clusterer;
    this.noiseOption = noiseOption;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    final TypeInformation dt = distance.getInputTypeRestriction();
    TypeInformation[] t = clusterer.getInputTypeRestriction();
    for(TypeInformation i : t) {
      if(dt.isAssignableFromType(i)) {
        return t;
      }
    }
    // Prepend distance type:
    TypeInformation[] t2 = new TypeInformation[t.length + 1];
    t2[0] = dt;
    System.arraycopy(t, 0, t2, 1, t.length);
    return t2;
  }

  /**
   * Run the Silhouette score as outlier method.
   *
   * @param database Database
   * @return Outlier scores
   */
  @Override
  public OutlierResult autorun(Database database) {
    Clustering<?> c = clusterer.autorun(database);
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    // Check if the clustering already provides a Silhouette:
    It<DoubleRelation> it = Metadata.hierarchyOf(c).iterDescendants() //
        .filter(DoubleRelation.class) //
        .filter(x -> Silhouette.SILHOUETTE_NAME.equals(x.getLongName()));
    if(!it.valid()) { // Otherwise, run Silhouette:
      DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).distanceQuery();
      new Silhouette<O>(distance, noiseOption, false).evaluateClustering(relation, dq, c);
      it = Metadata.hierarchyOf(c).iterDescendants() //
          .filter(DoubleRelation.class) //
          .filter(x -> Silhouette.SILHOUETTE_NAME.equals(x.getLongName()));
    }
    if(!it.valid()) {
      throw new NullPointerException("Silhouette did not produce Silhouette scores.");
    }
    DoubleRelation scoreResult = it.get();
    DoubleMinMax mm = new DoubleMinMax();
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      mm.put(scoreResult.doubleValue(iter));
    }
    return new OutlierResult(new InvertedOutlierScoreMeta(mm.getMin(), mm.getMax(), -1., 1., .5), scoreResult);
  }

  /**
   * Parameterizer.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter for choosing the clustering algorithm
     */
    public static final OptionID CLUSTERING_ID = new OptionID("silhouette.clustering", //
        "Clustering algorithm to use for the silhouette coefficients.");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * Clustering algorithm to use
     */
    protected ClusteringAlgorithm<?> clusterer;

    /**
     * Noise handling
     */
    protected NoiseHandling noiseOption = NoiseHandling.TREAT_NOISE_AS_SINGLETONS;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new ObjectParameter<ClusteringAlgorithm<?>>(CLUSTERING_ID, ClusteringAlgorithm.class) //
          .grab(config, x -> clusterer = x);
      new EnumParameter<NoiseHandling>(Silhouette.Par.NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS) //
          .grab(config, x -> noiseOption = x);
    }

    @Override
    public SilhouetteOutlierDetection<O> make() {
      return new SilhouetteOutlierDetection<>(distance, clusterer, noiseOption);
    }
  }
}
