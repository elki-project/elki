package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.distance.SubspaceDistance;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;

import java.util.regex.Pattern;

/**
 * Provides a distance function to determine a kind of correlation distance
 * between two points, which is a pair consisting of the distance between the two subspaces
 * spanned by the strong eigenvectors of the two points and the affine distance
 * between the two subspaces.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class SubspaceDistanceFunction<O extends RealVector>
    extends AbstractPreprocessorBasedDistanceFunction<O, SubspaceDistance> {

  /**
   * The Assocoiation ID for the association to be set by the preprocessor.
   */
  public static final AssociationID ASSOCIATION_ID = AssociationID.LOCAL_PCA;

  /**
   * The super class for the preprocessor.
   */
  public static final Class PREPROCESSOR_SUPER_CLASS = Preprocessor.class;

  /**
   * The default preprocessor class name.
   */
  public static final String DEFAULT_PREPROCESSOR_CLASS = KnnQueryBasedHiCOPreprocessor.class.getName();

  /**
   * Description for parameter preprocessor.
   */
  public static final String PREPROCESSOR_CLASS_D = "the preprocessor to determine the correlation dimensions of the objects " +
                                                    Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Preprocessor.class) +
                                                    ". Default: " + SubspaceDistanceFunction.DEFAULT_PREPROCESSOR_CLASS;

  /**
   * Provides a distance function to determine distances
   * between subspaces of equal dimensionality.
   */
  public SubspaceDistanceFunction() {
    super(Pattern.compile("\\d+(\\.\\d+)?([eE][-]?\\d+)?" +
                          AbstractCorrelationDistanceFunction.SEPARATOR.pattern() +
                          "\\d+(\\.\\d+)?([eE][-]?\\d+)?"));
  }

  /**
   * Returns the name of the default preprocessor.
   */
  String getDefaultPreprocessorClassName() {
    return DEFAULT_PREPROCESSOR_CLASS;
  }

  /**
   * Returns the description for parameter preprocessor.
   */
  String getPreprocessorClassDescription() {
    return PREPROCESSOR_CLASS_D;
  }

  /**
   * Returns the super class for the preprocessor.
   */
  Class getPreprocessorSuperClassName() {
    return PREPROCESSOR_SUPER_CLASS;
  }

  /**
   * Returns the assocoiation ID for the association to be set by the preprocessor.
   */
  AssociationID getAssociationID() {
    return ASSOCIATION_ID;
  }

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#valueOf(String)
   */
  public SubspaceDistance valueOf(String pattern) throws IllegalArgumentException {
    if (pattern.equals(INFINITY_PATTERN)) {
      return infiniteDistance();
    }
    if (matches(pattern)) {
      String[] values = AbstractCorrelationDistanceFunction.SEPARATOR.split(pattern);
      return new SubspaceDistance(Double.parseDouble(values[0]), Double.parseDouble(values[1]));
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" +
                                         pattern +
                                         "\" does not match required pattern \"" +
                                         requiredInputPattern() + "\"");
    }
  }

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#infiniteDistance()
   */
  public SubspaceDistance infiniteDistance() {
    return new SubspaceDistance(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#nullDistance()
   */
  public SubspaceDistance nullDistance() {
    return new SubspaceDistance(0, 0);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#undefinedDistance()
   */
  public SubspaceDistance undefinedDistance() {
    return new SubspaceDistance(Double.NaN, Double.NaN);
  }

  /**
   * Note, that the pca of o1 must have equal ore more strong
   * eigenvectors than the pca of o2.
   *
   * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject, de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public SubspaceDistance distance(O o1, O o2) {
    LocalPCA pca1 = (LocalPCA) getDatabase().getAssociation(AssociationID.LOCAL_PCA, o1.getID());
    LocalPCA pca2 = (LocalPCA) getDatabase().getAssociation(AssociationID.LOCAL_PCA, o2.getID());
    return distance(o1, o2, pca1, pca2);
  }

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function. Note, that the first pca must have an equal number of strong
   * eigenvectors than the second pca.
   *
   * @param o1   first DatabaseObject
   * @param o2   second DatabaseObject
   * @param pca1 first PCA
   * @param pca2 second PCA
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  public SubspaceDistance distance(O o1, O o2, LocalPCA pca1, LocalPCA pca2) {
    if (pca1.getCorrelationDimension() != pca2.getCorrelationDimension()) {
      throw new IllegalStateException("pca1.getCorrelationDimension() != pca2.getCorrelationDimension()");
    }

    Matrix strong_ev1 = pca1.getStrongEigenvectors();
    Matrix weak_ev2 = pca2.getWeakEigenvectors();

//    Matrix weak_ev1 = pca1.getWeakEigenvectors();
//    Matrix strong_ev2 = pca2.getStrongEigenvectors();

    Matrix m1 = weak_ev2.getColumnDimensionality() == 0 ? strong_ev1.transpose() : strong_ev1.transpose().times(weak_ev2);
//    Matrix m2 = weak_ev1.getColumnDimensionality() == 0 ? strong_ev2.transpose() : strong_ev2.transpose().times(weak_ev1);

    double d1 = m1.norm2();
//    double d2 = m2.norm2();

//    System.out.println("");
//    System.out.println(getDatabase().getAssociation(AssociationID.LABEL, o1.getID()) + " - " +
//                       getDatabase().getAssociation(AssociationID.LABEL, o2.getID()));
//    System.out.println(pca1.getCorrelationDimension()+ "- " +pca2.getCorrelationDimension());
//    System.out.println("d1 " + d1);
//    System.out.println("d2 " + d2);


    WeightedDistanceFunction<O> df1 = new WeightedDistanceFunction<O>(pca1.similarityMatrix());
    WeightedDistanceFunction<O> df2 = new WeightedDistanceFunction<O>(pca2.similarityMatrix());

    double affineDistance = Math.max(df1.distance(o1, o2).getDoubleValue(),
                                     df2.distance(o1, o2).getDoubleValue());

    return new SubspaceDistance(d1, affineDistance);
  }
}
