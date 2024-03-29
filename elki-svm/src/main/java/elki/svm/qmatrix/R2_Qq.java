/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.svm.qmatrix;

import elki.svm.data.DataSet;

/**
 * Q matrix used by SVDD variant R^2 L2SVM
 */
public class R2_Qq extends Kernel {
  /**
   * Inverse regularization parameter
   */
  final double invC;

  /**
   * Constructor.
   * 
   * @param x Data set
   * @param C Regularization term, 1/C is added to the diagonal
   */
  public R2_Qq(DataSet x, double C) {
    super(x);
    this.invC = 1. / C;
  }

  @Override
  public double similarity(int i, int j) {
    return i != j ? x.similarity(i, j) : (x.similarity(i, j) + invC);
  }
}
