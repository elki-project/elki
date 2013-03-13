package de.lmu.ifi.dbs.elki.data.projection;

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
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Random projections as suggested by Dimitris Achlioptas.
 * 
 * Reference:
 * <p>
 * Database-friendly random projections: Johnson-Lindenstrauss with binary coins
 * <br />
 * Dimitris Achlioptas<br />
 * In: Proceedings of the twentieth ACM SIGMOD-SIGACT-SIGART symposium on
 * Principles of database systems
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
@Reference(title = "Database-friendly random projections: Johnson-Lindenstrauss with binary coins", authors = "Dimitris Achlioptas", booktitle = "Proceedings of the twentieth ACM SIGMOD-SIGACT-SIGART symposium on Principles of database systems", url = "http://dx.doi.org/10.1145/375551.375608")
public class AchlioptasRandomProjection<V extends NumberVector<?>> implements Projection<V, V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(AchlioptasRandomProjection.class);

  /**
   * Vector factory.
   */
  private NumberVector.Factory<V, ?> factory = null;

  /**
   * Output dimensionality.
   */
  private int dimensionality;

  /**
   * Projection sparsity.
   */
  private double sparsity;

  /**
   * Projection matrix.
   */
  private Matrix projectionMatrix = null;

  /**
   * Random generator.
   */
  private RandomFactory random;

  /**
   * Constructor.
   * 
   * @param dimensionality Desired dimensionality
   * @param sparsity Desired matrix sparsity
   */
  public AchlioptasRandomProjection(int dimensionality, double sparsity, RandomFactory random) {
    super();
    this.dimensionality = dimensionality;
    this.sparsity = sparsity;
    this.random = random;
  }

  @Override
  public void initialize(SimpleTypeInformation<V> in) {
    final VectorFieldTypeInformation<V> vin = (VectorFieldTypeInformation<V>) in;
    factory = (NumberVector.Factory<V, ?>) vin.getFactory();
    int inputdim = vin.getDimensionality();

    final double pPos = .5 / sparsity;
    final double pNeg = pPos + pPos; // Threshold
    double baseValuePart = Math.sqrt(this.sparsity);

    projectionMatrix = new Matrix(dimensionality, inputdim);
    Random rnd = random.getRandom();
    for (int i = 0; i < dimensionality; ++i) {
      for (int j = 0; j < inputdim; ++j) {
        final double r = rnd.nextDouble();
        final double value;
        if (r < pPos) {
          value = baseValuePart;
        } else if (r < pNeg) {
          value = -baseValuePart;
        } else {
          value = 0.;
        }

        projectionMatrix.set(i, j, value);
      }
    }
    if (LOG.isDebugging()) {
      LOG.debug(projectionMatrix.toString());
    }
  }

  @Override
  public V project(V data) {
    // TODO: remove getColumnVector overhead?
    return factory.newNumberVector(projectionMatrix.times(data.getColumnVector()).getArrayRef());
  }

  @Override
  public TypeInformation getInputDataTypeInformation() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public SimpleTypeInformation<V> getOutputDataTypeInformation() {
    return new VectorFieldTypeInformation<>(factory, dimensionality);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for the projection sparsity.
     */
    public static final OptionID SPARSITY_ID = new OptionID("randomproj.sparsity", "Frequency of zeros in the projection matrix.");

    /**
     * Parameter for the desired output dimensionality.
     */
    public static final OptionID DIMENSIONALITY_ID = new OptionID("randomproj.dimensionality", "Amount of dimensions to project to.");

    /**
     * Parameter for the random generator.
     */
    public static final OptionID RANDOM_ID = new OptionID("randomproj.random", "Random generator seed.");

    /**
     * Output dimensionality.
     */
    private int dimensionality;

    /**
     * Projection sparsity
     */
    private double sparsity;

    /**
     * Random generator.
     */
    private RandomFactory random;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter sparsP = new DoubleParameter(SPARSITY_ID);
      sparsP.setDefaultValue(3.);
      sparsP.addConstraint(new GreaterEqualConstraint(1.));
      if (config.grab(sparsP)) {
        sparsity = sparsP.doubleValue();
      }

      IntParameter dimP = new IntParameter(DIMENSIONALITY_ID);
      dimP.addConstraint(new GreaterEqualConstraint(1));
      if (config.grab(dimP)) {
        dimensionality = dimP.intValue();
      }

      RandomParameter rndP = new RandomParameter(RANDOM_ID);
      if (config.grab(rndP)) {
        random = rndP.getValue();
      }
    }

    @Override
    protected AchlioptasRandomProjection<NumberVector<?>> makeInstance() {
      return new AchlioptasRandomProjection<>(dimensionality, sparsity, random);
    }
  }
}
