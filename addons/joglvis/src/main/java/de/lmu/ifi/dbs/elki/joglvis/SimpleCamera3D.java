package de.lmu.ifi.dbs.elki.joglvis;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

/**
 * Very simple (rotating) camera.
 * 
 * @author Erich Schubert
 */
public class SimpleCamera3D {
  /**
   * Viewing angle.
   */
  private double theta;

  /**
   * Screen (window) size.
   */
  private int width, height;

  /**
   * GLU utility class.
   */
  private GLU glu = new GLU();

  /**
   * Last camera position.
   */
  private float[] eye = new float[3];

  /**
   * Animate the camera.
   */
  public void simpleAnimate() {
    // Simple rotation
    theta += 0.0025;
  }

  /**
   * Apply the camera settings.
   * 
   * @param gl GL API.
   */
  public void applyCamera(GL2 gl) {
    // Setup projection.
    gl.glMatrixMode(GL2.GL_PROJECTION);
    gl.glLoadIdentity();
    glu.gluPerspective(45f, // fov,
        width / (float) height, // ratio
        0.f, 10.f); // near, far clipping
    eye[0] = (float) Math.sin(theta) * 2.f;
    eye[1] = .5f;
    eye[2] = (float) Math.cos(theta) * 2.f;
    glu.gluLookAt(eye[0], eye[1], eye[2], // eye
        .0f, .0f, 0.f, // center
        0.f, 1.f, 0.f); // up

    gl.glMatrixMode(GL2.GL_MODELVIEW);
    gl.glLoadIdentity();

    gl.glViewport(0, 0, width, height);
  }

  /**
   * Get screen width.
   * 
   * @return Screen width
   */
  public int getWidth() {
    return width;
  }

  /**
   * Get screen height.
   * 
   * @return Screen height
   */
  public int getHeight() {
    return height;
  }

  /**
   * Current eye position.
   * 
   * @return Eye position
   */
  public float[] getEyePosition() {
    return eye;
  }

  /**
   * Update the window size.
   * 
   * @param width Window width
   * @param height Window height
   */
  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
  }
}