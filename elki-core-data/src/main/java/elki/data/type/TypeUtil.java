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
package elki.data.type;

import elki.data.BitVector;
import elki.data.ClassLabel;
import elki.data.DoubleVector;
import elki.data.ExternalID;
import elki.data.FeatureVector;
import elki.data.FloatVector;
import elki.data.LabelList;
import elki.data.NumberVector;
import elki.data.SimpleClassLabel;
import elki.data.SparseDoubleVector;
import elki.data.SparseFloatVector;
import elki.data.SparseNumberVector;
import elki.data.spatial.PolygonsObject;
import elki.data.spatial.SpatialComparable;
import elki.database.ids.DBID;
import elki.database.ids.DBIDFactory;
import elki.database.ids.DBIDs;
import elki.database.ids.DoubleDBIDList;
import elki.database.ids.KNNList;
import elki.utilities.io.ByteArrayUtil;

/**
 * Utility package containing various common types.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @navhas - - - TypeInformation
 * @opt nodefillcolor LemonChiffon
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
  public static final SimpleTypeInformation<Object> ANY = new SimpleTypeInformation<>(Object.class);

  /**
   * Database IDs.
   */
  public static final SimpleTypeInformation<DBID> DBID = new SimpleTypeInformation<>(DBID.class, DBIDFactory.FACTORY.getDBIDSerializer());

  /**
   * Database ID lists (but not single DBIDs).
   */
  public static final SimpleTypeInformation<DBIDs> DBIDS = new SimpleTypeInformation<DBIDs>(DBIDs.class) {
    @Override
    public boolean isAssignableFromType(TypeInformation type) {
      return super.isAssignableFromType(type) && !DBID.isAssignableFromType(type);
    }
  };

  /**
   * A string.
   */
  public static final SimpleTypeInformation<String> STRING = new SimpleTypeInformation<>(String.class, ByteArrayUtil.STRING_SERIALIZER);

  /**
   * A class label.
   */
  public static final SimpleTypeInformation<ClassLabel> CLASSLABEL = new SimpleTypeInformation<>(ClassLabel.class);

  /**
   * Simple class labels.
   */
  public static final SimpleTypeInformation<SimpleClassLabel> SIMPLE_CLASSLABEL = new SimpleTypeInformation<>(SimpleClassLabel.class, SimpleClassLabel.SERIALIZER);

  /**
   * A list of labels.
   */
  public static final SimpleTypeInformation<LabelList> LABELLIST = new SimpleTypeInformation<>(LabelList.class, LabelList.SERIALIZER);

  /**
   * A list of neighbors.
   */
  public static final SimpleTypeInformation<DoubleDBIDList> NEIGHBORLIST = new SimpleTypeInformation<>(DoubleDBIDList.class);

  /**
   * Either class label, object labels or a string - anything that will be
   * accepted by
   * {@link elki.database.DatabaseUtil#guessObjectLabelRepresentation}
   * .
   */
  public static final TypeInformation GUESSED_LABEL = new AlternativeTypeInformation(LABELLIST, CLASSLABEL, STRING);

  /**
   * Number vectors of <em>variable</em> length.
   */
  public static final VectorTypeInformation<NumberVector> NUMBER_VECTOR_VARIABLE_LENGTH = VectorTypeInformation.typeRequest(NumberVector.class);

  /**
   * Input type for algorithms that require number vector fields.
   */
  public static final VectorFieldTypeInformation<NumberVector> NUMBER_VECTOR_FIELD = VectorFieldTypeInformation.typeRequest(NumberVector.class);

  /**
   * Type request for two-dimensional number vectors
   */
  public static final VectorFieldTypeInformation<? super NumberVector> NUMBER_VECTOR_FIELD_1D = VectorFieldTypeInformation.typeRequest(NumberVector.class, 1, 1);

  /**
   * Type request for two-dimensional number vectors
   */
  public static final VectorFieldTypeInformation<? super NumberVector> NUMBER_VECTOR_FIELD_2D = VectorFieldTypeInformation.typeRequest(NumberVector.class, 2, 2);

  /**
   * Type request for multivariate time series.
   */
  public static final MultivariateSeriesTypeInformation<NumberVector> MULTIVARIATE_SERIES = MultivariateSeriesTypeInformation.typeRequest(NumberVector.class);

  /**
   * Input type for algorithms that require number vector fields.
   *
   * If possible, please use {@link #NUMBER_VECTOR_FIELD}!
   */
  public static final VectorFieldTypeInformation<DoubleVector> DOUBLE_VECTOR_FIELD = VectorFieldTypeInformation.typeRequest(DoubleVector.class);

  /**
   * Input type for algorithms that require number vector fields.
   *
   * If possible, please use {@link #NUMBER_VECTOR_FIELD}!
   */
  public static final VectorFieldTypeInformation<FloatVector> FLOAT_VECTOR_FIELD = VectorFieldTypeInformation.typeRequest(FloatVector.class);

  /**
   * Input type for algorithms that require bit vectors.
   */
  public static final VectorTypeInformation<BitVector> BIT_VECTOR = VectorTypeInformation.typeRequest(BitVector.class);

  /**
   * Input type for algorithms that require bit vector fields.
   */
  public static final VectorFieldTypeInformation<BitVector> BIT_VECTOR_FIELD = VectorFieldTypeInformation.typeRequest(BitVector.class);

  /**
   * Sparse float vector field.
   */
  public static final VectorTypeInformation<SparseNumberVector> SPARSE_VECTOR_VARIABLE_LENGTH = VectorTypeInformation.typeRequest(SparseNumberVector.class);

  /**
   * Sparse vector field.
   */
  public static final VectorFieldTypeInformation<SparseNumberVector> SPARSE_VECTOR_FIELD = VectorFieldTypeInformation.typeRequest(SparseNumberVector.class);

  /**
   * Sparse float vector field.
   *
   * If possible, please use {@link #SPARSE_VECTOR_FIELD} instead!
   */
  public static final VectorFieldTypeInformation<SparseFloatVector> SPARSE_FLOAT_FIELD = VectorFieldTypeInformation.typeRequest(SparseFloatVector.class);

  /**
   * Sparse double vector field.
   *
   * If possible, please use {@link #SPARSE_VECTOR_FIELD} instead!
   */
  public static final VectorFieldTypeInformation<SparseDoubleVector> SPARSE_DOUBLE_FIELD = VectorFieldTypeInformation.typeRequest(SparseDoubleVector.class);

  /**
   * External ID type.
   */
  public static final SimpleTypeInformation<ExternalID> EXTERNALID = new SimpleTypeInformation<>(ExternalID.class);

  /**
   * Type for polygons.
   */
  public static final SimpleTypeInformation<PolygonsObject> POLYGON_TYPE = new SimpleTypeInformation<>(PolygonsObject.class);

  /**
   * Double type, outlier scores etc.
   */
  public static final SimpleTypeInformation<Double> DOUBLE = new SimpleTypeInformation<>(Double.class, ByteArrayUtil.DOUBLE_SERIALIZER);

  /**
   * Integer type.
   */
  public static final SimpleTypeInformation<Integer> INTEGER = new SimpleTypeInformation<>(Integer.class, ByteArrayUtil.INT_SERIALIZER);

  /**
   * Double array objects (do <b>not</b> use for input data points).
   */
  public static final SimpleTypeInformation<double[]> DOUBLE_ARRAY = new SimpleTypeInformation<>(double[].class);

  /**
   * Integer array objects.
   */
  public static final SimpleTypeInformation<int[]> INTEGER_ARRAY = new SimpleTypeInformation<>(int[].class);

  /**
   * Matrix type.
   */
  public static final SimpleTypeInformation<double[][]> MATRIX = new SimpleTypeInformation<>(double[][].class);

  /**
   * Any feature vector type.
   */
  public static final VectorTypeInformation<FeatureVector<?>> FEATURE_VECTORS = VectorTypeInformation.typeRequest(FeatureVector.class);

  /**
   * Spatial objects.
   */
  public static final SimpleTypeInformation<SpatialComparable> SPATIAL_OBJECT = new SimpleTypeInformation<>(SpatialComparable.class);

  /**
   * KNN lists.
   */
  public static final SimpleTypeInformation<KNNList> KNNLIST = new SimpleTypeInformation<>(KNNList.class);

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
