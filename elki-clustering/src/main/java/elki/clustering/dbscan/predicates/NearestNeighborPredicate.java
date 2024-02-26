/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2024
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
package elki.clustering.dbscan.predicates;

import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.ids.KNNList;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A neighbor predicate using the k nearest neighbors, as used for
 * nearest-neighbor consistency.
 * <p>
 * The basic idea is found in:
 * <p>
 * C. H. Q. Ding, X. He<br>
 * K-nearest-neighbor consistency in data clustering: incorporating local
 * information into global optimization<br>
 * Proc. Symposium on Applied Computing (SAC) 2004
 * <p>
 * but we reformulated this as a predicate in the Generalized DBSCAN framework
 * for ELKI and the research in
 * <p>
 * Lars Lenssen, Niklas Strahmann, Erich Schubert<br>
 * Fast k-Nearest-Neighbor-Consistent Clustering<br>
 * Lernen, Wissen, Daten, Analysen (LWDA), 2023
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
@Alias({ "knn", "nn" })
@Reference(authors = "C. H. Q. Ding, X. He", //
    title = "K-nearest-neighbor consistency in data clustering: incorporating local information into global optimization", //
    booktitle = "Proc. Symposium on Applied Computing (SAC) 2004", //
    url = "https://doi.org/10.1145/967900.968021", bibkey = "DBLP:conf/sac/DingH04")
@Reference(authors = "Lars Lenssen, Niklas Strahmann, Erich Schubert", //
    title = "Fast k-Nearest-Neighbor-Consistent Clustering", //
    booktitle = "Lernen, Wissen, Daten, Analysen (LWDA)", //
    url = "https://ceur-ws.org/Vol-3630/LWDA2023-paper34.pdf", bibkey = "DBLP:conf/lwa/LenssenSS23")
public class NearestNeighborPredicate<O> implements NeighborPredicate<O, KNNList> {
  /**
   * Number of neighbors (+1 for query point)
   */
  protected int kplus1;

  /**
   * Distance function to use
   */
  protected Distance<? super O> distance;

  /**
   * Full constructor.
   *
   * @param k Number of neighbors
   * @param distance Distance function to use
   */
  public NearestNeighborPredicate(int k, Distance<? super O> distance) {
    super();
    this.kplus1 = k + 1;
    this.distance = distance;
  }

  @Override
  public Instance instantiate(Relation<? extends O> relation) {
    KNNSearcher<DBIDRef> knnq = new QueryBuilder<>(relation, distance).kNNByDBID(kplus1);
    return new Instance(kplus1, knnq, relation.getDBIDs());
  }

  @Override
  public SimpleTypeInformation<KNNList> getOutputType() {
    return TypeUtil.KNNLIST;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return distance.getInputTypeRestriction();
  }

  /**
   * Instance for a particular data set.
   *
   * @author Erich Schubert
   */
  public static class Instance implements NeighborPredicate.Instance<KNNList> {
    /**
     * Number of neighbors to query (+1 for query object)
     */
    protected int kplus1;

    /**
     * Range query to use on the database.
     */
    protected KNNSearcher<DBIDRef> knnq;

    /**
     * DBIDs to process
     */
    protected DBIDs ids;

    /**
     * Constructor.
     *
     * @param kplus1 Number of neighbors to query (+1 for query object)
     * @param knnq kNN query to use
     * @param ids DBIDs to process
     */
    public Instance(int kplus1, KNNSearcher<DBIDRef> knnq, DBIDs ids) {
      super();
      this.kplus1 = kplus1;
      this.knnq = knnq;
      this.ids = ids;
    }

    @Override
    public DBIDs getIDs() {
      return ids;
    }

    @Override
    public KNNList getNeighbors(DBIDRef reference) {
      return knnq.getKNN(reference, kplus1);
    }

    @Override
    public DBIDIter iterDBIDs(KNNList neighbors) {
      return neighbors.iter();
    }
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Distance function parameter
     */
    public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("neighbors.distance", "Distance function to use.");

    /**
     * Option id for the number of neighbors
     */
    public static final OptionID KNN_ID = new OptionID("neighbors.knn", "Number of neighbors to use.");

    /**
     * Number of neighbors to query
     */
    protected int k;

    /**
     * Distance function to use
     */
    protected Distance<O> distfun = null;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<O>>(DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distfun = x);
      // Get the epsilon parameter
      new IntParameter(KNN_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
    }

    @Override
    public NearestNeighborPredicate<O> make() {
      return new NearestNeighborPredicate<>(k, distfun);
    }
  }
}
