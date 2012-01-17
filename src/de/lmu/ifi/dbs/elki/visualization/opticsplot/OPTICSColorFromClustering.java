package de.lmu.ifi.dbs.elki.visualization.opticsplot;

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

import java.awt.Color;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Adapter that uses an existing clustering to colorize the OPTICS plot.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses ColorLibrary
 */
public class OPTICSColorFromClustering implements OPTICSColorAdapter {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(OPTICSColorFromClustering.class);
  
  /**
   * The final mapping of object IDs to colors.
   */
  private final HashMap<DBID, Integer> idToColor;

  /**
   * Constructor.
   * 
   * @param colors Color library to use
   * @param refc Clustering to use
   */
  public OPTICSColorFromClustering(ColorLibrary colors, Clustering<?> refc) {
    final List<?> allClusters = refc.getAllClusters();
    // Build a list of colors 
    int[] cols = new int[allClusters.size()];
    for(int i = 0; i < allClusters.size(); i++) {
      Color color = SVGUtil.stringToColor(colors.getColor(i));
      if(color != null) {
        cols[i] = color.getRGB();
      }
      else {
        logger.warning("Could not parse color: " + colors.getColor(i));
        cols[i] = 0x7F7F7F7F;
      }
    }

    idToColor = new HashMap<DBID, Integer>();
    int cnum = 0;
    for(Cluster<?> clus : refc.getAllClusters()) {
      Color color = SVGUtil.stringToColor(colors.getColor(cnum));
      if (color == null) {
        logger.warning("Could not parse color: "+colors.getColor(cnum));
        color = Color.BLACK;
      }
      int rgb = color.getRGB();
      for(DBID id : clus.getIDs()) {
        idToColor.put(id, rgb);
      }
      cnum++;
    }
  }

  @Override
  public int getColorForEntry(ClusterOrderEntry<?> coe) {
    return idToColor.get(coe.getID());
  }
}