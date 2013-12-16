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

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Abstract base class for random projection families.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractRandomProjectionFamily implements RandomProjectionFamily {
  /**
   * Random generator.
   */
  protected Random random;

  /**
   * Constructor.
   */
  public AbstractRandomProjectionFamily(RandomFactory random) {
    super();
    this.random = random.getSingleThreadedRandom();
  }

  /**
   * Parameterization interface (with the shared
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for the random generator.
     */
    public static final OptionID RANDOM_ID = new OptionID("randomproj.random", "Random generator seed.");

    /**
     * Random generator.
     */
    protected RandomFactory random;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      RandomParameter rndP = new RandomParameter(RANDOM_ID);
      if (config.grab(rndP)) {
        random = rndP.getValue();
      }
    }
  }

  /**
   * Class to project using a matrix multiplication.
   * 
   * @author Erich Schubert
   */
  public static class MatrixProjection implements Projection {
    /**
     * Projection matrix.
     */
    Matrix matrix;

    /**
     * Projection buffer.
     */
    Vector buf;

    /**
     * Projection buffer values.
     */
    double[] vals;
    
    /**
     * Constructor.
     *
     * @param matrix Projection matrix.
     */
    public MatrixProjection(Matrix matrix) {
      super();
      this.matrix = matrix;
      this.buf = new Vector(matrix.getColumnDimensionality());
      this.vals = buf.getArrayRef();
    }

    @Override
    public double[] project(NumberVector<?> in) {
      for (int d = 0; d < vals.length; d++) {
        vals[d] = in.doubleValue(d);
      }
      return VectorUtil.fastTimes(matrix, buf);
    }

    @Override
    public int getOutputDimensionality() {
      return matrix.getRowDimensionality();
    }
  }
}
