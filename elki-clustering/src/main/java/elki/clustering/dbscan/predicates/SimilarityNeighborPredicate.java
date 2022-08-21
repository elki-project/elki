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
package elki.clustering.dbscan.predicates;

import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.similarity.Similarity;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * The DBSCAN neighbor predicate for a {@link Similarity}, using all
 * neighbors with a minimum similarity.
 * <p>
 * Reference:
 * <p>
 * Martin Ester, Hans-Peter Kriegel, Jörg Sander, Xiaowei Xu<br>
 * A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases
 * with Noise<br>
 * Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96)
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - Instance
 *
 * @param <O> object type
 */
@Reference(authors = "Martin Ester, Hans-Peter Kriegel, Jörg Sander, Xiaowei Xu", //
    title = "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise", //
    booktitle = "Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96)", //
    url = "http://www.aaai.org/Library/KDD/1996/kdd96-037.php", //
    bibkey = "DBLP:conf/kdd/EsterKSX96")
public class SimilarityNeighborPredicate<O> implements NeighborPredicate<DoubleDBIDList> {
  /**
   * Range to query with
   */
  protected double epsilon;

  /**
   * Similarity function to use
   */
  protected Similarity<? super O> simFunc;

  /**
   * Full constructor.
   *
   * @param epsilon Epsilon value
   * @param simFunc Similarity function to use
   */
  public SimilarityNeighborPredicate(double epsilon, Similarity<? super O> simFunc) {
    super();
    this.epsilon = epsilon;
    this.simFunc = simFunc;
  }

  @Override
  public Instance instantiate(Database database) {
    Relation<O> relation = database.getRelation(simFunc.getInputTypeRestriction());
    RangeSearcher<DBIDRef> rq = new QueryBuilder<>(relation, simFunc).similarityRangeByDBID(epsilon);
    return new Instance(epsilon, rq, relation.getDBIDs());
  }

  @Override
  public SimpleTypeInformation<DoubleDBIDList> getOutputType() {
    return TypeUtil.NEIGHBORLIST;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return simFunc.getInputTypeRestriction();
  }

  /**
   * Instance for a particular data set.
   *
   * @author Erich Schubert
   */
  public static class Instance implements NeighborPredicate.Instance<DoubleDBIDList> {
    /**
     * Range to query with
     */
    protected double epsilon;

    /**
     * Range query to use on the database.
     */
    protected RangeSearcher<DBIDRef> rq;

    /**
     * DBIDs to process
     */
    protected DBIDs ids;

    /**
     * Constructor.
     *
     * @param epsilon Epsilon
     * @param rq Range query to use
     * @param ids DBIDs to process
     */
    public Instance(double epsilon, RangeSearcher<DBIDRef> rq, DBIDs ids) {
      super();
      this.epsilon = epsilon;
      this.rq = rq;
      this.ids = ids;
    }

    @Override
    public DBIDs getIDs() {
      return ids;
    }

    @Override
    public DoubleDBIDList getNeighbors(DBIDRef reference) {
      return rq.getRange(reference, epsilon, DBIDUtil.newDistanceDBIDList());
    }

    @Override
    public DBIDIter iterDBIDs(DoubleDBIDList neighbors) {
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
     * Similarity function parameter.
     */
    public static final OptionID SIMILARITY_FUNCTION_ID = new OptionID("gdbscan.similarity", "Similarity function to use.");

    /**
     * Similarity threshold
     */
    public static final OptionID EPSILON_ID = new OptionID("gdbscan.minsim", "Minimum similarity of points to cluster.");

    /**
     * Minimum similarity threshold
     */
    protected double epsilon;

    /**
     * Similarity function to use
     */
    protected Similarity<O> distfun = null;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Similarity<O>>(SIMILARITY_FUNCTION_ID, Similarity.class) //
          .grab(config, x -> distfun = x);
      // Get the epsilon parameter
      new DoubleParameter(EPSILON_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> epsilon = x);
    }

    @Override
    public SimilarityNeighborPredicate<O> make() {
      return new SimilarityNeighborPredicate<>(epsilon, distfun);
    }
  }
}
