package de.lmu.ifi.dbs.elki.visualization.visualizers.events;

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

import java.util.EventObject;

import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;

/**
 * Event produced when the visualizer context has changed.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype event
 * 
 * @deprecated Listen for ResultChanged events of the context object!
 */
@Deprecated
public abstract class ContextChangedEvent extends EventObject {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Visualization context changed.
   * 
   * @param source context that has changed
   */
  public ContextChangedEvent(VisualizerContext source) {
    super(source);
  }
}