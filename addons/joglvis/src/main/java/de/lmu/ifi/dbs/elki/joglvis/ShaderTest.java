package de.lmu.ifi.dbs.elki.joglvis;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;

import com.jogamp.opengl.util.FPSAnimator;

import de.lmu.ifi.dbs.elki.joglvis.scatterplot.ScatterData;
import de.lmu.ifi.dbs.elki.joglvis.scatterplot.ScatterPlotOpenGL2Intel945;

public class ShaderTest implements GLEventListener {
  private static final boolean DEBUG = false;

  ScatterData data = new ScatterData();

  SimpleCamera3D camera = new SimpleCamera3D();

  ScatterPlotOpenGL2Intel945 scatter = new ScatterPlotOpenGL2Intel945();

  @Override
  public void init(GLAutoDrawable glautodrawable) {
    GL2 gl = glautodrawable.getGL().getGL2();
    if(DEBUG) {
      gl = new DebugGL2(gl);
    }
    scatter.initializeTextures(gl, glautodrawable.getGLProfile());
    scatter.initializeShaders(gl);
    scatter.setCamera(camera);
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

  public static void main(String[] args) {
    GLProfile glprofile = GLProfile.getDefault();
    GLCapabilities glcapabilities = new GLCapabilities(glprofile);
    final GLCanvas glcanvas = new GLCanvas(glcapabilities);

    glcanvas.addGLEventListener(new ShaderTest());

    final JFrame jframe = new JFrame("OpenGL Scatterplot");
    jframe.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent windowevent) {
        jframe.dispose();
        System.exit(0);
      }
    });

    jframe.getContentPane().add(glcanvas, BorderLayout.CENTER);
    jframe.setSize(640, 480);
    jframe.setVisible(true);

    final FPSAnimator animator = new FPSAnimator(glcanvas, 25);
    animator.start();
  }
}
