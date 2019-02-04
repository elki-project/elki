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
package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * The DBSCAN neighbor predicate for a {@link SimilarityFunction}, using all
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
   * Distance function to use
   */
  protected SimilarityFunction<? super O> distFunc;

  /**
   * Full constructor.
   *
   * @param epsilon Epsilon value
   * @param distFunc Distance function to use
   */
  public SimilarityNeighborPredicate(double epsilon, SimilarityFunction<? super O> distFunc) {
    super();
    this.epsilon = epsilon;
    this.distFunc = distFunc;
  }

  @Override
  public Instance instantiate(Database database) {
    SimilarityQuery<O> dq = QueryUtil.getSimilarityQuery(database, distFunc);
    RangeQuery<O> rq = database.getSimilarityRangeQuery(dq);
    return new Instance(epsilon, rq, dq.getRelation().getDBIDs());
  }

  @Override
  public SimpleTypeInformation<DoubleDBIDList> getOutputType() {
    return TypeUtil.NEIGHBORLIST;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return distFunc.getInputTypeRestriction();
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
    protected RangeQuery<?> rq;

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
    public Instance(double epsilon, RangeQuery<?> rq, DBIDs ids) {
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
      return rq.getRangeForDBID(reference, epsilon);
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
  public static class Parameterizer<O> extends AbstractParameterizer {
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
    protected SimilarityFunction<O> distfun = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Get the similarity function.
      ObjectParameter<SimilarityFunction<O>> distanceP = new ObjectParameter<>(SIMILARITY_FUNCTION_ID, SimilarityFunction.class);
      if(config.grab(distanceP)) {
        distfun = distanceP.instantiateClass(config);
      }
      // Get the epsilon parameter
      DoubleParameter epsilonP = new DoubleParameter(EPSILON_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.doubleValue();
      }
    }

    @Override
    protected SimilarityNeighborPredicate<O> makeInstance() {
      return new SimilarityNeighborPredicate<>(epsilon, distfun);
    }
  }
}
