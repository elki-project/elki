package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;
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

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;

/**
 * Interface for scaling functions used by Outlier evaluation such as Histograms
 * and visualization. Make sure to invoke {@link #prepare} prior to applying the
 * scaling function.
 * 
 * @author Erich Schubert
 */
public interface OutlierScalingFunction extends ScalingFunction {
  /**
   * Prepare is called once for each data set, before getScaled() will be
   * called. This function can be used to extract global parameters such as
   * means, minimums or maximums from the Database, Result or Annotation.
   * 
   * @param ids Database IDs to process
   * @param or Outlier result to use
   */
  public void prepare(DBIDs ids, OutlierResult or);
}