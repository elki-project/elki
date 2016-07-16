package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;

/**
 * Distribuition estimators that use the method of moments (MOM) in
 * exponentiated data.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <D> Distribution estimated.
 */
public interface ExpMADDistributionEstimator<D extends Distribution> extends DistributionEstimator<D> {
  /**
   * General form of the parameter estimation
   * 
   * @param median Median lof exp values.
   * @param mad Median absolute deviation from median (in expspace).
   * @return Estimated distribution
   */
  D estimateFromExpMedianMAD(double median, double mad);
}
