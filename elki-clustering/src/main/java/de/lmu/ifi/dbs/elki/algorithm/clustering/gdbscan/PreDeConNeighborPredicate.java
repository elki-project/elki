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

import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.PreDeCon;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Neighborhood predicate used by PreDeCon.
 * <p>
 * Reference:
 * <p>
 * Christian Böhm, Karin Kailing, Hans-Peter Kriegel, Peer Kröger<br>
 * Density Connected Clustering with Local Subspace Preferences.<br>
 * Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04)
 *
 * @author Peer Kröger
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @navassoc - - - PreDeConModel
 *
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Reference(authors = "Christian Böhm, Karin Kailing, Hans-Peter Kriegel, Peer Kröger", //
    title = "Density Connected Clustering with Local Subspace Preferences", //
    booktitle = "Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04)", //
    url = "https://doi.org/10.1109/ICDM.2004.10087", //
    bibkey = "DBLP:conf/icdm/BohmKKK04")
public class PreDeConNeighborPredicate<V extends NumberVector> extends AbstractRangeQueryNeighborPredicate<V, PreDeConNeighborPredicate.PreDeConModel, PreDeConNeighborPredicate.PreDeConModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(PreDeConNeighborPredicate.class);

  /**
   * Tool to help with parameterization.
   */
  private MeanVariance mvSize = new MeanVariance(), mvVar = new MeanVariance();

  /**
   * PreDeCon settings class.
   */
  private PreDeCon.Settings settings;

  /**
   * Constructor.
   * 
   * @param settings PreDeCon settings
   */
  public PreDeConNeighborPredicate(PreDeCon.Settings settings) {
    // Note: we use squared epsilon!
    super(settings.epsilon * settings.epsilon, SquaredEuclideanDistanceFunction.STATIC);
    this.settings = settings;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Instance instantiate(Database database) {
    DistanceQuery<V> dq = QueryUtil.getDistanceQuery(database, distFunc);
    Relation<V> relation = (Relation<V>) dq.getRelation();
    RangeQuery<V> rq = database.getRangeQuery(dq);
    mvSize.reset();
    mvVar.reset();
    DataStore<PreDeConModel> storage = preprocess(PreDeConModel.class, relation, rq);
    if(LOG.isVerbose()) {
      LOG.verbose("Average neighborhood size: " + mvSize.toString());
      LOG.verbose("Average variance size: " + mvVar.toString());
      final int dim = RelationUtil.dimensionality(relation);
      if(mvSize.getMean() < 5 * dim) {
        LOG.verbose("The epsilon parameter may be chosen too small.");
      }
      else if(mvSize.getMean() > .5 * relation.size()) {
        LOG.verbose("The epsilon parameter may be chosen too large.");
      }
      else {
        LOG.verbose("As a first guess, you can try minPts < " + ((int) mvSize.getMean() / dim) //
            + " and delta > " + mvVar.getMean() + //
            ", but you will need to experiment with these parameters and epsilon.");
      }
    }
    return new Instance(dq.getRelation().getDBIDs(), storage);
  }

  @Override
  protected PreDeConModel computeLocalModel(DBIDRef id, DoubleDBIDList neighbors, Relation<V> relation) {
    final int referenceSetSize = neighbors.size();
    mvSize.put(referenceSetSize);

    // Shouldn't happen:
    if(referenceSetSize < 0) {
      LOG.warning("Empty reference set - should at least include the query point!");
      return new PreDeConModel(Integer.MAX_VALUE, DBIDUtil.EMPTYDBIDS);
    }

    V obj = relation.get(id);
    final int dim = obj.getDimensionality();

    // Per-dimension variances:
    double[] s = new double[dim];
    for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
      V o = relation.get(neighbor);
      for(int d = 0; d < dim; d++) {
        final double diff = obj.doubleValue(d) - o.doubleValue(d);
        s[d] += diff * diff;
      }
    }
    // Adjust for sample size
    for(int d = 0; d < dim; d++) {
      s[d] /= referenceSetSize;
      mvVar.put(s[d]);
    }

    // Preference weight vector
    double[] weights = new double[dim];
    int pdim = 0;
    for(int d = 0; d < dim; d++) {
      if(s[d] <= settings.delta) {
        weights[d] = settings.kappa;
        pdim++;
      }
      else {
        weights[d] = 1.;
      }
    }

    // Check which neighbors survive
    HashSetModifiableDBIDs survivors = DBIDUtil.newHashSet(referenceSetSize);
    for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
      V o = relation.get(neighbor);
      // Weighted Euclidean distance:
      double dev = 0.;
      for(int d = 0; d < dim; d++) {
        final double diff = obj.doubleValue(d) - o.doubleValue(d);
        dev += weights[d] * diff * diff;
      }
      // Note: epsilon was squared - this saves us the sqrt here:
      if(dev <= epsilon) {
        survivors.add(neighbor);
      }
    }

    return new PreDeConModel(pdim, survivors);
  }

  @Override
  Logging getLogger() {
    return LOG;
  }

  /**
   * Model used by PreDeCon for core point property.
   * 
   * @author Erich Schubert
   */
  public static class PreDeConModel {
    /**
     * Preference dimensionality.
     */
    int pdim;

    /**
     * Neighbor ids.
     */
    SetDBIDs ids;

    /**
     * PreDeCon model.
     * 
     * @param pdim Preference dimensionality
     * @param ids Neighbor ids
     */
    public PreDeConModel(int pdim, SetDBIDs ids) {
      super();
      this.pdim = pdim;
      this.ids = ids;
    }
  }

  @Override
  public SimpleTypeInformation<PreDeConModel> getOutputType() {
    return new SimpleTypeInformation<>(PreDeConModel.class);
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   */
  public static class Instance extends AbstractRangeQueryNeighborPredicate.Instance<PreDeConModel, PreDeConModel> {
    /**
     * Constructor.
     * 
     * @param ids IDs this is defined for.
     * @param storage Stored models
     */
    public Instance(DBIDs ids, DataStore<PreDeConModel> storage) {
      super(ids, storage);
    }

    @Override
    public PreDeConModel getNeighbors(DBIDRef reference) {
      final PreDeConModel asymmetric = storage.get(reference);
      // Check for mutual preference reachability:
      HashSetModifiableDBIDs ids = DBIDUtil.newHashSet(asymmetric.ids.size());
      for(DBIDIter neighbor = asymmetric.ids.iter(); neighbor.valid(); neighbor.advance()) {
        if(storage.get(neighbor).ids.contains(reference)) {
          ids.add(neighbor);
        }
      }
      return new PreDeConModel(asymmetric.pdim, ids);
    }

    @Override
    public DBIDIter iterDBIDs(PreDeConModel neighbors) {
      return neighbors.ids.iter();
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * PreDeCon settings.
     */
    protected PreDeCon.Settings settings;

    @Override
    protected void makeOptions(Parameterization config) {
      settings = config.tryInstantiate(PreDeCon.Settings.class);
    }

    @Override
    protected PreDeConNeighborPredicate<V> makeInstance() {
      return new PreDeConNeighborPredicate<>(settings);
    }
  }
}
