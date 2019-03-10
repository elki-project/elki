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
package elki.index.preprocessed.localpca;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DoubleDBIDList;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.preprocessed.AbstractPreprocessorIndex;
import elki.logging.progress.FiniteProgress;
import elki.math.linearalgebra.pca.PCAFilteredResult;
import elki.math.linearalgebra.pca.PCAResult;
import elki.math.linearalgebra.pca.PCARunner;
import elki.math.linearalgebra.pca.filter.EigenPairFilter;
import elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;
import elki.utilities.exceptions.EmptyDataException;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
    protected Distance<NV> pcaDistance;

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
     * @param pcaDistance distance Function
     * @param pca PCA runner
     * @param filter Eigenvector filter
     */
    public Factory(Distance<NV> pcaDistance, PCARunner pca, EigenPairFilter filter) {
      super();
      this.pcaDistance = pcaDistance;
      this.pca = pca;
      this.filter = filter;
    }

    @Override
    public abstract AbstractFilteredPCAIndex<NV> instantiate(Relation<NV> relation);

    @Override
    public TypeInformation getInputTypeRestriction() {
      return pcaDistance.getInputTypeRestriction();
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
      protected Distance<NV> pcaDistance;

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
        final ObjectParameter<Distance<NV>> pcaDistanceP = new ObjectParameter<>(PCA_DISTANCE_ID, Distance.class, EuclideanDistance.class);

        if(config.grab(pcaDistanceP)) {
          pcaDistance = pcaDistanceP.instantiateClass(config);
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
