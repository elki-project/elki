package de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Random projection family based on selecting random features.
 * 
 * The basic idea of using this for data mining should probably be attributed to
 * L. Breiman, who used it to improve the performance of predictors in an
 * ensemble.
 * 
 * Reference:
 * <p>
 * L. Breiman<br />
 * Bagging predictors<br />
 * Machine learning 24.2
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "L. Breiman", title = "Bagging predictors", booktitle = "Machine learning 24.2", url = "http://dx.doi.org/10.1007/BF00058655")
public class RandomSubsetProjectionFamily extends AbstractRandomProjectionFamily {
  /**
   * Constructor.
   * 
   * @param random Random generator.
   */
  public RandomSubsetProjectionFamily(RandomFactory random) {
    super(random);
  }

  @Override
  public Matrix generateProjectionMatrix(int dim, int odim) {
    Matrix projectionMatrix = new Matrix(odim, dim);
    for (int i = 0; i < odim; i++) {
      projectionMatrix.set(i, random.nextInt(dim), 1.);
    }
    return projectionMatrix;
  }

  @Override
  public Vector generateProjectionVector(int dim) {
    Vector vec = new Vector(dim);
    vec.set(random.nextInt(dim), 1.);
    return vec;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractRandomProjectionFamily.Parameterizer {
    @Override
    protected RandomSubsetProjectionFamily makeInstance() {
      return new RandomSubsetProjectionFamily(random);
    }
  }
}
