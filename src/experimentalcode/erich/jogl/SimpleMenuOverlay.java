package experimentalcode.erich.jogl;

import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.media.opengl.GL2;
import javax.swing.event.MouseInputAdapter;

import com.jogamp.opengl.util.awt.TextRenderer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

/**
 * Simple menu overlay.
 * 
 * @author Erich Schubert
 */
public class SimpleMenuOverlay extends MouseInputAdapter {
  /**
   * Screen ratio.
   */
  int width = 100, height = 100;

  /**
   * Text renderer
   */
  TextRenderer renderer;

  /**
   * Options to display.
   */
  ArrayList<String> options = new ArrayList<>();

  /**
   * Font size.
   */
  int fontsize;

  /**
   * Constructor.
   */
  public SimpleMenuOverlay() {
    super();
    fontsize = 28;
    renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, fontsize));
    options.add("Example");
    options.add("acenop");
    options.add("qjFWXM");
  }

  public void render(GL2 gl) {
    gl.glMatrixMode(GL2.GL_PROJECTION);
    gl.glPushMatrix();
    gl.glLoadIdentity();
    gl.glMatrixMode(GL2.GL_MODELVIEW);

    gl.glOrtho(0, width, 0, height, -1, +1);
    gl.glColor4f(0f, 0f, 0f, .5f);

    // Fade background:
    gl.glBegin(GL2.GL_QUADS);
    gl.glVertex2f(0f, 0f);
    gl.glVertex2f(width, 0f);
    gl.glVertex2f(width, height);
    gl.glVertex2f(0f, height);
    gl.glEnd();

    final int numopt = options.size();

    double maxwidth = 0.;
    Rectangle2D[] bounds = new Rectangle2D[numopt];
    for (int i = 0; i < numopt; i++) {
      bounds[i] = renderer.getBounds(options.get(i));
      maxwidth = Math.max(bounds[i].getWidth(), maxwidth);
    }
    final double padding = .3 * fontsize;
    final double margin = padding * .3;
    final float bx1 = (float)(.5 * (width - maxwidth - margin));
    final float bx2 = (float)(.5 * (width + maxwidth + margin));
    double totalheight = numopt * fontsize + (numopt - 1) * padding;

    // Render background buttons:
    gl.glBegin(GL2.GL_QUADS);
    gl.glColor4f(0f, 0f, 0f, .5f);
    for (int i = 0; i < numopt; i++) {
      final double pos = (height - totalheight) * .5 + fontsize * i + padding * i;

      // Render a background button:
      gl.glVertex2f(bx1, (float)(pos - margin));
      gl.glVertex2f(bx1, (float)(pos + fontsize + margin));
      gl.glVertex2f(bx2, (float)(pos + fontsize + margin));
      gl.glVertex2f(bx2, (float)(pos - margin));
    }
    gl.glEnd();

    // Render text labels:
    renderer.beginRendering(width, height);
    renderer.setColor(1f, 1f, 1f, 1f);
    // NOTE: renderer uses (0,0) as BOTTOM left!
    for (int j = 0; j < numopt; j++) {
      final int i = numopt - j - 1;
      // Extra offset .17 * fontsize because of text baseline!
      final double pos = (height - totalheight) * .5 + fontsize * i + padding * i + .17 * fontsize;
      renderer.setColor(1f, 1f, 1f, 1f);
      renderer.draw(options.get(j), (width - (int) bounds[j].getWidth()) >> 1, (int) pos);
    }
    renderer.endRendering();

    gl.glMatrixMode(GL2.GL_PROJECTION);
    gl.glPopMatrix();
    gl.glMatrixMode(GL2.GL_MODELVIEW);
    gl.glPopMatrix();
  }

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
