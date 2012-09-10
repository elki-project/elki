package de.lmu.ifi.dbs.elki.datasource.filter.transform;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractConversionFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.FilterUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.EigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Apply principal component analysis to the data set.
 * 
 * TODO: add dimensionality reduction
 * 
 * @author Erich Schubert
 * 
 * @param <O> Vector type
 */
public class GlobalPrincipalComponentAnalysisTransform<O extends NumberVector<?>> extends AbstractConversionFilter<O, O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(GlobalPrincipalComponentAnalysisTransform.class);

  /**
   * Filter to use for dimensionality reduction.
   */
  EigenPairFilter filter = null;

  /**
   * Actual dataset dimensionality.
   */
  int dim = -1;

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
   * Vector factory.
   */
  NumberVector.Factory<O, ?> factory;

  /**
   * Constructor.
   * 
   * @param filter Filter to use for dimensionality reduction.
   */
  public GlobalPrincipalComponentAnalysisTransform(EigenPairFilter filter) {
    super();
    this.filter = filter;
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<O> in) {
    if(!(in instanceof VectorFieldTypeInformation)) {
      throw new AbortException("PCA can only applied to fixed dimensionality vectors");
    }
    dim = ((VectorFieldTypeInformation<?>) in).dimensionality();
    covmat = new CovarianceMatrix(dim);
    return true;
  }

  @Override
  protected void prepareProcessInstance(O obj) {
    covmat.put(obj);
  }

  @Override
  protected void prepareComplete() {
    mean = covmat.getMeanVector().getArrayRef();
    PCAResult pcares = (new PCARunner<O>(null)).processCovarMatrix(covmat.destroyToSampleMatrix());
    SortedEigenPairs eps = pcares.getEigenPairs();
    covmat = null;

    if(filter == null) {
      proj = new double[dim][dim];
      for(int d = 0; d < dim; d++) {
        EigenPair ep = eps.getEigenPair(d);
        double[] ev = ep.getEigenvector().getArrayRef();
        double eval = Math.sqrt(ep.getEigenvalue());
        // Fill weighted and transposed:
        for(int i = 0; i < dim; i++) {
          proj[d][i] = ev[i] / eval;
        }
      }
    }
    else {
      List<EigenPair> axes = filter.filter(eps).getStrongEigenPairs();
      final int pdim = axes.size(); // Projection dimensionality
      if (LOG.isVerbose()) {
        LOG.verbose("Reducing dimensionality from "+dim+" to "+pdim+" via PCA.");
      }
      proj = new double[pdim][dim];
      for(int d = 0; d < pdim; d++) {
        EigenPair ep = axes.get(d);
        double[] ev = ep.getEigenvector().getArrayRef();
        double eval = Math.sqrt(ep.getEigenvalue());
        // Fill weighted and transposed:
        for(int i = 0; i < dim; i++) {
          proj[d][i] = ev[i] / eval;
        }
      }
    }
    buf = new double[dim];
  }

  @Override
  protected O filterSingleObject(O obj) {
    // Shift by mean and copy
    for(int i = 0; i < dim; i++) {
      buf[i] = obj.doubleValue(i + 1) - mean[i];
    }
    double[] p = VMath.times(proj, buf);
    return factory.newNumberVector(p);
  }

  @Override
  protected SimpleTypeInformation<? super O> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<? super O> convertedType(SimpleTypeInformation<O> in) {
    if(proj.length == dim) {
      return in;
    }
    else {
      factory = FilterUtil.guessFactory(in);
      return new VectorFieldTypeInformation<O>(factory, proj.length);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * To specify the eigenvectors to keep.
     */
    public static final OptionID FILTER_ID = OptionID.getOrCreateOptionID("globalpca.filter", "Filter to use for dimensionality reduction.");

    /**
     * Filter to use for dimensionality reduction.
     */
    EigenPairFilter filter = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<EigenPairFilter> filterP = new ObjectParameter<EigenPairFilter>(FILTER_ID, EigenPairFilter.class, true);
      if(config.grab(filterP)) {
        filter = filterP.instantiateClass(config);
      }
    }

    @Override
    protected Object makeInstance() {
      return new GlobalPrincipalComponentAnalysisTransform<O>(filter);
    }
  }
}