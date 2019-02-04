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
package de.lmu.ifi.dbs.elki.algorithm.statistics;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.ScalesResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;

/**
 * Pseudo "algorithm" that computes the global min/max for a relation across all
 * attributes.
 *
 * FIXME: this should become part of relation metadata.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Description("Setup a scaling so that all dimensions have the same value range.")
public class AddUniformScale implements Algorithm {
  /**
   * Constructor.
   */
  public AddUniformScale() {
    super();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Result run(Database database) {
    for(Relation<?> rel : database.getRelations()) {
      if(TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        ScalesResult res = run((Relation<? extends NumberVector>) rel);
        ResultUtil.addChildResult(rel, res);
      }
    }
    return null;
  }

  /**
   * Add scales to a single vector relation.
   *
   * @param rel Relation
   * @return Scales
   */
  private ScalesResult run(Relation<? extends NumberVector> rel) {
    double[][] mms = RelationUtil.computeMinMax(rel);
    int dim = mms[0].length;
    double delta = 0.;
    for(int d = 0; d < dim; d++) {
      double del = mms[1][d] - mms[0][d];
      delta = del > delta ? del : delta;
    }
    if(delta < Double.MIN_NORMAL) {
      delta = 1.;
    }
    int log10res = (int) Math.ceil(Math.log10(delta / (LinearScale.MAXTICKS - 1)));
    double res = MathUtil.powi(10, log10res);
    double target = Math.ceil(delta / res) * res; // Target width
    LinearScale[] scales = new LinearScale[dim];
    for(int d = 0; d < dim; d++) {
      double mid = (mms[0][d] + mms[1][d] - target) * .5;
      double min = Math.floor(mid / res) * res;
      double max = Math.ceil((mid + target) / res) * res;
      scales[d] = new LinearScale(min, max);
    }
    return new ScalesResult(scales);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }
}
