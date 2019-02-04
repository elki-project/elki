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
package de.lmu.ifi.dbs.elki.joglvis.scatterplot;

import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;

import de.lmu.ifi.dbs.elki.joglvis.SimpleCamera3D;

/**
 * OpenGL Scatterplot interface.
 * <p>
 * Necessary to allow using drivers with different OpenGL levels.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public interface ScatterPlot {
  void initializeShaders(GL2 gl);

  void initializeTextures(GL2 gl, GLProfile profile);

  void enableProgram(GL2 gl);

  void setCamera(SimpleCamera3D camera);

  void free(GL2 gl);
}
