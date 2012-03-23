package de.lmu.ifi.dbs.elki.visualization.visualizers;

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

import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Abstract superclass for Visualizers (aka: Visualization Factories).
 * 
 * @author Remigius Wojdanowski
 * 
 * @apiviz.uses ThumbnailVisualization oneway - - «create»
 */
public abstract class AbstractVisFactory implements VisFactory {
  /**
   * Mask for redrawing thumbnails
   */
  protected int thumbmask = 0;

  /**
   * Constructor.
   */
  protected AbstractVisFactory() {
    super();
  }

  @Override
  public Visualization makeVisualizationOrThumbnail(VisualizationTask task) {
    // Is this a thumbnail request?
    Boolean isthumb = task.get(VisualizationTask.THUMBNAIL, Boolean.class);
    if (isthumb != null && isthumb.booleanValue() && allowThumbnails(task)) {
      return new ThumbnailVisualization(this, task, thumbmask);
    }
    return makeVisualization(task);
  }
  
  @Override
  abstract public Visualization makeVisualization(VisualizationTask task);

  /**
   * Test whether to do a thumbnail or a full rendering.
   * 
   * Override this with "false" to disable thumbnails!
   * 
   * @param task Task requested 
   */
  public boolean allowThumbnails(VisualizationTask task) {
    return true;
  }
}