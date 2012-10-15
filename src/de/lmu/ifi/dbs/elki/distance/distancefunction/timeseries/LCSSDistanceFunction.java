package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractVectorDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides the Longest Common Subsequence distance for FeatureVectors.
 * 
 * 
 * Adapted for Java, based on Matlab Code by Michalis Vlachos. Original
 * Copyright Notice:
 * 
 * BEGIN COPYRIGHT NOTICE
 * 
 * lcsMatching code -- (c) 2002 Michalis Vlachos
 * (http://www.cs.ucr.edu/~mvlachos)
 * 
 * This code is provided as is, with no guarantees except that bugs are almost
 * surely present. Published reports of research using this code (or a modified
 * version) should cite the article that describes the algorithm:
 * 
 * <p>
 * M. Vlachos, M. Hadjieleftheriou, D. Gunopulos, E. Keogh:<br />
 * Indexing Multi-Dimensional Time-Series with Support for Multiple Distance
 * Measures<br />
 * In Proc. of 9th SIGKDD, Washington, DC, 2003
 * </p>
 * 
 * Comments and bug reports are welcome. Email to mvlachos@cs.ucr.edu I would
 * also appreciate hearing about how you used this code, improvements that you
 * have made to it.
 * 
 * You are free to modify, extend or distribute this code, as long as this
 * copyright notice is included whole and unchanged.
 * 
 * END COPYRIGHT NOTICE
 * 
 * 
 * @author Thomas Bernecker
 */
@Title("Longest Common Subsequence distance function")
@Reference(authors = "M. Vlachos, M. Hadjieleftheriou, D. Gunopulos, E. Keogh", title = "Indexing Multi-Dimensional Time-Series with Support for Multiple Distance Measures", booktitle = "Proceedings of the ninth ACM SIGKDD international conference on Knowledge discovery and data mining", url = "http://dx.doi.org/10.1145/956750.956777")
public class LCSSDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  /**
   * PDELTA parameter
   */
  public static final OptionID PDELTA_ID = OptionID.getOrCreateOptionID("lcss.pDelta", "the allowed deviation in x direction for LCSS alignment (positive double value, 0 <= pDelta <= 1)");

  /**
   * PEPSILON parameter
   */
  public static final OptionID PEPSILON_ID = OptionID.getOrCreateOptionID("lcss.pEpsilon", "the allowed deviation in y directionfor LCSS alignment (positive double value, 0 <= pEpsilon <= 1)");

  /**
   * Keeps the currently set pDelta.
   */
  private double pDelta;

  /**
   * Keeps the currently set pEpsilon.
   */
  private double pEpsilon;

  /**
   * Constructor.
   * 
   * @param pDelta pDelta
   * @param pEpsilon pEpsilon
   */
  public LCSSDistanceFunction(double pDelta, double pEpsilon) {
    super();
    this.pDelta = pDelta;
    this.pEpsilon = pEpsilon;
  }

  /**
   * Provides the Longest Common Subsequence distance between the given two
   * vectors.
   * 
   * @return the Longest Common Subsequence distance between the given two
   *         vectors as an instance of {@link DoubleDistance DoubleDistance}.
   */
  @Override
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    final int delta = (int) Math.ceil(v2.getDimensionality() * pDelta);

    DoubleMinMax extrema1 = VectorUtil.getRangeDouble(v1);
    DoubleMinMax extrema2 = VectorUtil.getRangeDouble(v2);
    double range = Math.max(extrema1.getMax(), extrema2.getMax()) - Math.min(extrema1.getMin(), extrema2.getMin());
    final double epsilon = range * pEpsilon;

    int m = -1;
    int n = -1;
    double[] a, b;

    // put shorter vector first
    if(v1.getDimensionality() < v2.getDimensionality()) {
      m = v1.getDimensionality();
      n = v2.getDimensionality();
      a = new double[m];
      b = new double[n];

      for(int i = 0; i < v1.getDimensionality(); i++) {
        a[i] = v1.doubleValue(i);
      }
      for(int j = 0; j < v2.getDimensionality(); j++) {
        b[j] = v2.doubleValue(j);
      }
    }
    else {
      m = v2.getDimensionality();
      n = v1.getDimensionality();
      a = new double[m];
      b = new double[n];

      for(int i = 0; i < v2.getDimensionality(); i++) {
        a[i] = v2.doubleValue(i);
      }
      for(int j = 0; j < v1.getDimensionality(); j++) {
        b[j] = v1.doubleValue(j);
      }
    }

    double[] curr = new double[n + 1];

    for(int i = 0; i < m; i++) {
      double[] next = new double[n + 1];
      for(int j = Math.max(0, i - delta); j <= Math.min(n - 1, i + delta); j++) {
        if((b[j] + epsilon) >= a[i] && (b[j] - epsilon) <= a[i]) { // match
          next[j + 1] = curr[j] + 1;
        }
        else if(curr[j + 1] > next[j]) { // ins
          next[j + 1] = curr[j + 1];
        }
        else { // del
          next[j + 1] = next[j];
        }
      }
      curr = next;
    }

    // search for maximum in the last line
    double maxEntry = -1;
    for(int i = 1; i < n + 1; i++) {
      if(curr[i] > maxEntry) {
        maxEntry = curr[i];
      }
    }
    double sim = maxEntry / Math.min(m, n);
    return 1 - sim;
  }

  // TODO: relax this to VectorTypeInformation!
  @Override
  public VectorFieldTypeInformation<? super NumberVector<?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if (!this.getClass().equals(obj.getClass())) {
      return false;
    }
    return (this.pDelta == ((LCSSDistanceFunction) obj).pDelta) && (this.pEpsilon == ((LCSSDistanceFunction) obj).pEpsilon);
  }
  
  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected Double pDelta = 0.0;

    protected Double pEpsilon = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter pDeltaP = new DoubleParameter(PDELTA_ID, new IntervalConstraint(0, IntervalBoundary.CLOSE, 1, IntervalBoundary.CLOSE), 0.1);
      if(config.grab(pDeltaP)) {
        pDelta = pDeltaP.doubleValue();
      }

      final DoubleParameter pEpsilonP = new DoubleParameter(PEPSILON_ID, new IntervalConstraint(0, IntervalBoundary.CLOSE, 1, IntervalBoundary.CLOSE), 0.05);
      if(config.grab(pEpsilonP)) {
        pEpsilon = pEpsilonP.doubleValue();
      }
    }

    @Override
    protected LCSSDistanceFunction makeInstance() {
      return new LCSSDistanceFunction(pDelta, pEpsilon);
    }
  }
}