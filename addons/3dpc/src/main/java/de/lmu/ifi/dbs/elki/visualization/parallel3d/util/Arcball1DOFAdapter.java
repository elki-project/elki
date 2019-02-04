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

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.swing.event.MouseInputAdapter;

import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import net.jafama.FastMath;

/**
 * Arcball style helper.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @assoc - - - Simple1DOFCamera
 */
public class Arcball1DOFAdapter extends MouseInputAdapter {
  /**
   * Debug flag.
   */
  private static final boolean DEBUG = false;

  /**
   * The true camera.
   */
  private final Simple1DOFCamera camera;

  /**
   * Starting point of drag.
   */
  private double[] startvec = new double[3];

  /**
   * Ending point of drag.
   */
  private double[] endvec = new double[3];

  /**
   * Temp buffer we use for computations.
   */
  private double[] near = new double[3], far = new double[3];

  /**
   * Starting angle for dragging.
   */
  double startangle;

  /**
   * Camera that was in use when the drag started.
   */
  private Simple1DOFCamera startcamera;

  /**
   * Constructor.
   * 
   * @param camera Scene camera
   */
  public Arcball1DOFAdapter(Simple1DOFCamera camera) {
    super();
    this.camera = camera;
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    int s = e.getWheelRotation();
    double distance = camera.getDistance();
    for (; s >= 1; s--) {
      distance += .1;
    }
    for (; s <= -1; s++) {
      if (distance > .15) {
        distance -= .1;
      }
    }
    camera.setDistance(distance);
  }

  @Override
  public void mousePressed(MouseEvent e) {
    // Start drag.
    startcamera = new Simple1DOFCamera(camera);

    Point startPoint = e.getPoint();
    mapMouseToPlane(startcamera, startPoint, startvec);
    startangle = FastMath.atan2(startvec[1], startvec[0]);
  }

  /**
   * Map the coordinates. Note: vec will be overwritten!
   * 
   * @param camera Camera
   * @param point2d Input point
   * @param vec Output vector
   */
  private void mapMouseToPlane(Simple1DOFCamera camera, Point point2d, double[] vec) {
    // Far plane
    camera.unproject(point2d.x, point2d.y, -100., far);
    // Near plane
    camera.unproject(point2d.x, point2d.y, 1., near);
    // Delta vector: far -= near.
    VMath.minusEquals(far, near);
    // Intersection with z=0 plane:
    // far.z - a * near.z = 0 -> a = far.z / near.z
    if (near[2] < 0 || near[2] > 0) {
      double a = far[2] / near[2];
      vec[0] = far[0] - a * near[0];
      vec[1] = far[1] - a * near[1];
      vec[2] = 0;
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    mapMouseToPlane(startcamera, e.getPoint(), endvec);
    double upangle = FastMath.atan2(endvec[1], endvec[0]);
    camera.setRotationZ(startcamera.getRotationZ() + (upangle - startangle));
    // TODO: add full arcball support?
  }

  /**
   * Render a debugging hint for the arcball tool.
   * 
   * @param gl GL class for rendering-
   */
  @SuppressWarnings("unused")
  public void debugRender(GL2 gl) {
    if (!DEBUG || (startcamera == null)) {
      return;
    }
    gl.glLineWidth(3f);
    gl.glColor4f(1.f, 0.f, 0.f, .66f);
    gl.glBegin(GL.GL_LINES);
    gl.glVertex3f(0.f, 0.f, 0.f);
    double rot = startangle - startcamera.getRotationZ();
    gl.glVertex3f((float) FastMath.cos(rot) * 4.f, (float) -FastMath.sin(rot) * 4.f, 0.f);
    gl.glVertex3f((float) FastMath.cos(rot) * 1.f, (float) -FastMath.sin(rot) * 1.f, 0.f);
    gl.glVertex3f((float) FastMath.cos(rot) * 1.f, (float) -FastMath.sin(rot) * 1.f, 1.f);
    gl.glEnd();
  }
}