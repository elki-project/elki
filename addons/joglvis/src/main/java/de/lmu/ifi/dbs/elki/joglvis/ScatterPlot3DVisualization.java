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
package de.lmu.ifi.dbs.elki.joglvis;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.util.FPSAnimator;

import de.lmu.ifi.dbs.elki.joglvis.scatterplot.ScatterData;
import de.lmu.ifi.dbs.elki.joglvis.scatterplot.ScatterPlot;
import de.lmu.ifi.dbs.elki.joglvis.scatterplot.opengl2intel945.ScatterPlotOpenGL2Intel945;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * 3D scatter plot using OpenGL shaders for efficiency.
 * <p>
 * Not very portable yet to older intel graphics.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ScatterPlot3DVisualization implements GLEventListener {
  private static final Logging LOG = Logging.getLogger(ScatterPlot3DVisualization.class);

  private static final boolean DEBUG = true;

  ScatterData data;

  SimpleCamera3D camera = new SimpleCamera3D();

  ScatterPlot scatter = new ScatterPlotOpenGL2Intel945();

  FPSAnimator animator = null;

  public ScatterPlot3DVisualization(ScatterData data) {
    this.data = data;
  }

  @Override
  public void init(GLAutoDrawable glautodrawable) {
    GL2 gl = glautodrawable.getGL().getGL2();
    if(DEBUG) {
      gl = new DebugGL2(gl);
    }
    scatter.initializeTextures(gl, glautodrawable.getGLProfile());
    scatter.initializeShaders(gl);
    scatter.setCamera(camera);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Loading data into video memory.");
    }
    data.initializeData(gl);
  }

  @Override
  public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width, int height) {
    camera.setSize(width, height);
  }

  @Override
  public void display(GLAutoDrawable glautodrawable) {
    GL2 gl = glautodrawable.getGL().getGL2();
    if(DEBUG) {
      gl = new DebugGL2(gl);
    }
    camera.simpleAnimate();
    camera.applyCamera(gl);

    // int width = glautodrawable.getWidth();
    gl.glClearColor(1.f, 1.f, 1.f, 1.f);
    gl.glClear(GL.GL_COLOR_BUFFER_BIT);

    // setup vbo, tbo's, etc.
    gl.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
    gl.glEnable(GL2.GL_POINT_SPRITE);
    gl.glTexEnvf(GL2.GL_POINT_SPRITE, GL2.GL_COORD_REPLACE, GL.GL_TRUE);
    // This has a surprisingly large performance impact on i945:
    // gl2.glPointParameteri(GL2.GL_POINT_SPRITE_COORD_ORIGIN,
    // GL2.GL_LOWER_LEFT);
    gl.glDisable(GL2.GL_ALPHA_TEST);
    gl.glEnable(GL.GL_BLEND);
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
    gl.glDepthMask(false);
    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, data.getBufferID());
    gl.glClientActiveTexture(GL.GL_TEXTURE0);
    gl.glTexCoordPointer(2, GL.GL_FLOAT, data.stride, data.getOffsetX());
    gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    gl.glClientActiveTexture(GL.GL_TEXTURE1);
    gl.glTexCoordPointer(2, GL.GL_FLOAT, data.stride, data.getOffsetY());
    gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    gl.glClientActiveTexture(GL.GL_TEXTURE2);
    gl.glTexCoordPointer(2, GL.GL_FLOAT, data.stride, data.getOffsetZ());
    gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    gl.glClientActiveTexture(GL.GL_TEXTURE0);
    gl.glVertexPointer(3, GL.GL_FLOAT, data.stride, data.getOffsetShapeNum());
    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
    gl.glNormalPointer(GL.GL_FLOAT, data.stride, data.getOffsetColorNum());
    gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);

    scatter.enableProgram(gl);

    // draw all active particles
    gl.glDrawArrays(GL.GL_POINTS, 0, data.size());

    // clean up
    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    gl.glDisable(GL.GL_BLEND);
    gl.glDisable(GL2.GL_POINT_SPRITE);
    gl.glDisable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    gl.glUseProgram(0);
  }

  @Override
  public void dispose(GLAutoDrawable glautodrawable) {
    GL2 gl = glautodrawable.getGL().getGL2();
    if(DEBUG) {
      gl = new DebugGL2(gl);
    }
    if(animator != null) {
      animator.stop();
    }
    data.free(gl);
    scatter.free(gl);
  }

  public void start(GLCanvas canvas) {
    animator = new FPSAnimator(canvas, 25);
    // FIXME: Auto-start animator for now.
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        animator.start();
      }
    });
  }
}
