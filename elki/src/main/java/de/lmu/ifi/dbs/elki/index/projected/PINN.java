package de.lmu.ifi.dbs.elki.index.projected;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.projection.RandomProjection;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections.AchlioptasRandomProjectionFamily;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Projection-Indexed nearest-neighbors (PINN) is an index to retrieve the
 * nearest neighbors in high dimensional spaces by using a random projection
 * based index.
 * 
 * Reference:
 * <p>
 * Finding local anomalies in very high dimensional space<br />
 * T. de Vries, S. Chawla, M. E. Houle<br />
 * In: Proc. IEEE 10th International Conference on Data Mining (ICDM)
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.composedOf AchlioptasRandomProjectionFamily
 * 
 * @param <O> Object type
 */
@Reference(title = "Finding local anomalies in very high dimensional space", authors = "T. de Vries, S. Chawla, M. E. Houle", booktitle = "Proc. IEEE 10th International Conference on Data Mining (ICDM)", url = "http://dx.doi.org/10.1109/ICDM.2010.151")
public class PINN<O extends NumberVector> extends ProjectedIndex.Factory<O, O> {
  /**
   * Constructor.
   * 
   * @param inner Inner index
   * @param t Target dimensionality
   * @param s Sparsity
   * @param h Neighborhood size multiplicator
   * @param random Random generator factory
   */
  public PINN(IndexFactory<O, ?> inner, int t, double s, double h, RandomFactory random) {
    super(new RandomProjection<O>(t, new AchlioptasRandomProjectionFamily(s, random)), inner, true, false, h);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <O> Outer object type.
   */
  public static class Parameterizer<O extends NumberVector> extends AbstractParameterizer {
    /**
     * Target dimensionality.
     */
    public static final OptionID T_ID = new OptionID("pinn.t", "Target dimensionality.");

    /**
     * Sparsity option.
     */
    public static final OptionID S_ID = new OptionID("pinn.s", "Sparsity of the random projection.");

    /**
     * Neighborhood size.
     */
    public static final OptionID H_ID = new OptionID("pinn.hmult", "Multiplicator for neighborhood size.");

    /**
     * Random generator.
     */
    public static final OptionID RANDOM_ID = new OptionID("pinn.seed", "Random generator seed.");

    /**
     * Inner index factory.
     */
    IndexFactory<O, ?> inner;

    /**
     * Dimensionality.
     */
    int t;

    /**
     * Sparsity.
     */
    double s;

    /**
     * Multiplicator.
     */
    double h;

    /**
     * Random generator.
     */
    RandomFactory random;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<IndexFactory<O, ?>> innerP = new ObjectParameter<>(ProjectedIndex.Factory.Parameterizer.INDEX_ID, IndexFactory.class);
      if(config.grab(innerP)) {
        inner = innerP.instantiateClass(config);
      }

      IntParameter tP = new IntParameter(T_ID);
      tP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(tP)) {
        t = tP.intValue();
      }

      DoubleParameter sP = new DoubleParameter(S_ID, 1.);
      sP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_DOUBLE);
      if(config.grab(sP)) {
        s = sP.doubleValue();
      }

      DoubleParameter hP = new DoubleParameter(H_ID, 3.);
      hP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_DOUBLE);
      if(config.grab(hP)) {
        h = hP.doubleValue();
      }

      RandomParameter randomP = new RandomParameter(RANDOM_ID);
      if(config.grab(randomP)) {
        random = randomP.getValue();
      }
    }

    @Override
    protected PINN<O> makeInstance() {
      return new PINN<>(inner, t, s, h, random);
    }
  }
}
