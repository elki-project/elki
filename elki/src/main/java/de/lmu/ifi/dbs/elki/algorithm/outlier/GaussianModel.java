package de.lmu.ifi.dbs.elki.algorithm.outlier;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Outlier have smallest GMOD_PROB: the outlier scores is the
 * <em>probability density</em> of the assumed distribution.
 * 
 * @author Lisa Reichert
 * 
 * @param <V> Vector type
 */
@Title("Gaussian Model Outlier Detection")
@Description("Fit a multivariate gaussian model onto the data, and use the PDF to compute an outlier score.")
public class GaussianModel<V extends NumberVector> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(GaussianModel.class);

  /**
   * OptionID for inversion flag.
   */
  public static final OptionID INVERT_ID = new OptionID("gaussod.invert", "Invert the value range to [0:1], with 1 being outliers instead of 0.");

  /**
   * Small value to increment diagonally of a matrix in order to avoid
   * singularity before building the inverse.
   */
  private static final double SINGULARITY_CHEAT = 1E-9;

  /**
   * Invert the result
   */
  private boolean invert = false;

  /**
   * Constructor with actual parameters.
   * 
   * @param invert inversion flag.
   */
  public GaussianModel(boolean invert) {
    super();
    this.invert = invert;
  }

  /**
   * Run the algorithm
   * 
   * @param relation Data relation
   * @return Outlier result
   */
  public OutlierResult run(Relation<V> relation) {
    DoubleMinMax mm = new DoubleMinMax();
    // resulting scores
    WritableDoubleDataStore oscores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);

    // Compute mean and covariance Matrix
    CovarianceMatrix temp = CovarianceMatrix.make(relation);
    Vector mean = temp.getMeanVector(relation).getColumnVector();
    // debugFine(mean.toString());
    Matrix covarianceMatrix = temp.destroyToNaiveMatrix();
    // debugFine(covarianceMatrix.toString());
    Matrix covarianceTransposed = covarianceMatrix.cheatToAvoidSingularity(SINGULARITY_CHEAT).inverse();

    // Normalization factors for Gaussian PDF
    final double fakt = (1.0 / (Math.sqrt(MathUtil.powi(MathUtil.TWOPI, RelationUtil.dimensionality(relation)) * covarianceMatrix.det())));

    // for each object compute Mahalanobis distance
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      Vector x = relation.get(iditer).getColumnVector().minusEquals(mean);
      // Gaussian PDF
      final double mDist = x.transposeTimesTimes(covarianceTransposed, x);
      final double prob = fakt * Math.exp(-mDist / 2.0);

      mm.put(prob);
      oscores.putDouble(iditer, prob);
    }

    final OutlierScoreMeta meta;
    if(invert) {
      double max = mm.getMax() != 0 ? mm.getMax() : 1.;
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        oscores.putDouble(iditer, (max - oscores.doubleValue(iditer)) / max);
      }
      meta = new BasicOutlierScoreMeta(0.0, 1.0);
    }
    else {
      meta = new InvertedOutlierScoreMeta(mm.getMin(), mm.getMax(), 0.0, Double.POSITIVE_INFINITY);
    }
    DoubleRelation res = new MaterializedDoubleRelation("Gaussian Model Outlier Score", "gaussian-model-outlier", oscores, relation.getDBIDs());
    return new OutlierResult(meta, res);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
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
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    protected boolean invert = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final Flag flag = new Flag(INVERT_ID);
      if(config.grab(flag)) {
        invert = flag.getValue();
      }
    }

    @Override
    protected GaussianModel<V> makeInstance() {
      return new GaussianModel<>(invert);
    }
  }
}