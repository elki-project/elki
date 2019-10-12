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
package elki.clustering.kmeans.initialization;

import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.data.NumberVector;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.utilities.documentation.Reference;
import elki.utilities.random.RandomFactory;

/**
 * Initialize K-means by randomly choosing k existing elements as initial
 * cluster centers.
 * <p>
 * Reference:
 * <p>
 * D. J. McRae<br>
 * MIKCA: A FORTRAN IV Iterative K-Means Cluster Analysis Program<br>
 * Behavioral Science 16(4)
 * <p>
 * E. W. Forgy<br>
 * Cluster analysis of multivariate data: efficiency versus interpretability of
 * classifications<br>
 * Abstract published in Biometrics 21(3)
 * <p>
 * M. R. Anderberg<br>
 * Hierarchical Clustering Methods<br>
 * Cluster Analysis for Applications
 * <p>
 * This initialization is often attributed to Forgy (but this is also debated),
 * but we were unable to verify neither McRae (not available online?) nor Forgy
 * so far (apparently, only an abstract is available in print, so we mostly can
 * rely on indirect references, such as Anderberg).
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <O> Vector type
 */
@Reference(authors = "D. J. McRae", //
    title = "MIKCA: A FORTRAN IV Iterative K-Means Cluster Analysis Program", //
    booktitle = "Behavioral Science 16(4)", //
    bibkey = "journals/misc/McRae71")
@Reference(authors = "E. W. Forgy", //
    title = "Cluster analysis of multivariate data: efficiency versus interpretability of classifications", //
    booktitle = "Biometrics 21(3)", //
    bibkey = "journals/biometrics/Forgy65")
@Reference(authors = "M. R. Anderberg", //
    title = "Nonhierarchical Clustering Methods", //
    booktitle = "Cluster Analysis for Applications", //
    bibkey = "books/academic/Anderberg73/Ch7")
public class RandomlyChosen<O> extends AbstractKMeansInitialization implements KMedoidsInitialization<O> {
  /**
   * Constructor.
   *
   * @param rnd Random generator.
   */
  public RandomlyChosen(RandomFactory rnd) {
    super(rnd);
  }

  @Override
  public double[][] chooseInitialMeans(Relation<? extends NumberVector> relation, int k, NumberVectorDistance<?> distance) {
    if(relation.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + relation.size() + " < k objects.");
    }
    DBIDs ids = DBIDUtil.randomSample(relation.getDBIDs(), k, rnd);
    double[][] means = new double[k][];
    DBIDIter iter = ids.iter();
    for(int i = 0; i < k; i++, iter.advance()) {
      means[i] = relation.get(iter).toArray();
    }
    return means;
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DBIDs ids, DistanceQuery<? super O> distance) {
    return DBIDUtil.randomSample(ids, k, rnd);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<V> extends AbstractKMeansInitialization.Par {
    @Override
    public RandomlyChosen<V> make() {
      return new RandomlyChosen<>(rnd);
    }
  }
}
