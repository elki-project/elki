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

import java.awt.Font;
import java.awt.geom.Rectangle2D;

import javax.media.opengl.GL2;

import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Simple menu overlay.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class SimpleMessageOverlay extends AbstractSimpleOverlay {
  /**
   * Text renderer
   */
  TextRenderer renderer;

  /**
   * Message to display.
   */
  private String message = "";

  /**
   * Font size.
   */
  int fontsize;

  /**
   * Constructor.
   */
  public SimpleMessageOverlay() {
    super();
    fontsize = 18;
    renderer = new TextRenderer(new Font(Font.SANS_SERIF, Font.PLAIN, fontsize));
  }

  @Override
  void renderContents(GL2 gl) {
    // Get text bounds.
    Rectangle2D bounds = renderer.getBounds(getMessage());

    // Render message background:
    final float bx1 = .45f * (float) (width - bounds.getWidth());
    final float bx2 = .55f * (float) (width + bounds.getWidth());
    final float by1 = .45f * (float) (height - bounds.getHeight());
    final float by2 = .55f * (float) (height + bounds.getHeight());
    gl.glBegin(GL2.GL_QUADS);
    gl.glColor4f(0f, 0f, 0f, .75f);
    gl.glVertex2f(bx1, by1);
    gl.glVertex2f(bx1, by2);
    gl.glVertex2f(bx2, by2);
    gl.glVertex2f(bx2, by1);
    gl.glEnd();

    // Render message
    renderer.beginRendering(width, height);
    renderer.setColor(1f, 1f, 1f, 1f);
    renderer.setColor(1f, 1f, 1f, 1f);
    renderer.draw(getMessage(), (width - (int) bounds.getWidth()) >> 1, (height - (int) bounds.getHeight()) >> 1);
    renderer.endRendering();
  }

  /**
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * @param message the message to set
   */
  public void setMessage(String message) {
    this.message = message;
  }
}
