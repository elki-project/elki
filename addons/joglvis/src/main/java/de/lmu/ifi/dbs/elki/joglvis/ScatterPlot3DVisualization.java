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
import de.lmu.ifi.dbs.elki.joglvis.scatterplot.ScatterPlotOpenGL2Intel945;
import de.lmu.ifi.dbs.elki.logging.Logging;

public class ScatterPlot3DVisualization implements GLEventListener {
  private static final Logging LOG = Logging.getLogger(ScatterPlot3DVisualization.class);

  private static final boolean DEBUG = false;

  ScatterData data;

  SimpleCamera3D camera = new SimpleCamera3D();

  ScatterPlotOpenGL2Intel945 scatter = new ScatterPlotOpenGL2Intel945();

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
    data.free(gl);
    scatter.free(gl);
  }

  public void start(GLCanvas canvas) {
    // FIXME: Auto-start animator for now.
    final FPSAnimator animator = new FPSAnimator(canvas, 25);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        animator.start();
      }
    });
  }
}
