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
package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.meta.BestFitEstimator;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * CDF based outlier score scaling.
 * <p>
 * Enhanced version of the scaling proposed in:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek<br>
 * Interpreting and Unifying Outlier Scores<br>
 * Proc. 11th SIAM International Conference on Data Mining (SDM 2011)
 * <p>
 * See also:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek<br>
 * Outlier Detection in Arbitrarily Oriented Subspaces<br>
 * in: Proc. IEEE Int. Conf. on Data Mining (ICDM 2012)
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
    title = "Outlier Detection in Arbitrarily Oriented Subspaces", //
    booktitle = "Proc. IEEE Int. Conf. on Data Mining (ICDM 2012)", //
    url = "https://doi.org/10.1109/ICDM.2012.21", //
    bibkey = "DBLP:conf/icdm/KriegelKSZ12")
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
    title = "Interpreting and Unifying Outlier Scores", //
    booktitle = "Proc. 11th SIAM International Conference on Data Mining (SDM 2011)", //
    url = "https://doi.org/10.1137/1.9781611972818.2", //
    bibkey = "DBLP:conf/sdm/KriegelKSZ11")
public class COPOutlierScaling implements OutlierScaling {
  /**
   * Phi parameter.
   */
  private double phi = 0.;

  /**
   * Score distribution.
   */
  private Distribution dist;

  /**
   * Inversion flag.
   */
  private boolean inverted = false;

  /**
   * Constructor.
   * 
   * @param phi Phi parameter
   */
  public COPOutlierScaling(double phi) {
    super();
    this.phi = phi;
  }

  @Override
  public double getScaled(double value) {
    if(dist == null) {
      throw new AbortException("Programming error: outlier scaling not initialized.");
    }
    double s = inverted ? (1 - dist.cdf(value)) : dist.cdf(value);
    return (phi > 0.) ? (phi * s) / (1 - s + phi) : s;
  }

  @Override
  public double getMin() {
    return 0.;
  }

  @Override
  public double getMax() {
    return 1.;
  }

  @Override
  public void prepare(OutlierResult or) {
    DoubleRelation scores = or.getScores();
    double[] s = new double[scores.size()];
    int i = 0;
    for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance(), i++) {
      s[i] = scores.doubleValue(id);
    }
    Arrays.sort(s);
    dist = BestFitEstimator.STATIC.estimate(s, ArrayLikeUtil.DOUBLEARRAYADAPTER);
    inverted = (or.getOutlierMeta() instanceof InvertedOutlierScoreMeta);
  }

  @Override
  public <A> void prepare(A array, NumberArrayAdapter<?, A> adapter) {
    double[] s = ArrayLikeUtil.toPrimitiveDoubleArray(array, adapter);
    Arrays.sort(s);
    dist = BestFitEstimator.STATIC.estimate(s, ArrayLikeUtil.DOUBLEARRAYADAPTER);
    inverted = false; // Not supported
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Phi parameter.
     */
    public static final OptionID PHI_ID = new OptionID("copscaling.phi", "Phi parameter, expected rate of outliers. Set to 0 to use raw CDF values.");

    /**
     * Phi value.
     */
    private double phi = 0.;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter phiP = new DoubleParameter(PHI_ID);
      if(config.grab(phiP)) {
        phi = phiP.doubleValue();
      }
    }

    @Override
    protected COPOutlierScaling makeInstance() {
      return new COPOutlierScaling(phi);
    }
  }
}
