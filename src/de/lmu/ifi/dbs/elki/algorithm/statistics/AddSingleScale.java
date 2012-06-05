package de.lmu.ifi.dbs.elki.algorithm.statistics;

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
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.ScalesResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;

/**
 * Pseudo "algorith" that computes the global min/max for a relation across all
 * attributes.
 * 
 * @author Erich Schubert
 */
@Description("Setup a scaling so that all dimensions are scaled equally in visualization.")
public class AddSingleScale implements Algorithm {
  /**
   * Constructor.
   */
  public AddSingleScale() {
    super();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Result run(Database database) {
    for(Relation<?> rel : database.getRelations()) {
      if(TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        ScalesResult res = run((Relation<? extends NumberVector<?, ?>>) rel);
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
  private ScalesResult run(Relation<? extends NumberVector<?, ?>> rel) {
    final int dim = DatabaseUtil.dimensionality(rel);
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBID id : rel.iterDBIDs()) {
      NumberVector<?, ?> vec = rel.get(id);
      for(int d = 1; d <= dim; d++) {
        minmax.put(vec.doubleValue(d));
      }
    }
    LinearScale scale = new LinearScale(minmax.getMin(), minmax.getMax());
    LinearScale[] scales = new LinearScale[dim];
    for(int i = 0; i < dim; i++) {
      scales[i] = scale;
    }
    ScalesResult res = new ScalesResult(scales);
    return res;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }
}