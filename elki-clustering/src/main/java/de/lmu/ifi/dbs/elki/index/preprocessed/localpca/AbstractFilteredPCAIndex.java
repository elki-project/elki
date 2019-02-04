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
package de.lmu.ifi.dbs.elki.index.preprocessed.localpca;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.EigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.EmptyDataException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for a local PCA based index.
 *
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - PCAFilteredRunner
 *
 * @param <NV> Vector type
 */
@Title("Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database.")
public abstract class AbstractFilteredPCAIndex<NV extends NumberVector> extends AbstractPreprocessorIndex<NV, PCAFilteredResult> implements FilteredLocalPCAIndex<NV> {
  /**
   * PCA utility object.
   */
  protected final PCARunner pca;

  /**
   * Filter for selecting eigenvectors
   */
  protected EigenPairFilter filter;

  /**
   * Constructor.
   *
   * @param relation Relation to use
   * @param pca PCA runner to use
   * @param filter Filter for Eigenvectors
   */
  public AbstractFilteredPCAIndex(Relation<NV> relation, PCARunner pca, EigenPairFilter filter) {
    super(relation);
    this.pca = pca;
    this.filter = filter;
  }

  @Override
  public void initialize() {
    if(relation == null || relation.size() <= 0) {
      throw new EmptyDataException();
    }

    // Note: this is required for ERiC to work properly, otherwise the data is
    // recomputed for the partitions!
    if(storage != null) {
      return;
    }

    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, PCAFilteredResult.class);

    long start = System.currentTimeMillis();
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Performing local PCA", relation.size(), getLogger()) : null;

    // TODO: use a bulk operation?
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      PCAResult epairs = pca.processIds(objectsForPCA(iditer), relation);
      int numstrong = filter.filter(epairs.getEigenvalues());
      PCAFilteredResult pcares = new PCAFilteredResult(epairs.getEigenPairs(), numstrong, 1., 0.);
      storage.put(iditer, pcares);
      getLogger().incrementProcessed(progress);
    }
    getLogger().ensureCompleted(progress);

    if(getLogger().isVerbose()) {
      long elapsedTime = System.currentTimeMillis() - start;
      getLogger().verbose(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
    }
  }

  @Override
  public PCAFilteredResult getLocalProjection(DBIDRef objid) {
    if(storage == null) {
      initialize();
    }
    return storage.get(objid);
  }

  /**
   * Returns the objects to be considered within the PCA for the specified query
   * object.
   *
   * @param id the id of the query object for which a PCA should be performed
   * @return the list of the objects (i.e. the ids and the distances to the
   *         query object) to be considered within the PCA
   */
  protected abstract DoubleDBIDList objectsForPCA(DBIDRef id);

  /**
   * Factory class.
   *
   * @author Erich Schubert
   *
   * @stereotype factory
   * @navassoc - create - AbstractFilteredPCAIndex
   */
  public abstract static class Factory<NV extends NumberVector> implements FilteredLocalPCAIndex.Factory<NV> {
    /**
     * Holds the instance of the distance function specified by
     * {@link Parameterizer#PCA_DISTANCE_ID}.
     */
    protected DistanceFunction<NV> pcaDistanceFunction;

    /**
     * PCA utility object.
     */
    protected PCARunner pca;

    /**
     * Filter for selecting eigenvectors
     */
    protected EigenPairFilter filter;

    /**
     * Constructor.
     *
     * @param pcaDistanceFunction distance Function
     * @param pca PCA runner
     * @param filter Eigenvector filter
     */
    public Factory(DistanceFunction<NV> pcaDistanceFunction, PCARunner pca, EigenPairFilter filter) {
      super();
      this.pcaDistanceFunction = pcaDistanceFunction;
      this.pca = pca;
      this.filter = filter;
    }

    @Override
    public abstract AbstractFilteredPCAIndex<NV> instantiate(Relation<NV> relation);

    @Override
    public TypeInformation getInputTypeRestriction() {
      return pcaDistanceFunction.getInputTypeRestriction();
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public abstract static class Parameterizer<NV extends NumberVector, I extends AbstractFilteredPCAIndex<NV>> extends AbstractParameterizer {
      /**
       * Parameter to specify the distance function used for running PCA.
       *
       * Key: {@code -localpca.distancefunction}
       */
      public static final OptionID PCA_DISTANCE_ID = new OptionID("localpca.distancefunction", "The distance function used to select objects for running PCA.");

      /**
       * Holds the instance of the distance function specified by
       * {@link #PCA_DISTANCE_ID}.
       */
      protected DistanceFunction<NV> pcaDistanceFunction;

      /**
       * PCA utility object.
       */
      protected PCARunner pca;

      /**
       * Filter for selecting eigenvectors
       */
      protected EigenPairFilter filter;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final ObjectParameter<DistanceFunction<NV>> pcaDistanceFunctionP = new ObjectParameter<>(PCA_DISTANCE_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

        if(config.grab(pcaDistanceFunctionP)) {
          pcaDistanceFunction = pcaDistanceFunctionP.instantiateClass(config);
        }

        ObjectParameter<PCARunner> pcaP = new ObjectParameter<>(PCARunner.Parameterizer.PCARUNNER_ID, PCARunner.class, PCARunner.class);
        if(config.grab(pcaP)) {
          pca = pcaP.instantiateClass(config);
        }

        ObjectParameter<EigenPairFilter> filterP = new ObjectParameter<>(EigenPairFilter.PCA_EIGENPAIR_FILTER, EigenPairFilter.class, PercentageEigenPairFilter.class);
        if(config.grab(filterP)) {
          filter = filterP.instantiateClass(config);
        }
      }
    }
  }
}
