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

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minusEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transposeTimesTimes;

import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.COPAC;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * COPAC neighborhood predicate.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Christian Böhm, Hans-Peter Kriegel, Peer Kröger, Arthur
 * Zimek<br>
 * Robust, Complete, and Efficient Correlation Clustering<br>
 * Proc. 7th SIAM Int. Conf. on Data Mining (SDM'07)
 * <p>
 * TODO: improve performance by allowing index support for finding neighbors
 * and/or exploiting the data partitioning better.
 *
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @navassoc - - - COPACNeighborPredicate.COPACModel
 *
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Reference(authors = "Elke Achtert, Christian Böhm, Hans-Peter Kriegel, Peer Kröger, Arthur Zimek", //
    title = "Robust, Complete, and Efficient Correlation Clustering", //
    booktitle = "Proc. 7th SIAM Int. Conf. on Data Mining (SDM'07)", //
    url = "https://doi.org/10.1137/1.9781611972771.37", //
    bibkey = "DBLP:conf/sdm/AchtertBKKZ07")
public class COPACNeighborPredicate<V extends NumberVector> implements NeighborPredicate<COPACNeighborPredicate.COPACModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(COPACNeighborPredicate.class);

  /**
   * COPAC parameters
   */
  protected final COPAC.Settings settings;

  /**
   * Squared value of epsilon.
   */
  protected double epsilonsq;

  /**
   * Constructor.
   * 
   * @param settings COPAC settings
   */
  public COPACNeighborPredicate(COPAC.Settings settings) {
    super();
    this.settings = settings;
    this.epsilonsq = settings.epsilon * settings.epsilon;
  }

  @Override
  public NeighborPredicate.Instance<COPACModel> instantiate(Database database) {
    return instantiate(database, database.<V> getRelation(TypeUtil.NUMBER_VECTOR_FIELD));
  }

  /**
   * Full instantiation method.
   * 
   * @param database Database
   * @param relation Vector relation
   * @return Instance
   */
  public COPACNeighborPredicate.Instance instantiate(Database database, Relation<V> relation) {
    DistanceQuery<V> dq = database.getDistanceQuery(relation, EuclideanDistanceFunction.STATIC);
    KNNQuery<V> knnq = database.getKNNQuery(dq, settings.k);

    WritableDataStore<COPACModel> storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, COPACModel.class);

    Duration time = LOG.newDuration(this.getClass().getName() + ".preprocessing-time").begin();
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress(this.getClass().getName(), relation.size(), LOG) : null;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DoubleDBIDList ref = knnq.getKNNForDBID(iditer, settings.k);
      storage.put(iditer, computeLocalModel(iditer, ref, relation));
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
    LOG.statistics(time.end());
    return new Instance(relation.getDBIDs(), storage);
  }

  /**
   * COPAC model computation
   * 
   * @param id Query object
   * @param knnneighbors k nearest neighbors
   * @param relation Data relation
   * @return COPAC object model
   */
  protected COPACModel computeLocalModel(DBIDRef id, DoubleDBIDList knnneighbors, Relation<V> relation) {
    PCAResult epairs = settings.pca.processIds(knnneighbors, relation);
    int pdim = settings.filter.filter(epairs.getEigenvalues());
    PCAFilteredResult pcares = new PCAFilteredResult(epairs.getEigenPairs(), pdim, 1., 0.);

    double[][] mat = pcares.similarityMatrix();
    double[] vecP = relation.get(id).toArray();
    if(pdim == vecP.length) {
      // Full dimensional - noise!
      return new COPACModel(pdim, DBIDUtil.EMPTYDBIDS);
    }

    // Check which neighbors survive
    HashSetModifiableDBIDs survivors = DBIDUtil.newHashSet();
    for(DBIDIter neighbor = relation.iterDBIDs(); neighbor.valid(); neighbor.advance()) {
      double[] diff = minusEquals(relation.get(neighbor).toArray(), vecP);
      double cdistP = transposeTimesTimes(diff, mat, diff);
      if(cdistP <= epsilonsq) {
        survivors.add(neighbor);
      }
    }
    return new COPACModel(pdim, survivors);
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  /**
   * Model used by COPAC for core point property.
   * 
   * @author Erich Schubert
   */
  public static class COPACModel implements SetDBIDs {
    /**
     * Correlation dimensionality.
     */
    int cdim;

    /**
     * Neighbor ids.
     */
    SetDBIDs ids;

    /**
     * COPAC model.
     * 
     * @param cdim Correlation dimensionality
     * @param ids Neighbor ids
     */
    public COPACModel(int cdim, SetDBIDs ids) {
      super();
      this.cdim = cdim;
      this.ids = ids;
    }

    @Override
    public DBIDIter iter() {
      return ids.iter();
    }

    @Override
    public int size() {
      return ids.size();
    }

    @Override
    public boolean contains(DBIDRef o) {
      return ids.contains(o);
    }

    @Override
    public boolean isEmpty() {
      return ids.isEmpty();
    }
  }

  @Override
  public SimpleTypeInformation<COPACModel> getOutputType() {
    return new SimpleTypeInformation<>(COPACModel.class);
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   */
  public static class Instance extends AbstractRangeQueryNeighborPredicate.Instance<COPACModel, COPACModel> {
    /**
     * Constructor.
     * 
     * @param ids IDs this is defined for.
     * @param storage Stored models
     */
    public Instance(DBIDs ids, DataStore<COPACModel> storage) {
      super(ids, storage);
    }

    @Override
    public COPACModel getNeighbors(DBIDRef reference) {
      final COPACModel asymmetric = storage.get(reference);
      // We use empty models for noise (full-dimensional).
      if(asymmetric.ids.size() <= 0) {
        return asymmetric;
      }
      // Check for mutual preference reachability:
      HashSetModifiableDBIDs ids = DBIDUtil.newHashSet(asymmetric.ids.size());
      for(DBIDIter neighbor = asymmetric.ids.iter(); neighbor.valid(); neighbor.advance()) {
        final COPACModel nmodel = storage.get(neighbor);
        // Check correlation dimensionality and mutual reachability
        if(nmodel.cdim == asymmetric.cdim && nmodel.ids.contains(reference)) {
          ids.add(neighbor);
        }
      }
      return new COPACModel(asymmetric.cdim, ids);
    }

    @Override
    public DBIDIter iterDBIDs(COPACModel neighbors) {
      return neighbors.ids.iter();
    }

    /**
     * Get the correlation dimensionality of a single object.
     * 
     * @param id Object ID
     * @return correlation dimensionality
     */
    public int dimensionality(DBIDRef id) {
      return storage.get(id).cdim;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * COPAC settings.
     */
    protected COPAC.Settings settings;

    @Override
    protected void makeOptions(Parameterization config) {
      settings = config.tryInstantiate(COPAC.Settings.class);
    }

    @Override
    protected COPACNeighborPredicate<V> makeInstance() {
      return new COPACNeighborPredicate<>(settings);
    }
  }
}
