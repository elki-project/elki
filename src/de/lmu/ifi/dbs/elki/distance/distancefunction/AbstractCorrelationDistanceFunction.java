package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.CorrelationDistance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.properties.Properties;

import java.util.regex.Pattern;

/**
 * Abstract super class for correlation based distance functions. Provides the
 * Correlation distance for real valued vectors. All subclasses must implement a
 * method to process the preprocessing step in terms of doing the PCA for each
 * object of the database.
 *
 * @author Elke Achtert 
 */
public abstract class AbstractCorrelationDistanceFunction<O extends RealVector<O,?>, P extends Preprocessor<O>, D extends CorrelationDistance<D>>
    extends AbstractPreprocessorBasedDistanceFunction<O,P,D> {

  /**
   * Indicates a separator.
   */
  public static final Pattern SEPARATOR = Pattern.compile("x");

  /**
   * Provides a CorrelationDistanceFunction with a pattern defined to accept
   * Strings that define an Integer followed by a separator followed by a
   * Double.
   */
  public AbstractCorrelationDistanceFunction() {
    super(Pattern.compile("\\d+" + AbstractCorrelationDistanceFunction.SEPARATOR.pattern() + "\\d+(\\.\\d+)?([eE][-]?\\d+)?"));
  }

  /**
   * Provides the Correlation distance between the given two vectors.
   *
   * @return the Correlation distance between the given two vectors as an
   *         instance of {@link CorrelationDistance CorrelationDistance}.
   * @see DistanceFunction#distance(de.lmu.ifi.dbs.elki.data.DatabaseObject,
   *      de.lmu.ifi.dbs.elki.data.DatabaseObject)
   */
  public D distance(O rv1, O rv2) {
    return correlationDistance(rv1, rv2);
  }

  /**
   * Returns a description of the class and the required parameters.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("Correlation distance for NumberVectors. Pattern for defining a range: \""
                                           + requiredInputPattern() + "\".", false));
    description.append('\n');
    description.append("Preprocessors available within this framework for distance function ");
    description.append(this.getClass().getName());
    description.append(":\n");
    description.append(Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(getPreprocessorSuperClassName()));
    description.append('\n');
    return description.toString();

  }

  /**
   * Computes the correlation distance between the two specified vectors.
   *
   * @param dv1 first RealVector
   * @param dv2 second RealVector
   * @return the correlation distance between the two specified vectors
   */
  abstract D correlationDistance(O dv1, O dv2);

  /**
   * Returns the name of the default preprocessor.
   */
  abstract String getDefaultPreprocessorClassName();

  /**
   * Returns the description for parameter preprocessor.
   */
  abstract String getPreprocessorClassDescription();

  /**
   * Returns the super class for the preprocessor.
   */
  abstract Class<? extends Preprocessor> getPreprocessorSuperClassName();

  /**
   * Returns the assocoiation ID for the association to be set by the preprocessor.
   */
  abstract AssociationID getAssociationID();
}
