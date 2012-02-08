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

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Adapter that uses a styling policy to colorize the OPTICS plot.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses ColorLibrary
 */
public class OPTICSColorFromStylingPolicy implements OPTICSColorAdapter {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(OPTICSColorFromStylingPolicy.class);

  /**
   * The styling policy
   */
  private StylingPolicy policy;

  /**
   * The colors assigned to the individual objects
   */
  private int[] cols;

  /**
   * Constructor.
   * 
   * @param colors Color library to use
   * @param refc Clustering to use
   */
  public OPTICSColorFromStylingPolicy(ColorLibrary colors, StylingPolicy policy) {
    super();
    this.policy = policy;
    // Build a list of colors
    cols = new int[policy.getMaxStyle() + 1];
    for(int i = 0; i <= policy.getMaxStyle(); i++) {
      Color color = SVGUtil.stringToColor(colors.getColor(i));
      if(color != null) {
        cols[i] = color.getRGB();
      }
      else {
        logger.warning("Could not parse color: " + colors.getColor(i));
        cols[i] = 0x7F7F7F7F;
      }
    }
  }

  @Override
  public int getColorForEntry(ClusterOrderEntry<?> coe) {
    return cols[policy.getStyleForDBID(coe.getID())];
  }
}