package experimentalcode.erich;

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
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractStreamConversionFilter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * Add Jitter, preserving the histogram properties (same sum, nonnegative).
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class HistogramJitterFilter<V extends NumberVector<V, N>, N extends Number> extends AbstractStreamConversionFilter<V, V> {
  /**
   * Jitter amount
   */
  double jitter;

  /**
   * Random generator
   */
  Random rnd;

  /**
   * Constructor.
   * 
   * @param jitter Relative amount of jitter to add
   * @param seed Random seed
   */
  public HistogramJitterFilter(double jitter, Long seed) {
    super();
    this.jitter = jitter;
    this.rnd = (seed == null) ? new Random() : new Random(seed);
  }

  @Override
  protected V filterSingleObject(V obj) {
    double[] raw = ArrayLikeUtil.toPrimitiveDoubleArray(obj);
    // Compute the total sum.
    double osum = 0;
    for(int i = 0; i < raw.length; i++) {
      osum += raw[i];
    }
    double nsum = 0;
    for(int i = 0; i < raw.length; i++) {
      double v = rnd.nextDouble() * jitter * osum;
      raw[i] += v;
      nsum += raw[i];
    }
    // Rescale to have the original sum back.
    for(int i = 0; i < raw.length; i++) {
      raw[i] *= osum / nsum;
    }
    return obj.newNumberVector(raw);
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH;
  }

  @Override
  protected SimpleTypeInformation<V> convertedType(SimpleTypeInformation<V> in) {
    return in;
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
     * Option ID for the jitter strength.
     */
    public static final OptionID JITTER_ID = OptionID.getOrCreateOptionID("-jitter.amount", "Jitter amount relative to data.");

    /**
     * Option ID for the jitter random seed.
     */
    public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("-jitter.seed", "Jitter random seed.");

    /**
     * Jitter amount
     */
    double jitter = 0.1;

    /**
     * Random generator seed
     */
    Long seed = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter jitterP = new DoubleParameter(JITTER_ID, new GreaterEqualConstraint(0.0));
      if(config.grab(jitterP)) {
        jitter = jitterP.getValue();
      }
      LongParameter seedP = new LongParameter(SEED_ID, true);
      if(config.grab(seedP)) {
        seed = seedP.getValue();
      }
    }

    @Override
    protected HistogramJitterFilter<DoubleVector, Double> makeInstance() {
      return new HistogramJitterFilter<DoubleVector, Double>(jitter, seed);
    }
  }
}