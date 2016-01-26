package de.lmu.ifi.dbs.elki.algorithm.outlier.lof;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Influence Outliers using Symmetric Relationship (INFLO) using two-way search,
 * is an outlier detection method based on LOF; but also using the reverse kNN.
 *
 * Reference: <br>
 * <p>
 * W. Jin, A. Tung, J. Han, and W. Wang<br />
 * Ranking outliers using symmetric neighborhood relationship<br />
 * Proc. 10th Pacific-Asia conference on Advances in Knowledge Discovery and
 * Data Mining, 2006.
 * </p>
 *
 * @author Ahmed Hettab
 * @author Erich Schubert
 * @since 0.3
 *
 * @apiviz.has KNNQuery
 *
 * @param <O> the type of DatabaseObject the algorithm is applied on
 */
@Title("INFLO: Influenced Outlierness Factor")
@Description("Ranking Outliers Using Symmetric Neigborhood Relationship")
@Reference(authors = "W. Jin, A. Tung, J. Han, and W. Wang", //
title = "Ranking outliers using symmetric neighborhood relationship", //
booktitle = "Proc. 10th Pacific-Asia conference on Advances in Knowledge Discovery and Data Mining", //
url = "http://dx.doi.org/10.1007/11731139_68")
@Alias("de.lmu.ifi.dbs.elki.algorithm.outlier.INFLO")
public class INFLO<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(INFLO.class);

  /**
   * Pruning threshold m.
   */
  private double m;

  /**
   * Number of neighbors to use.
   */
  private int k;

  /**
   * Constructor with parameters.
   *
   * @param distanceFunction Distance function in use
   * @param m m Parameter
   * @param k k Parameter
   */
  public INFLO(DistanceFunction<? super O> distanceFunction, double m, int k) {
    super(distanceFunction);
    this.m = m;
    this.k = k;
  }

  /**
   * Run the algorithm
   *
   * @param database Database to process
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    DistanceQuery<O> distFunc = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O> knnQuery = database.getKNNQuery(distFunc, k + 1, DatabaseQuery.HINT_HEAVY_USE);

    ModifiableDBIDs pruned = DBIDUtil.newHashSet();
    // KNNS
    WritableDataStore<ModifiableDBIDs> knns = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, ModifiableDBIDs.class);
    // RNNS
    WritableDataStore<ModifiableDBIDs> rnns = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, ModifiableDBIDs.class);
    // density
    WritableDoubleDataStore density = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
    // init knns and rnns
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      knns.put(iditer, DBIDUtil.newArray());
      rnns.put(iditer, DBIDUtil.newArray());
    }

    computeNeighborhoods(relation, knnQuery, pruned, knns, rnns, density);

    // Calculate INFLO for any Object
    DoubleMinMax inflominmax = new DoubleMinMax();
    WritableDoubleDataStore inflos = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    // Note: this modifies knns, by adding rknns!
    computeINFLO(relation, pruned, knns, rnns, density, inflos, inflominmax);

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Influence Outlier Score", "inflo-outlier", inflos, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(inflominmax.getMin(), inflominmax.getMax(), 0., Double.POSITIVE_INFINITY, 1.);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Compute neighborhoods
   *
   * @param relation
   * @param knnQuery
   * @param pruned
   * @param knns
   * @param rnns
   * @param density
   */
  protected void computeNeighborhoods(Relation<O> relation, KNNQuery<O> knnQuery, ModifiableDBIDs pruned, WritableDataStore<ModifiableDBIDs> knns, WritableDataStore<ModifiableDBIDs> rnns, WritableDoubleDataStore density) {
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      // if not visited count=0
      int count = rnns.get(iter).size();
      DBIDs knn = getKNN(iter, knnQuery, knns, density);
      for(DBIDIter niter = knn.iter(); niter.valid(); niter.advance()) {
        // Ignore the query point itself.
        if(DBIDUtil.equal(iter, niter)) {
          continue;
        }
        if(getKNN(niter, knnQuery, knns, density).contains(iter)) {
          rnns.get(niter).add(iter);
          rnns.get(iter).add(niter);
          count++;
        }
      }
      if(count >= knn.size() * m) {
        pruned.add(iter);
      }
    }
  }

  /**
   * Compute the final INFLO scores.
   *
   * @param relation Data relation
   * @param pruned Pruned objects
   * @param knns kNN storage
   * @param rnns reverse kNN storage
   * @param density Density estimation
   * @param inflos Inflo score storage
   * @param inflominmax Output of minimum and maximum
   */
  protected void computeINFLO(Relation<O> relation, ModifiableDBIDs pruned, WritableDataStore<ModifiableDBIDs> knns, WritableDataStore<ModifiableDBIDs> rnns, WritableDoubleDataStore density, WritableDoubleDataStore inflos, DoubleMinMax inflominmax) {
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      if(pruned.contains(iter)) {
        inflos.putDouble(iter, 1.);
        inflominmax.put(1.);
        continue;
      }
      ModifiableDBIDs knn = knns.get(iter), rnn = rnns.get(iter);
      knn.addDBIDs(rnn);
      // Compute mean density of NN \cup RNN
      double sum = 0.;
      int c = 0;
      for(DBIDIter niter = knn.iter(); niter.valid(); niter.advance()) {
        if(DBIDUtil.equal(iter, niter)) {
          continue;
        }
        sum += density.doubleValue(niter);
        c++;
      }
      double denP = density.doubleValue(iter);
      final double inflo;
      if(denP > 0.) {
        inflo = denP < Double.POSITIVE_INFINITY ? sum / (c * denP) : 1.;
      }
      else {
        inflo = sum == 0 ? 1. : Double.POSITIVE_INFINITY;
      }
      inflos.putDouble(iter, inflo);
      // update minimum and maximum
      inflominmax.put(inflo);
    }
  }

  /**
   * Get the (forward only) kNN of an object, including the query point
   *
   * @param q Query point
   * @param knnQuery Query function
   * @param knns kNN storage
   * @param density Density storage
   * @return Neighbor list
   */
  protected DBIDs getKNN(DBIDIter q, KNNQuery<O> knnQuery, WritableDataStore<ModifiableDBIDs> knns, WritableDoubleDataStore density) {
    ModifiableDBIDs s = knns.get(q);
    if(s.size() == 0) {
      KNNList listQ = knnQuery.getKNNForDBID(q, k + 1);
      s.addDBIDs(listQ);
      density.putDouble(q, 1. / listQ.getKNNDistance());
    }
    return s;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter to specify if any object is a Core Object must be a double
     * greater than 0.0
     *
     * see paper "Two-way search method" 3.2
     */
    public static final OptionID M_ID = new OptionID("inflo.m", "The pruning threshold");

    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its INFLO score.
     */
    public static final OptionID K_ID = new OptionID("inflo.k", "The number of nearest neighbors of an object to be considered for computing its INFLO score.");

    /**
     * M parameter
     */
    protected double m = 1.0;

    /**
     * Number of neighbors to use.
     */
    protected int k = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter mP = new DoubleParameter(M_ID, 1.0)//
      .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(mP)) {
        m = mP.doubleValue();
      }

      final IntParameter kP = new IntParameter(K_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }
    }

    @Override
    protected INFLO<O> makeInstance() {
      return new INFLO<>(distanceFunction, m, k);
    }
  }
}
