package de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections;

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
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Random hyperplane projection family.
 * 
 * Reference:
 * <p>
 * M.S. Charikar<br />
 * Similarity estimation techniques from rounding algorithms<br />
 * Proc. 34th ACM Symposium on Theory of computing, STOC'02
 * </p>
 * 
 * @author Evgeniy Faerman
 */
@Reference(authors = "M.S. Charikar", //
title = "Similarity estimation techniques from rounding algorithms", //
booktitle = "Proc. 34th ACM Symposium on Theory of computing, STOC'02", //
url = "https://dx.doi.org/10.1145%2F509907.509965")
public class RandomHyperplaneProjectionFamily extends AbstractRandomProjectionFamily {
  /**
   * Constructor.
   * 
   * @param random Random number generator.
   */
  public RandomHyperplaneProjectionFamily(RandomFactory random) {
    super(random);
  }

  @Override
  public Projection generateProjection(int dim, int k) {
    Matrix projectionMatrix = new Matrix(k, dim);
    for(int i = 0; i < k; ++i) {
      for(int j = 0; j < dim; ++j) {
        final double value = random.nextBoolean() ? 1 : -1;
        projectionMatrix.set(i, j, value);
      }
    }
    return new MatrixProjection(projectionMatrix);
  }

  /**
   * Parameterization class.
   * 
   * @author Evgeniy Faerman
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractRandomProjectionFamily.Parameterizer {
    @Override
    protected RandomHyperplaneProjectionFamily makeInstance() {
      return new RandomHyperplaneProjectionFamily(random);
    }
  }
}
