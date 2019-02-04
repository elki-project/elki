/**
 * The linear algebra package provides classes and computational methods for
 * operations on matrices and vectors.
 * <p>
 * Some content of this package is adapted from the Jama package.
 * <p>
 * Five fundamental matrix decompositions, which consist of pairs or triples
 * of matrices, permutation vectors, and the like, produce results in five
 * decomposition classes. These decompositions are accessed by the Matrix
 * class to compute solutions of simultaneous linear equations, determinants,
 * inverses and other matrix functions. The five decompositions are:
 * <ul>
 * <li>Cholesky Decomposition of symmetric, positive definite matrices.</li>
 * <li>LU Decomposition of rectangular matrices.</li>
 * <li>QR Decomposition of rectangular matrices.</li>
 * <li>Singular Value Decomposition of rectangular matrices.</li>
 * <li>Eigenvalue Decomposition of both symmetric and nonsymmetric square
 * matrices.</li>
 * </ul>
 * <b>Example of use:</b>
 * <p>
 * Solve a linear system \(Ax=b\) and compute the residual norm, \(||b-Ax||\).
 * <p>
 * {@code double[][] matrix = { {1.,2.,3}, {4.,5.,6.}, {7.,8.,10.} };}<br>
 * {@code double[] b = MathUtil.randomDoubleArray(3, new Random());}<br>
 * {@code double[] x = VMath.solve(matrix, b);}<br>
 * {@code double[] r = VMath.minusEquals(VMath.times(matrix, x), b);}<br>
 * {@code double norm = VMath.euclideanLength(r);}
 * <p>
 * The original Jama-package has been developed by
 * the <a href="http://www.mathworks.com/">MathWorks</a> and
 * <a href="http://www.nist.gov/">NIST</a>
 * and can be found at
 * <a href="http://math.nist.gov/javanumerics/jama/">math.nist.gov</a>.
 * <p>
 * Here, for the adaption some classes and methods convenient for data mining
 * applications within ELKI were added.
 * Furthermore some erroneous comments were corrected and the coding-style was
 * subtly changed to a more Java-typical style.
 *
 * @opt hide ^java\.(io|lang)\..*
 */
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
package de.lmu.ifi.dbs.elki.math.linearalgebra;
