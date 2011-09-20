package experimentalcode.students.roedler.parallelCoordinates.projector;

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
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.projector.ProjectorFactory;

/**
 * Produce parallel plot projections.
 * 
 * @author Robert Rödler
 */

public class ParallelPlotFactory implements ProjectorFactory {

  /**
   * Constructor.
   */
  public ParallelPlotFactory() {
    super();
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    Database db = ResultUtil.findDatabase(newResult);
    if(db != null) {
      for(Relation<?> rel : db.getRelations()) {
        if(TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
          @SuppressWarnings("unchecked")
          Relation<NumberVector<?, ?>> vrel = (Relation<NumberVector<?, ?>>) rel;
          ParallelPlotProjector<NumberVector<?, ?>> proj = new ParallelPlotProjector<NumberVector<?, ?>>(vrel);
          baseResult.getHierarchy().add(vrel, proj);
        }
      }
    }
  }
}