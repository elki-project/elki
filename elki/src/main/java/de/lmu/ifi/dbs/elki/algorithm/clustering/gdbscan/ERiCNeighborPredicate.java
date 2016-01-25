package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

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

import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.ERiC;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * ERiC neighborhood predicate.
 * 
 * Reference:
 * <p>
 * E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, and A. Zimek:<br />
 * On Exploring Complex Relationships of Correlation Clusters.<br />
 * In Proc. 19th International Conference on Scientific and Statistical Database
 * Management (SSDBM 2007), Banff, Canada, 2007.
 * </p>
 * 
 * TODO: improve performance by allowing index support for finding neighbors
 * and/or exploiting the data partitioning better.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, and A. Zimek",//
title = "On Exploring Complex Relationships of Correlation Clusters", //
booktitle = "Proc. 19th International Conference on Scientific and Statistical Database Management (SSDBM 2007), Banff, Canada, 2007", //
url = "http://dx.doi.org/10.1109/SSDBM.2007.21")
public class ERiCNeighborPredicate<V extends NumberVector> implements NeighborPredicate {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ERiCNeighborPredicate.class);

  /**
   * ERiC parameters
   */
  protected final ERiC.Settings settings;

  /**
   * Squared delta value.
   */
  private double deltasq;

  /**
   * Constructor.
   * 
   * @param settings ERiC settings
   */
  public ERiCNeighborPredicate(ERiC.Settings settings) {
    super();
    this.settings = settings;
    this.deltasq = settings.delta * settings.delta;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> NeighborPredicate.Instance<T> instantiate(Database database, SimpleTypeInformation<?> type) {
    Relation<V> relation = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    return (NeighborPredicate.Instance<T>) instantiate(database, relation);
  }

  /**
   * Full instantiation interface.
   * 
   * @param database Database
   * @param relation Relation
   * @return Instance
   */
  public Instance instantiate(Database database, Relation<V> relation) {
    DistanceQuery<V> dq = database.getDistanceQuery(relation, EuclideanDistanceFunction.STATIC);
    KNNQuery<V> knnq = database.getKNNQuery(dq, settings.k);

    WritableDataStore<PCAFilteredResult> storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, PCAFilteredResult.class);

    Duration time = LOG.newDuration(this.getClass().getName() + ".preprocessing-time").begin();
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress(this.getClass().getName(), relation.size(), LOG) : null;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DoubleDBIDList ref = knnq.getKNNForDBID(iditer, settings.k);
      storage.put(iditer, settings.pca.processQueryResult(ref, relation));
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
    LOG.statistics(time.end());
    return new Instance(relation.getDBIDs(), storage, relation);
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public SimpleTypeInformation<?>[] getOutputType() {
    return new SimpleTypeInformation[] { new SimpleTypeInformation<>(PCAFilteredResult.class) };
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   */
  public class Instance extends AbstractRangeQueryNeighborPredicate.Instance<DBIDs, PCAFilteredResult> {
    /**
     * Vector data relation.
     */
    private Relation<? extends NumberVector> relation;

    /**
     * Constructor.
     * 
     * @param ids IDs this is defined for.
     * @param storage Stored models
     */
    public Instance(DBIDs ids, DataStore<PCAFilteredResult> storage, Relation<? extends NumberVector> relation) {
      super(ids, storage);
      this.relation = relation;
    }

    @Override
    public DBIDs getNeighbors(DBIDRef reference) {
      final PCAFilteredResult pca1 = storage.get(reference);
      NumberVector v1 = relation.get(reference);

      // Check for mutual preference reachability:
      HashSetModifiableDBIDs ids = DBIDUtil.newHashSet();
      for(DBIDIter neighbor = relation.iterDBIDs(); neighbor.valid(); neighbor.advance()) {
        final PCAFilteredResult pca2 = storage.get(neighbor);
        NumberVector v2 = relation.get(neighbor);
        // Check correlation dimensionality and mutual reachability
        if(strongNeighbors(v1, v2, pca1, pca2)) {
          ids.add(neighbor);
        }
      }
      return ids;
    }

    @Override
    public DBIDIter iterDBIDs(DBIDs neighbors) {
      return neighbors.iter();
    }

    /**
     * Computes the distance between two given DatabaseObjects according to this
     * distance function. Note, that the first PCA must have equal or more
     * strong eigenvectors than the second PCA.
     * 
     * @param v1 first DatabaseObject
     * @param v2 second DatabaseObject
     * @param pca1 first PCA
     * @param pca2 second PCA
     * @return {@code true} when the two vectors are close enough.
     */
    public boolean strongNeighbors(NumberVector v1, NumberVector v2, PCAFilteredResult pca1, PCAFilteredResult pca2) {
      if(pca1.getCorrelationDimension() != pca2.getCorrelationDimension()) {
        return false;
      }

      if(!approximatelyLinearDependent(pca1, pca2) || !approximatelyLinearDependent(pca2, pca1)) {
        return false;
      }

      Vector v1_minus_v2 = v1.getColumnVector().minusEquals(v2.getColumnVector());
      return MathUtil.mahalanobisDistance(pca1.similarityMatrix(), v1_minus_v2) <= settings.tau && MathUtil.mahalanobisDistance(pca2.similarityMatrix(), v1_minus_v2) <= settings.tau;
    }

    /**
     * Computes the distance between two given DatabaseObjects according to this
     * distance function. Note, that the first PCA must have equal or more
     * strong eigenvectors than the second PCA.
     * 
     * @param v1 first DatabaseObject
     * @param v2 second DatabaseObject
     * @param pca1 first PCA
     * @param pca2 second PCA
     * @return {@code true} when the two vectors are close enough.
     */
    public boolean weakNeighbors(NumberVector v1, NumberVector v2, PCAFilteredResult pca1, PCAFilteredResult pca2) {
      if(pca1.getCorrelationDimension() < pca2.getCorrelationDimension()) {
        return false;
      }

      if(!approximatelyLinearDependent(pca1, pca2)) {
        return false;
      }
      if(pca1.getCorrelationDimension() == pca2.getCorrelationDimension() && !approximatelyLinearDependent(pca2, pca1)) {
        return false;
      }

      Vector v1_minus_v2 = v1.getColumnVector().minusEquals(v2.getColumnVector());
      if(MathUtil.mahalanobisDistance(pca1.similarityMatrix(), v1_minus_v2) > settings.tau) {
        return false;
      }
      if(pca1.getCorrelationDimension() == pca2.getCorrelationDimension()) {
        return MathUtil.mahalanobisDistance(pca2.similarityMatrix(), v1_minus_v2) <= settings.tau;
      }
      return true;
    }

    /**
     * Returns true, if the strong eigenvectors of the two specified PCAs span
     * up the same space. Note, that the first PCA must have at least as many
     * strong eigenvectors than the second PCA.
     * 
     * @param pca1 first PCA
     * @param pca2 second PCA
     * @return true, if the strong eigenvectors of the two specified PCAs span
     *         up the same space
     */
    protected boolean approximatelyLinearDependent(PCAFilteredResult pca1, PCAFilteredResult pca2) {
      Matrix m1_czech = pca1.dissimilarityMatrix();
      Matrix v2_strong = pca2.adapatedStrongEigenvectors();
      for(int i = 0; i < v2_strong.getColumnDimensionality(); i++) {
        Vector v2_i = v2_strong.getCol(i);
        // check, if distance of v2_i to the space of pca_1 > delta
        // (i.e., if v2_i spans up a new dimension)
        double distsq = v2_i.transposeTimes(v2_i) - v2_i.transposeTimesTimes(m1_czech, v2_i);

        // if so, return false
        if(distsq > deltasq) {
          return false;
        }
      }

      return true;
    }

    /**
     * Get the correlation dimensionality of a single object.
     * 
     * @param id Object ID
     * @return correlation dimensionality
     */
    public int dimensionality(DBIDRef id) {
      return storage.get(id).getCorrelationDimension();
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * ERiC settings.
     */
    protected ERiC.Settings settings;

    @Override
    protected void makeOptions(Parameterization config) {
      settings = config.tryInstantiate(ERiC.Settings.class);
    }

    @Override
    protected ERiCNeighborPredicate<V> makeInstance() {
      return new ERiCNeighborPredicate<>(settings);
    }
  }
}
