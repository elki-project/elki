package de.lmu.ifi.dbs.elki.math.statistics;

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


/**
 * Epanechnikov kernel density estimator.
 * 
 * @author Erich Schubert
 */
public final class EpanechnikovKernelDensityFunction implements KernelDensityFunction {
  @Override
  public double density(double delta) {
    if(delta < 1) {
      return 0.75 * (1 - delta * delta);
    }
    return 0;
  }

  /**
   * Private, empty constructor. Use the static instance!
   */
  private EpanechnikovKernelDensityFunction() {
    // Nothing to do.
  }

  /**
   * Static instance.
   */
  public static final EpanechnikovKernelDensityFunction KERNEL = new EpanechnikovKernelDensityFunction();
}
