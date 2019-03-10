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
package elki.outlier;

import static elki.math.linearalgebra.VMath.*;

import elki.AbstractAlgorithm;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.math.MathUtil;
import elki.math.linearalgebra.CovarianceMatrix;
import elki.math.linearalgebra.LUDecomposition;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.InvertedOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;

import net.jafama.FastMath;

/**
 * Outlier detection based on the probability density of the single normal
 * distribution.
 * 
 * @author Lisa Reichert
 * @since 0.3
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
    double[] mean = temp.getMeanVector(relation).toArray();
    // debugFine(mean.toString());
    double[][] covarianceMatrix = temp.destroyToPopulationMatrix();
    // debugFine(covarianceMatrix.toString());
    double[][] covarianceTransposed = inverse(covarianceMatrix);

    // Normalization factors for Gaussian PDF
    double det = new LUDecomposition(covarianceMatrix).det();
    final double fakt = 1.0 / FastMath.sqrt(MathUtil.powi(MathUtil.TWOPI, RelationUtil.dimensionality(relation)) * det);

    // for each object compute Mahalanobis distance
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double[] x = minusEquals(relation.get(iditer).toArray(), mean);
      // Gaussian PDF
      final double mDist = transposeTimesTimes(x, covarianceTransposed, x);
      final double prob = fakt * FastMath.exp(-mDist * .5);

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
    DoubleRelation res = new MaterializedDoubleRelation("Gaussian Model Outlier Score", relation.getDBIDs(), oscores);
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
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * OptionID for inversion flag.
     */
    public static final OptionID INVERT_ID = new OptionID("gaussod.invert", "Invert the value range to [0:1], with 1 being outliers instead of 0.");

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
