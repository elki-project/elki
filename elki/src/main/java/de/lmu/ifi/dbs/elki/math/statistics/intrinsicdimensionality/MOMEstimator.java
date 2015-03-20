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

/**
 * Methods of moments estimator, using the first moment (i.e. average).
 * 
 * This could be generalized to higher order moments, but the variance increases
 * with the order, and we need this to work well with small sample sizes.
 * 
 * Reference:
 * <p>
 * Amsaleg, L., Chelly, O., Furon, T., Girard, S., Houle, M. E., & Nett, M.<br />
 * Estimating Continuous Intrinsic Dimensionality.<br />
 * NII Technical Report NII-2014-001E.
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "Amsaleg, L., Chelly, O., Furon, T., Girard, S., Houle, M. E., & Nett, M.", //
title = "Estimating Continuous Intrinsic Dimensionality", //
booktitle = "NII Technical Report NII-2014-001E.", //
url = "http://www.nii.ac.jp/TechReports/14-001E.pdf")
public class MOMEstimator extends AbstractIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final MOMEstimator STATIC = new MOMEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter, final int len) {
    double v1 = 0.;
    final int num = len - 1;
    for(int i = 0; i < num; i++) {
      v1 += adapter.getDouble(data, i);
    }
    v1 /= num * adapter.getDouble(data, len - 1);
    return v1 / (1 - v1);
  }
}
