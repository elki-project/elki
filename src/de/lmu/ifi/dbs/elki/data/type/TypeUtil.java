package de.lmu.ifi.dbs.elki.data.type;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.ExternalID;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.spatial.PolygonsObject;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Utility package containing various common types
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has TypeInformation oneway - -
 * @apiviz.landmark
 */
public final class TypeUtil {
  /**
   * Input type for algorithms that accept anything
   */
  public static final SimpleTypeInformation<Object> ANY = new SimpleTypeInformation<Object>(Object.class);

  /**
   * Database IDs
   */
  public static final SimpleTypeInformation<DBID> DBID = new SimpleTypeInformation<DBID>(DBID.class);

  /**
   * A string
   */
  public static final SimpleTypeInformation<String> STRING = new SimpleTypeInformation<String>(String.class);

  /**
   * A class label
   */
  public static final SimpleTypeInformation<ClassLabel> CLASSLABEL = new SimpleTypeInformation<ClassLabel>(ClassLabel.class);

  /**
   * A list of labels.
   */
  public static final SimpleTypeInformation<LabelList> LABELLIST = new SimpleTypeInformation<LabelList>(LabelList.class);

  /**
   * Either class label, object labels or a string - anything that will be
   * accepted by
   * {@link de.lmu.ifi.dbs.elki.utilities.DatabaseUtil#guessObjectLabelRepresentation}
   */
  public static final TypeInformation GUESSED_LABEL = new AlternativeTypeInformation(LABELLIST, CLASSLABEL, STRING);

  /**
   * Input type for algorithms that require number vector fields.
   */
  public static final VectorFieldTypeInformation<NumberVector<?, ?>> NUMBER_VECTOR_FIELD = new VectorFieldTypeInformation<NumberVector<?, ?>>(NumberVector.class);

  /**
   * Input type for algorithms that require number vector fields.
   * 
   * If possible, please use {@link #NUMBER_VECTOR_FIELD}!
   */
  public static final VectorFieldTypeInformation<DoubleVector> DOUBLE_VECTOR_FIELD = new VectorFieldTypeInformation<DoubleVector>(DoubleVector.class);

  /**
   * Input type for algorithms that require number vector fields.
   */
  public static final VectorFieldTypeInformation<BitVector> BIT_VECTOR_FIELD = new VectorFieldTypeInformation<BitVector>(BitVector.class);

  /**
   * Sparse float vector field.
   */
  public static final VectorFieldTypeInformation<SparseFloatVector> SPARSE_FLOAT_FIELD = new VectorFieldTypeInformation<SparseFloatVector>(SparseFloatVector.class);

  /**
   * External ID type
   */
  public static final SimpleTypeInformation<ExternalID> EXTERNALID = new SimpleTypeInformation<ExternalID>(ExternalID.class);

  /**
   * Type for polygons
   */
  public static final SimpleTypeInformation<PolygonsObject> POLYGON_TYPE = new SimpleTypeInformation<PolygonsObject>(PolygonsObject.class);

  /**
   * Double type, outlier scores etc.
   */
  public static final SimpleTypeInformation<Double> DOUBLE = new SimpleTypeInformation<Double>(Double.class);

  /**
   * Integer type.
   */
  public static final SimpleTypeInformation<Integer> INTEGER = new SimpleTypeInformation<Integer>(Integer.class);

  /**
   * Vector type.
   */
  public static final SimpleTypeInformation<Vector> VECTOR = new SimpleTypeInformation<Vector>(Vector.class);

  /**
   * Matrix type.
   */
  public static final SimpleTypeInformation<Matrix> MATRIX = new SimpleTypeInformation<Matrix>(Matrix.class);

  /**
   * Cluster model type.
   */
  public static final SimpleTypeInformation<Model> MODEL = new SimpleTypeInformation<Model>(Model.class);

  /**
   * Make a type array easily.
   * 
   * @param ts Types
   * @return array
   */
  public static final TypeInformation[] array(TypeInformation... ts) {
    return ts;
  }
}