package de.lmu.ifi.dbs.elki.datasource.filter.normalization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;

/**
 * Attribute-wise Normalization using the error function. This mostly makes
 * sense when you have data that has been mean-variance normalized before.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * 
 * @apiviz.uses NumberVector
 */
public class AttributeWiseErfNormalization<O extends NumberVector<O, ?>> extends AbstractNormalization<O> {
  /**
   * Constructor.
   */
  public AttributeWiseErfNormalization() {
    super();
  }

  @Override
  public O restore(O featureVector) throws NonNumericFeaturesException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  protected O filterSingleObject(O obj) {
    double[] val = new double[obj.getDimensionality()];
    for(int i = 0; i < val.length; i++) {
      val[i] = NormalDistribution.erf(obj.doubleValue(i + 1));
    }
    return obj.newNumberVector(val);
  }

  @Override
  protected SimpleTypeInformation<? super O> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }
}
