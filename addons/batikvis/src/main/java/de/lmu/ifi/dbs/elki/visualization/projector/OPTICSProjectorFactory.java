package de.lmu.ifi.dbs.elki.visualization.projector;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.ClusterOrder;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;

/**
 * Produce OPTICS plot projections
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has OPTICSProjector
 */
public class OPTICSProjectorFactory implements ProjectorFactory {
  /**
   * Constructor.
   */
  public OPTICSProjectorFactory() {
    super();
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    Collection<ClusterOrder> cos = ResultUtil.filterResults(hier, newResult, ClusterOrder.class);
    for(ClusterOrder co : cos) {
      hier.add(co, new OPTICSProjector(co));
    }
  }
}
