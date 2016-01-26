package de.lmu.ifi.dbs.elki.datasource.filter.normalization.columnwise;

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
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.AbstractNormalization;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.Alias;

/**
 * Attribute-wise Normalization using the error function. This mostly makes
 * sense when you have data that has been mean-variance normalized before.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <V> Object type
 * 
 * @apiviz.uses NumberVector
 */
@Alias({ "de.lmu.ifi.dbs.elki.datasource.filter.normalization.AttributeWiseErfNormalization", //
"de.lmu.ifi.dbs.elki.datasource.filter.AttributeWiseErfNormalization" })
public class AttributeWiseErfNormalization<V extends NumberVector> extends AbstractNormalization<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(AttributeWiseErfNormalization.class);

  /**
   * Constructor.
   */
  public AttributeWiseErfNormalization() {
    super();
  }

  @Override
  protected V filterSingleObject(V obj) {
    double[] val = new double[obj.getDimensionality()];
    for(int i = 0; i < val.length; i++) {
      val[i] = NormalDistribution.erf(obj.doubleValue(i));
    }
    return factory.newNumberVector(val);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }
}
