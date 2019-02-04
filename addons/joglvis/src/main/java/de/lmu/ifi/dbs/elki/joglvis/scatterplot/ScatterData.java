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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

/**
 * Class to manage the vector data in the GPU.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ScatterData {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ScatterData.class);

  /**
   * Vertex buffer IDs.
   */
  int[] vbos = { -1 };

  /**
   * Number of vectors stored.
   */
  int length = -1;

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
   * Relations to visualize.
   */
  private List<Relation<?>> relations;

  /**
   * Data dimensionality.
   */
  private int dim;

  /**
   * DBIDs to visualize.
   */
  private DBIDs ids;

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

  public ScatterData(DBIDs ids) {
    this.ids = ids;
    this.relations = new ArrayList<>();
  }

  public void addRelation(Relation<?> rel) {
    this.relations.add(rel);
  }

  public void initializeData(GL2 gl) {
    length = ids.size();
    dim = 0;
    vecOffset = -1;
    classOffset = -1;

    // Scan relations for dimensionalities:
    int[] dims = new int[relations.size()];
    LinearScale[][] scales = new LinearScale[relations.size()][];
    ArrayList<Relation<? extends NumberVector>> vrels = new ArrayList<>(relations.size());
    for(int r = 0; r < relations.size(); r++) {
      Relation<?> rel = relations.get(r);
      final SimpleTypeInformation<?> type = rel.getDataTypeInformation();
      if(type instanceof VectorFieldTypeInformation) {
        @SuppressWarnings("unchecked")
        final Relation<? extends NumberVector> vrel = (Relation<? extends NumberVector>) rel;
        final int d = ((VectorFieldTypeInformation<?>) type).getDimensionality();
        dims[r] = d;
        LinearScale[] rscales = new LinearScale[d];
        double[][] minmax = RelationUtil.computeMinMax(vrel);
        for(int i = 0; i < d; i++) {
          rscales[i] = new LinearScale(minmax[0][i], minmax[1][i]);
        }
        scales[r] = rscales;
        vrels.add(vrel);
        if(vecOffset < 0) {
          vecOffset = dim;
        }
        dim += d;
      }
      else {
        // FIXME: handle other relation types!
        dims[r] = 0;
        vrels.add(null);
      }
    }
    if(classOffset < 0) {
      ++dim;
    }
    LOG.warning("Dimensionalities: " + FormatUtil.format(dims));

    // Initialize vertex buffer handles:
    assert (vbos[0] == -1);
    gl.glGenBuffers(1, vbos, 0);
    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbos[0]);
    gl.glBufferData(GL.GL_ARRAY_BUFFER, length * dim * SIZE_FLOAT //
        + 3 * SIZE_FLOAT // safety padding
    , null, GL2.GL_STATIC_DRAW);
    ByteBuffer vbytebuffer = gl.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);
    FloatBuffer vertices = vbytebuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();

    Random rnd = new Random();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      for(int r = 0; r < dims.length; r++) {
        if(dims[r] <= 0) {
          continue;
        }
        final Relation<? extends NumberVector> vrel = vrels.get(r);
        LinearScale[] rscales = scales[r];
        if(vrel != null) {
          NumberVector vec = vrel.get(iter);
          for(int d = 0; d < dims[r]; d++) {
            // vertices.put( rnd.nextFloat());
            vertices.put((float) rscales[d].getScaled(vec.doubleValue(d)) * 2.f - 1.f);
          }
        }
      }
      if(classOffset < 0) {
        vertices.put(rnd.nextInt(30));
      }
    }
    stride = dim * SIZE_FLOAT;
    if(classOffset < 0) {
      classOffset = (dim - 1) * SIZE_FLOAT;
    }

    if(vertices.position() != length * dim) {
      LOG.warning("Size mismatch: " + vertices.position() + " expected: " + length * dim, new Throwable());
    }
    vertices.flip();
    gl.glUnmapBuffer(GL.GL_ARRAY_BUFFER);
    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

    LOG.warning("Size: " + length + " dim: " + dim + " " + vecOffset + " " + classOffset);
  }

  /**
   * Free all memory allocations.
   *
   * @param gl GL context
   */
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