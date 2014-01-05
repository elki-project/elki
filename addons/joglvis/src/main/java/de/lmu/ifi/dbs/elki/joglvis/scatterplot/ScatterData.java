package de.lmu.ifi.dbs.elki.joglvis.scatterplot;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 * Class to manage the vector data in the GPU.
 * 
 * @author Erich Schubert
 */
public class ScatterData {
  /**
   * Vertex buffer IDs.
   */
  int[] vbos;

  /**
   * Number of vectors stored.
   */
  int length;

  /**
   * Stride, i.e. size of each point.
   */
  public int stride;

  /**
   * Offset for the class data.
   */
  public int classOffset;

  /**
   * Offset for the vector data.
   */
  public int vecOffset;

  /**
   * Dimensions to visualize.
   */
  private int[] dims = { 0, 1, 2 };

  /**
   * Size of a float, in bytes.
   */
  public static final int SIZE_FLOAT = 4;

  /**
   * Get the data size.
   * 
   * @return Number of vertexes.
   */
  public int size() {
    return length;
  }

  public void initializeData(GL2 gl) {
    length = 10000;

    // Initialize vertex buffer handles:
    vbos = new int[1];
    gl.glGenBuffers(1, vbos, 0);
    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbos[0]);
    gl.glBufferData(GL.GL_ARRAY_BUFFER, length // Number of lines *
        * 4 // 3 coordinates + 1 type
        * SIZE_FLOAT //
        + 3 * SIZE_FLOAT // safety padding
    , null, GL2.GL_STATIC_DRAW);
    ByteBuffer vbytebuffer = gl.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);
    FloatBuffer vertices = vbytebuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();

    Random r = new Random();
    for(int i = 0; i < length; i++) {
      vertices.put((float) r.nextGaussian());
      vertices.put((float) r.nextGaussian());
      vertices.put((float) r.nextGaussian());
      vertices.put((float) r.nextInt(6));
    }
    stride = /* floats per vertex */4 * SIZE_FLOAT;
    vecOffset = 0;
    classOffset = 3 * SIZE_FLOAT;

    vertices.flip();
    gl.glUnmapBuffer(GL.GL_ARRAY_BUFFER);
    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
  }

  public void free(GL2 gl) {
    if(vbos[0] >= 0) {
      gl.glDeleteBuffers(1, vbos, 0);
    }
    vbos[0] = -1;
  }

  /**
   * Get the OpenGL buffer ID.
   * 
   * @return OpenGL buffer ID.
   */
  public int getBufferID() {
    return vbos[0];
  }

  /**
   * Get the offset for the current X coordinate.
   * 
   * @return Buffer offset in bytes.
   */
  public int getOffsetX() {
    return vecOffset + dims[0] * SIZE_FLOAT;
  }

  /**
   * Get the offset for the current Y coordinate.
   * 
   * @return Buffer offset in bytes.
   */
  public int getOffsetY() {
    return vecOffset + dims[1] * SIZE_FLOAT;
  }

  /**
   * Get the offset for the current Z coordinate.
   * 
   * @return Buffer offset in bytes.
   */
  public int getOffsetZ() {
    return vecOffset + dims[2] * SIZE_FLOAT;
  }

  /**
   * Get the offset for the current shape source. 
   * 
   * @return Buffer offset in bytes.
   */
  public int getOffsetShapeNum() {
    return classOffset;
  }

  /**
   * Get the offset for the current integer color source.
   * 
   * @return Buffer offset in bytes.
   */
  public int getOffsetColorNum() {
    return classOffset;
  }
}