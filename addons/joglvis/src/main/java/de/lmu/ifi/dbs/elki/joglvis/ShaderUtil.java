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

import java.io.IOException;
import java.io.InputStream;

import javax.media.opengl.GL2;

import de.lmu.ifi.dbs.elki.utilities.io.FileUtil;

/**
 * Class to help dealing with shaders.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public final class ShaderUtil {
  /**
   * Private constructor. Static methods only.
   */
  private ShaderUtil() {
    // Do not use.
  }

  /**
   * Compile a shader from a file.
   *
   * @param context Class context for loading the resource file.
   * @param gl GL context
   * @param type
   * @param name
   * @return Shader program number.
   * @throws ShaderCompilationException When compilation failed.
   */
  public static int compileShader(Class<?> context, GL2 gl, int type, String name) throws ShaderCompilationException {
    int prog = -1;
    try (InputStream in = context.getResourceAsStream(name)) {
      int[] error = new int[1];
      String shaderdata = FileUtil.slurp(in);
      prog = gl.glCreateShader(type);
      gl.glShaderSource(prog, 1, new String[] { shaderdata }, null, 0);
      gl.glCompileShader(prog);
      // This worked best for me to capture error messages:
      gl.glGetObjectParameterivARB(prog, GL2.GL_OBJECT_INFO_LOG_LENGTH_ARB, error, 0);
      if(error[0] > 1) {
        byte[] info = new byte[error[0]];
        gl.glGetInfoLogARB(prog, info.length, error, 0, info, 0);
        String out = new String(info);
        gl.glDeleteShader(prog);
        throw new ShaderCompilationException("Shader compilation error in '" + name + "': " + out);
      }
      // Different way of catching errors.
      gl.glGetShaderiv(prog, GL2.GL_COMPILE_STATUS, error, 0);
      if(error[0] > 1) {
        throw new ShaderCompilationException("Shader compilation of '" + name + "' failed.");
      }
    }
    catch(IOException e) {
      throw new ShaderCompilationException("IO error loading shader: " + name, e);
    }
    return prog;
  }

  /**
   * Link multiple (compiled) shaders into one program.
   *
   * @param gl GL context
   * @param shaders Shaders to link
   * @return Program id
   * @throws ShaderCompilationException on errors.
   */
  public static int linkShaderProgram(GL2 gl, int[] shaders) throws ShaderCompilationException {
    int[] error = new int[1];
    int shaderprogram = gl.glCreateProgram();
    for(int shader : shaders) {
      gl.glAttachShader(shaderprogram, shader);
    }
    gl.glLinkProgram(shaderprogram);
    gl.glValidateProgram(shaderprogram);

    // This worked best for me to get error messages:
    gl.glGetObjectParameterivARB(shaderprogram, GL2.GL_OBJECT_INFO_LOG_LENGTH_ARB, error, 0);
    if(error[0] > 1) {
      byte[] info = new byte[error[0]];
      gl.glGetInfoLogARB(shaderprogram, info.length, error, 0, info, 0);
      String out = new String(info);
      gl.glDeleteProgram(shaderprogram);
      throw new ShaderCompilationException("Shader compilation error: " + out);
    }
    return shaderprogram;
  }

  /**
   * Exceptions when compiling shaders.
   *
   * @author Erich Schubert
   */
  public static class ShaderCompilationException extends Exception {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param message Error message
     * @param cause Cause (e.g. file not found)
     */
    public ShaderCompilationException(String message, Throwable cause) {
      super(message, cause);
    }

    /**
     * Constructor.
     *
     * @param message Error message
     */
    public ShaderCompilationException(String message) {
      super(message);
    }
  }
}
