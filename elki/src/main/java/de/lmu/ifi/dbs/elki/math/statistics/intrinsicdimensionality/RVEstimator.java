package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

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
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Regularly Varying Functions estimator of the intrinsic dimensionality 
 * 
 * Reference:
 * <p>
 * Amsaleg, L., Chelly, O., Furon, T., Girard, S., Houle, M. E., Kawarabayashi, K. & Nett, M.<br />
 * Estimating Local Intrinsic Dimensionality.<br />
 * http://dl.acm.org/citation.cfm?id=2783405.
 * </p>
 * 
 * @author Oussama Chelly
 * @author Erich Schubert
 */
@Reference(authors = "Amsaleg, L., Chelly, O., Furon, T., Girard, S., Houle, M. E., & Nett, M.", //
title = "Estimating Continuous Intrinsic Dimensionality", //
booktitle = "Proceedings of the 21th ACM SIGKDD.", //
url = "http://dl.acm.org/citation.cfm?id=2783405")
public class RVEstimator extends AbstractIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final RVEstimator STATIC = new RVEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter, final int len) {
    int k = len;
    double n1 = k/2;
    double n2 = 3*k/4;
    double n3 = k-1;
    double r1 = adapter.getDouble(data, (int)n1);
    double r2 = adapter.getDouble(data, (int)n2);
    double r3 = adapter.getDouble(data, (int)n3);
    double p = (r3-r2)/(r1-2*r2+r3);
    double a1 = 1;
    double a2 = (1/p)-1;
    return ( a1*Math.log(n3/n2) + a2*Math.log(n1/n2) ) / ( a1*Math.log(r3/r2) + a2*Math.log(r1/r2) );
  }
  
  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected RVEstimator makeInstance() {
      return STATIC;
    }
  }
}
