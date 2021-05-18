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
package elki.datasource.filter.transform;

import static elki.math.linearalgebra.VMath.plusTimesEquals;
import static elki.math.linearalgebra.VMath.times;

import elki.data.NumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.datasource.filter.AbstractVectorConversionFilter;
import elki.logging.Logging;
import elki.math.linearalgebra.CovarianceMatrix;
import elki.math.linearalgebra.pca.EigenPair;
import elki.math.linearalgebra.pca.PCAResult;
import elki.math.linearalgebra.pca.PCARunner;
import elki.math.linearalgebra.pca.filter.EigenPairFilter;
import elki.utilities.Alias;
import elki.utilities.Priority;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Apply Principal Component Analysis (PCA) to the data set.
 * <p>
 * This is also popular form of "Whitening transformation", and will project the
 * data to have a unit covariance matrix.
 * <p>
 * If you want to also reduce dimensionality, set the {@code -pca.filter}
 * parameter! Note that this implementation currently will always perform a full
 * matrix inversion. For very high dimensional data, this can take an excessive
 * amount of time O(d³) and memory O(d²). Please contribute a better
 * implementation to ELKI that only computes the requiried dimensions, yet
 * allows for the same filtering flexibility.
 * <p>
 * TODO: design an API (and implementation) that allows plugging in efficient
 * solvers that do not need to decompose the entire matrix. This may, however,
 * require external dependencies such as jBlas.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @composed - - - PCARunner
 * @composed - - - CovarianceMatrix
 * @composed - - - EigenPairFilter
 * 
 * @param <O> Vector type
 */
@Alias({ "whiten", "whitening", "pca" })
@Priority(Priority.RECOMMENDED)
public class GlobalPrincipalComponentAnalysisTransform<O extends NumberVector> extends AbstractVectorConversionFilter<O, O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(GlobalPrincipalComponentAnalysisTransform.class);

  /**
   * Transformation mode.
   */
  public enum Mode {
    /** Center, rotate, and scale */
    FULL,
    /** Center and rotate */
    CENTER_ROTATE,
  }

  /**
   * Filter to use for dimensionality reduction.
   */
  EigenPairFilter filter = null;

  /**
   * Covariance matrix builder.
   */
  CovarianceMatrix covmat = null;

  /**
   * Final projection after analysis run.
   */
  double[][] proj = null;

  /**
   * Projection buffer.
   */
  double[] buf = null;

  /**
   * Vector for data set centering.
   */
  double[] mean = null;

  /**
   * Mode.
   */
  Mode mode;

  /**
   * Constructor.
   * 
   * @param filter Filter to use for dimensionality reduction.
   */
  public GlobalPrincipalComponentAnalysisTransform(EigenPairFilter filter) {
    this(filter, Mode.FULL);
  }

  /**
   * Constructor.
   * 
   * @param filter Filter to use for dimensionality reduction.
   * @param mode Mode
   */
  public GlobalPrincipalComponentAnalysisTransform(EigenPairFilter filter, Mode mode) {
    super();
    this.filter = filter;
    this.mode = mode;
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<O> in) {
    if(!(in instanceof VectorFieldTypeInformation)) {
      throw new AbortException("PCA can only applied to fixed dimensionality vectors");
    }
    int dim = ((VectorFieldTypeInformation<?>) in).getDimensionality();
    covmat = new CovarianceMatrix(dim);
    proj = null;
    mean = null;
    return true;
  }

  @Override
  protected void prepareProcessInstance(O obj) {
    covmat.put(obj);
  }

  @Override
  protected void prepareComplete() {
    mean = covmat.getMeanVector();
    PCAResult pcares = (new PCARunner(null)).processCovarMatrix(covmat.destroyToPopulationMatrix());
    covmat = null;

    final int dim = mean.length;
    final int pdim = filter != null ? filter.filter(pcares.getEigenvalues()) : dim;
    if(filter != null && LOG.isVerbose()) {
      LOG.verbose("Reducing dimensionality from " + dim + " to " + pdim + " via PCA.");
    }
    // Build the projection matrux
    proj = new double[pdim][dim];
    for(int d = 0; d < pdim; d++) {
      EigenPair ep = pcares.getEigenPairs()[d];
      plusTimesEquals(proj[d], ep.getEigenvector(), mode == Mode.FULL ? 1. / Math.sqrt(ep.getEigenvalue()) : 1.);
    }
    buf = new double[dim];
  }

  @Override
  protected O filterSingleObject(O obj) {
    // Shift by mean and copy to scratch buffer
    for(int i = 0; i < mean.length; i++) {
      buf[i] = obj.doubleValue(i) - mean[i];
    }
    return factory.newNumberVector(times(proj, buf));
  }

  @Override
  protected SimpleTypeInformation<? super O> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<? super O> convertedType(SimpleTypeInformation<O> in) {
    initializeOutputType(in);
    return new VectorFieldTypeInformation<>(factory, proj.length);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<O extends NumberVector> implements Parameterizer {
    /**
     * To specify the eigenvectors to keep.
     */
    public static final OptionID FILTER_ID = new OptionID("globalpca.filter", "Filter to use for dimensionality reduction.");

    /**
     * Mode control.
     */
    public static final OptionID MODE_ID = new OptionID("globalpca.mode", "Operation mode: full, or rotate only.");

    /**
     * Filter to use for dimensionality reduction.
     */
    EigenPairFilter filter = null;

    /**
     * Mode.
     */
    Mode mode;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<EigenPairFilter>(FILTER_ID, EigenPairFilter.class) //
          .setOptional(true) //
          .grab(config, x -> filter = x);
      new EnumParameter<Mode>(MODE_ID, Mode.class, Mode.FULL) //
          .grab(config, x -> mode = x);
    }

    @Override
    public GlobalPrincipalComponentAnalysisTransform<O> make() {
      return new GlobalPrincipalComponentAnalysisTransform<>(filter, mode);
    }
  }
}
