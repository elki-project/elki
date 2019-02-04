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
package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.awt.event.ActionEvent;

import de.lmu.ifi.dbs.elki.visualization.gui.detail.DetailView;

/**
 * Event when a particular subplot was selected. Plots are currently identified
 * by their coordinates on the screen.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class DetailViewSelectedEvent extends ActionEvent {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Parent overview plot.
   */
  OverviewPlot overview;

  /**
   * Plot item selected
   */
  PlotItem it;

  /**
   * Constructor. To be called by OverviewPlot only!
   * 
   * @param source source plot
   * @param id ID
   * @param command command that was invoked
   * @param modifiers modifiers
   * @param it Plot item selected
   */
  public DetailViewSelectedEvent(OverviewPlot source, int id, String command, int modifiers, PlotItem it) {
    super(source, id, command, modifiers);
    this.overview = source;
    this.it = it;
  }

  /**
   * Retrieve a materialized detail plot.
   * 
   * @return materialized detail plot
   */
  public DetailView makeDetailView() {
    return overview.makeDetailView(it);
  }
}