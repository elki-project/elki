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

import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.glu.GLU;

import net.jafama.FastMath;

/**
 * Class for a simple camera. Restricted: always looks at 0,0,0 from a position
 * defined by rotationX, distance and height.
 * <p>
 * For rotationX = 0, the camera will be at y=distance, x=0, so that the default
 * view will have the usual X/Y plane on the ground.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @assoc - - - CameraListener
 */
public class Simple1DOFCamera {
  /**
   * Rotation on X axis.
   */
  private double rotationZ = 0.;

  /**
   * Distance
   */
  private double distance = 3;

  /**
   * Height
   */
  private double height = 1.5;

  /**
   * Screen ratio
   */
  private double ratio = 1.0;

  /**
   * GLU viewport storage
   */
  private int[] viewp = new int[4];

  /**
   * GLU model view matrix
   */
  private double[] modelview = new double[16];

  /**
   * GLU projection matrix
   */
  private double[] projection = new double[16];

  /**
   * GLU utility
   */
  private GLU glu;

  /**
   * Cache the Z rotation cosine
   */
  private double cosZ;

  /**
   * Cache the Z rotation sine
   */
  private double sinZ;

  /**
   * Camera listener list.
   */
  ArrayList<CameraListener> listeners;

  /**
   * Constructor.
   * 
   * @param glu GLU utility class
   */
  public Simple1DOFCamera(GLU glu) {
    super();
    this.glu = glu;
    viewp = new int[4];
    modelview = new double[16];
    projection = new double[16];
    // Initial angle:
    rotationZ = 0;
    cosZ = 1.0;
    sinZ = 0.0;
    listeners = new ArrayList<>(5);
  }

  /**
   * Copy constructor, for freezing a camera position.
   * 
   * Note: listeners will not be copied.
   * 
   * @param other Existing camera
   */
  public Simple1DOFCamera(Simple1DOFCamera other) {
    super();
    this.rotationZ = other.rotationZ;
    this.distance = other.distance;
    this.height = other.height;
    this.ratio = other.ratio;
    this.viewp = other.viewp.clone();
    this.modelview = other.modelview.clone();
    this.projection = other.projection.clone();
    this.glu = other.glu;
    this.listeners = null; // Do NOT copy listeners
  }

  /**
   * Get the distance
   * 
   * @return Distance
   */
  public double getDistance() {
    return distance;
  }

  /**
   * Set camera distance
   * 
   * @param distance Distance
   */
  public void setDistance(double distance) {
    this.distance = distance;

    fireCameraChangedEvent();
  }

  /**
   * Get camera height
   * 
   * @return Camera height
   */
  public double getHeight() {
    return height;
  }

  /**
   * Set camera height
   * 
   * @param height Camera height
   */
  public void setHeight(double height) {
    this.height = height;

    fireCameraChangedEvent();
  }

  /**
   * Get screen ratio.
   * 
   * @return Screen ratio
   */
  public double getRatio() {
    return ratio;
  }

  /**
   * Set screen ratio.
   * 
   * @param ratio Screen ratio
   */
  public void setRatio(double ratio) {
    this.ratio = ratio;

    // As this will be triggered by the canvas only,
    // Do not fire an event.
  }

  /**
   * Get the Z rotation in radians.
   * 
   * @return Z rotation angle (radians)
   */
  public double getRotationZ() {
    return rotationZ;
  }

  /**
   * Set the z rotation angle in radians.
   * 
   * @param rotationZ Z rotation angle.
   */
  public void setRotationZ(double rotationZ) {
    this.rotationZ = rotationZ;
    this.cosZ = FastMath.cos(rotationZ);
    this.sinZ = FastMath.sin(rotationZ);

    fireCameraChangedEvent();
  }

  /**
   * Apply the camera to a GL context.
   * 
   * @param gl GL context.
   */
  public void apply(GL2 gl) {
    // 3D projection
    gl.glMatrixMode(GL2.GL_PROJECTION);
    gl.glLoadIdentity();
    // Perspective.
    glu.gluPerspective(35, ratio, 1, 1000);
    glu.gluLookAt(distance * sinZ, distance * -cosZ, height, // pos
        0, 0, .5, // center
        0, 0, 1 // up
    );
    // Change back to model view matrix.
    gl.glMatrixMode(GL2.GL_MODELVIEW);
    gl.glLoadIdentity();

    // Store the matrixes for reference.
    gl.glGetIntegerv(GL.GL_VIEWPORT, viewp, 0);
    gl.glGetDoublev(GLMatrixFunc.GL_MODELVIEW_MATRIX, modelview, 0);
    gl.glGetDoublev(GLMatrixFunc.GL_PROJECTION_MATRIX, projection, 0);
  }

  /**
   * Unproject a screen coordinate (at depth 0) to 3D model coordinates.
   * 
   * @param x X
   * @param y Y
   * @param z Z
   * @return model coordinates
   */
  public double[] unproject(double x, double y, double z) {
    double[] out = new double[3];
    unproject(x, y, z, out);
    return out;
  }

  /**
   * Unproject a screen coordinate (at depth 0) to 3D model coordinates.
   * 
   * @param x X
   * @param y Y
   * @param z Z
   * @param out output buffer
   */
  public void unproject(double x, double y, double z, double[] out) {
    glu.gluUnProject(x, y, z, modelview, 0, projection, 0, viewp, 0, out, 0);
  }

  /**
   * Project a coordinate
   * 
   * @param x X
   * @param y Y
   * @param z Z
   * @param out output buffer
   */
  public void project(double x, double y, double z, double[] out) {
    glu.gluProject(x, y, z, modelview, 0, projection, 0, viewp, 0, out, 0);
  }

  /**
   * Distance from camera
   * 
   * @param x X position
   * @param y Y position
   * @return Squared distance
   */
  public double squaredDistanceFromCamera(double x, double y) {
    double dx = (distance * sinZ) - x;
    double dy = (distance * -cosZ) - y;
    return dx * dx + dy * dy;
  }

  /**
   * Distance from camera
   * 
   * @param x X position
   * @param y Y position
   * @param z Z position
   * @return Squared distance
   */
  public double squaredDistanceFromCamera(double x, double y, double z) {
    double dx = (distance * sinZ) - x;
    double dy = (distance * -cosZ) - y;
    double dz = height - z;
    return dx * dx + dy * dy + dz * dz;
  }

  /**
   * Add a camera listener.
   * 
   * @param lis Listener
   */
  public void addCameraListener(CameraListener lis) {
    if (listeners == null) {
      listeners = new ArrayList<>(5);
    }
    listeners.add(lis);
  }

  /**
   * Remove a camera listener.
   * 
   * @param lis Listener
   */
  public void removeCameraListener(CameraListener lis) {
    if (listeners == null) {
      return;
    }
    listeners.remove(lis);
  }

  /**
   * Fire the camera changed event.
   */
  protected void fireCameraChangedEvent() {
    if (listeners != null) {
      for (CameraListener list : listeners) {
        list.cameraChanged();
      }
    }
  }

  /**
   * Camera Listener class
   * 
   * @author Erich Schubert
   */
  @FunctionalInterface
  public interface CameraListener {
    /**
     * Camera changed.
     */
    void cameraChanged();
  }
}
