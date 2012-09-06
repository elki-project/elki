package de.lmu.ifi.dbs.elki.data.type;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.data.FloatVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.data.SparseDoubleVector;
import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.spatial.PolygonsObject;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;

/**
 * Utility package containing various common types.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has TypeInformation oneway - -
 * @apiviz.landmark
 */
public final class TypeUtil {
  /**
   * Fake Constructor.
   */
  private TypeUtil() {
    // Do not instantiate.
  }
  
  /**
   * Input type for algorithms that accept anything.
   */
  public static final SimpleTypeInformation<Object> ANY = new SimpleTypeInformation<Object>(Object.class);

  /**
   * Database IDs.
   */
  public static final SimpleTypeInformation<DBID> DBID = new SimpleTypeInformation<DBID>(DBID.class, DBIDFactory.FACTORY.getDBIDSerializer());

  /**
   * Database ID lists.
   */
  public static final SimpleTypeInformation<DBIDs> DBIDS = new SimpleTypeInformation<DBIDs>(DBIDs.class);

  /**
   * A string.
   */
  public static final SimpleTypeInformation<String> STRING = new SimpleTypeInformation<String>(String.class, ByteArrayUtil.STRING_SERIALIZER);

  /**
   * A class label.
   */
  public static final SimpleTypeInformation<ClassLabel> CLASSLABEL = new SimpleTypeInformation<ClassLabel>(ClassLabel.class);

  /**
   * Simple class labels.
   */
  public static final SimpleTypeInformation<SimpleClassLabel> SIMPLE_CLASSLABEL = new SimpleTypeInformation<SimpleClassLabel>(SimpleClassLabel.class, SimpleClassLabel.SERIALIZER);

  /**
   * A list of labels.
   */
  public static final SimpleTypeInformation<LabelList> LABELLIST = new SimpleTypeInformation<LabelList>(LabelList.class, LabelList.SERIALIZER);

  /**
   * A list of neighbors.
   */
  public static final SimpleTypeInformation<DistanceDBIDResult<?>> NEIGHBORLIST = new SimpleTypeInformation<DistanceDBIDResult<?>>(DistanceDBIDResult.class);

  /**
   * Either class label, object labels or a string - anything that will be
   * accepted by
   * {@link de.lmu.ifi.dbs.elki.utilities.DatabaseUtil#guessObjectLabelRepresentation}.
   */
  public static final TypeInformation GUESSED_LABEL = new AlternativeTypeInformation(LABELLIST, CLASSLABEL, STRING);

  /**
   * Number vectors of <em>variable</em> length.
   */
  public static final SimpleTypeInformation<? super NumberVector<?, ?>> NUMBER_VECTOR_VARIABLE_LENGTH = new SimpleTypeInformation<NumberVector<?, ?>>(NumberVector.class);

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
   * 
   * If possible, please use {@link #NUMBER_VECTOR_FIELD}!
   */
  public static final VectorFieldTypeInformation<FloatVector> FLOAT_VECTOR_FIELD = new VectorFieldTypeInformation<FloatVector>(FloatVector.class);

  /**
   * Input type for algorithms that require number vector fields.
   */
  public static final VectorFieldTypeInformation<BitVector> BIT_VECTOR_FIELD = new VectorFieldTypeInformation<BitVector>(BitVector.class);

  /**
   * Sparse float vector field.
   */
  public static final SimpleTypeInformation<SparseNumberVector<?, ?>> SPARSE_VECTOR_VARIABLE_LENGTH = new SimpleTypeInformation<SparseNumberVector<?, ?>>(SparseNumberVector.class);

  /**
   * Sparse vector field.
   */
  public static final VectorFieldTypeInformation<SparseNumberVector<?, ?>> SPARSE_VECTOR_FIELD = new VectorFieldTypeInformation<SparseNumberVector<?, ?>>(SparseNumberVector.class);

  /**
   * Sparse float vector field.
   * 
   * If possible, please use {@link #SPARSE_VECTOR_FIELD} instead!
   */
  public static final VectorFieldTypeInformation<SparseFloatVector> SPARSE_FLOAT_FIELD = new VectorFieldTypeInformation<SparseFloatVector>(SparseFloatVector.class);

  /**
   * Sparse double vector field.
   * 
   * If possible, please use {@link #SPARSE_VECTOR_FIELD} instead!
   */
  public static final VectorFieldTypeInformation<SparseDoubleVector> SPARSE_DOUBLE_FIELD = new VectorFieldTypeInformation<SparseDoubleVector>(SparseDoubleVector.class);

  /**
   * External ID type.
   */
  public static final SimpleTypeInformation<ExternalID> EXTERNALID = new SimpleTypeInformation<ExternalID>(ExternalID.class);

  /**
   * Type for polygons.
   */
  public static final SimpleTypeInformation<PolygonsObject> POLYGON_TYPE = new SimpleTypeInformation<PolygonsObject>(PolygonsObject.class);

  /**
   * Double type, outlier scores etc.
   */
  public static final SimpleTypeInformation<Double> DOUBLE = new SimpleTypeInformation<Double>(Double.class, ByteArrayUtil.DOUBLE_SERIALIZER);

  /**
   * Integer type.
   */
  public static final SimpleTypeInformation<Integer> INTEGER = new SimpleTypeInformation<Integer>(Integer.class, ByteArrayUtil.INT_SERIALIZER);

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
  public static TypeInformation[] array(TypeInformation... ts) {
    return ts;
  }
}