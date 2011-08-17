package de.lmu.ifi.dbs.elki.visualization.opticsplot;
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

import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderEntry;

/**
 * Class to handle coloring of the OPTICS plot.
 * 
 * @author Erich Schubert
 */
public interface OPTICSColorAdapter {
  /**
   * Get the color value for a particular cluster order entry.
   * 
   * @param coe Cluster order entry
   * @return Color value (rgba integer)
   */
  public int getColorForEntry(ClusterOrderEntry<?> coe);
}
