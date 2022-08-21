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
package elki.svm.model;

public class Model {
  public int nr_class; // number of classes, = 2 in regression/one class svm

  public int l; // total #SV

  // public ArrayList<?> SV; // SVs (SV[l])

  public double[][] sv_coef; // coefficients for SVs in decision functions
  // (sv_coef[k-1][l])

  public double[] rho; // constants in decision functions (rho[k*(k-1)/2])

  public int[] sv_indices; // sv_indices[0,...,nSV-1] are values in
  // [0,...,num_traning_data-1] to indicate SVs in
  // the training set
  
  public double r_square; // For SVDD
};
