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
package de.lmu.ifi.dbs.elki.joglvis.scatterplot.opengl4;

import java.io.IOException;
import java.io.InputStream;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;

import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import de.lmu.ifi.dbs.elki.joglvis.ShaderUtil;
import de.lmu.ifi.dbs.elki.joglvis.SimpleCamera3D;
import de.lmu.ifi.dbs.elki.joglvis.scatterplot.ScatterPlot;

/**
 * OpenGL4 version of the scatterplot renderer.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ScatterPlotOpenGL4 implements ScatterPlot {
  private int[] textures = new int[2];

  private int numcolors;

  private int st_prog = 0;

  private int texgrid = 4; // 4*4 textures

  private SimpleCamera3D camera;

  private int[] shaders;

  @Override
  public void initializeShaders(GL2 gl) {
    try {
      shaders = new int[2];
      shaders[0] = ShaderUtil.compileShader(this.getClass(), gl, GL2.GL_VERTEX_SHADER, "st_vertex_points.shader");
      shaders[1] = ShaderUtil.compileShader(this.getClass(), gl, GL2.GL_FRAGMENT_SHADER, "st_fragment_points.shader");
      st_prog = ShaderUtil.linkShaderProgram(gl, shaders);
    }
    catch(ShaderUtil.ShaderCompilationException e) {
      throw new RuntimeException("Shader compilation failed.", e);
    }
  }

  @Override
  public void initializeTextures(GL2 gl, GLProfile profile) {
    // Initialize textures
    gl.glGenTextures(2, textures, 0);
    {
      gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[0]);
      gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);

      for(int tsize = 64, lev = 0; tsize >= 4; tsize >>>= 1, lev++) {
        final String filename = "markers/markers-" + tsize + ".png";
        TextureData data = loadTexture(gl, profile, filename);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, lev, data.getInternalFormat(), data.getWidth(), data.getHeight(), 0, data.getPixelFormat(), GL2.GL_UNSIGNED_BYTE, data.getBuffer());
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LEVEL, lev);
      }
      gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
      gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
      gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_NEAREST);
      gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);

      gl.glBindTexture(GL2.GL_TEXTURE_2D, 0); // unbind
    }
    {
      gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[1]);
      final String filename = "markers/colorpalette14.png";
      TextureData data = loadTexture(gl, profile, filename);
      numcolors = data.getWidth();
      gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, data.getInternalFormat(), data.getWidth(), data.getHeight(), 0, data.getPixelFormat(), GL2.GL_UNSIGNED_BYTE, data.getBuffer());
      gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
      gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
      gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
      gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
      gl.glBindTexture(GL2.GL_TEXTURE_2D, 0); // unbind
    }

    int[] maxTextureUnits = new int[1];
    gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_UNITS, maxTextureUnits, 0);
    if(maxTextureUnits[0] < 2) {
      throw new RuntimeException("Need at least 2 texture units. Available: " + maxTextureUnits[0]);
    }

    int[] maxTextureCoords = new int[1];
    gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_COORDS, maxTextureCoords, 0);
    if(maxTextureCoords[0] < 3) {
      throw new RuntimeException("Need at least 3 texture coordinates. Available: " + maxTextureCoords[0]);
    }
  }

  private TextureData loadTexture(GL2 gl, GLProfile profile, final String filename) {
    try (InputStream stream = ScatterPlot.class.getResourceAsStream(filename)) {
      return TextureIO.newTextureData(profile, stream, false, "png");
    }
    catch(IOException exc) {
      throw new RuntimeException("Could not load texture: " + filename, exc);
    }
  }

  @Override
  public void enableProgram(GL2 gl) {
    gl.glUseProgram(st_prog);

    gl.glActiveTexture(GL.GL_TEXTURE0);
    gl.glBindTexture(GL.GL_TEXTURE_2D, textures[0]);
    gl.glActiveTexture(GL.GL_TEXTURE1);
    gl.glBindTexture(GL.GL_TEXTURE_2D, textures[1]);
    // Reset to Texture0, needed for i945 (otherwise: slow)
    gl.glActiveTexture(GL.GL_TEXTURE0);

    float[] eye = camera.getEyePosition();
    gl.glUniform3f(gl.glGetUniformLocation(st_prog, "eye"), eye[0], eye[1], eye[2]);
    gl.glUniform1f(gl.glGetUniformLocation(st_prog, "size"), 25.f);
    gl.glUniform1i(gl.glGetUniformLocation(st_prog, "texAlpha"), 0); // texture
    gl.glUniform1i(gl.glGetUniformLocation(st_prog, "texColor"), 1); // texture
    gl.glUniform1f(gl.glGetUniformLocation(st_prog, "alpha"), .8f);
    gl.glUniform1i(gl.glGetUniformLocation(st_prog, "grid"), texgrid);
    gl.glUniform1i(gl.glGetUniformLocation(st_prog, "numcolors"), numcolors);
  }

  @Override
  public void setCamera(SimpleCamera3D camera) {
    this.camera = camera;
  }

  @Override
  public void free(GL2 gl) {
    gl.glDeleteTextures(1, textures, 0);
    for(int shader : shaders) {
      gl.glDeleteShader(shader);
    }
    gl.glDeleteProgram(st_prog);
  }
}
