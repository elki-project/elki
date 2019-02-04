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
package de.lmu.ifi.dbs.elki.visualization.parallel3d.util;

import javax.media.opengl.GL2;

/**
 * Renderer for simple overlays.
 * 
 * TODO: make color configurable?
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public abstract class AbstractSimpleOverlay {
  /**
   * Screen ratio.
   */
  int width = 100;

  /**
   * Screen ratio.
   */
  int height = 100;

  /**
   * Main render method
   * 
   * @param gl GL context
   */
  public final void render(GL2 gl) {
    gl.glMatrixMode(GL2.GL_PROJECTION);
    gl.glPushMatrix();
    gl.glLoadIdentity();
    gl.glMatrixMode(GL2.GL_MODELVIEW);
    gl.glPushMatrix();
    gl.glLoadIdentity();

    gl.glOrtho(0, width, 0, height, -1, +1);
    gl.glColor4f(0f, 0f, 0f, .5f);

    // Fade background:
    gl.glBegin(GL2.GL_QUADS);
    gl.glVertex2f(0f, 0f);
    gl.glVertex2f(width, 0f);
    gl.glVertex2f(width, height);
    gl.glVertex2f(0f, height);
    gl.glEnd();
    
    renderContents(gl);

    gl.glMatrixMode(GL2.GL_PROJECTION);
    gl.glPopMatrix();
    gl.glMatrixMode(GL2.GL_MODELVIEW);
    gl.glPopMatrix();
  }

  /**
   * Render the actual overlay contents.
   * 
   * @param gl GL context
   */
  abstract void renderContents(GL2 gl);

  /**
   * Set screen ratio.
   * 
   * @param width Screen width
   * @param height Screen height
   */
  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
  }
}
