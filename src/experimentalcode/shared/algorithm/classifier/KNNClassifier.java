package experimentalcode.shared.algorithm.classifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * KNNClassifier classifies instances based on the class distribution among the
 * k nearest neighbors in a database.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * @param <L> the type of the ClassLabel the Classifier is assigning
 */
@Title("kNN-classifier")
@Description("Lazy classifier classifies a given instance to the majority class of the k-nearest neighbors.")
public class KNNClassifier<O, D extends Distance<D>, L extends ClassLabel> extends DistanceBasedClassifier<O, D, L> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(KNNClassifier.class);
  
  /**
   * OptionID for
   * {@link experimentalcode.shared.algorithm.classifier.KNNClassifier#K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knnclassifier.k", "The number of neighbors to take into account for classification.");

  /**
   * Parameter to specify the number of neighbors to take into account for
   * classification, must be an integer greater than 0.
   * <p>
   * Default value: {@code 1}
   * </p>
   * <p>
   * Key: {@code -knnclassifier.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0), 1);

  /**
   * Holds the value of @link #K_PARAM}.
   */
  protected int k;

  /**
   * Holds the database where the classification is to base on.
   */
  protected Database database;

  /**
   * Provides a KNNClassifier, adding parameter {@link #K_PARAM} to the option
   * handler additionally to parameters of super class.
   */
  public KNNClassifier(Parameterization config) {
    super(config);
    config = config.descend(this);
    // parameter k
    if (config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
  }

  /**
   * Checks whether the database has the class labels set. Collects the class
   * labels available n the database. Holds the database to lazily classify new
   * instances later on.
   */
  @Override
  public void buildClassifier(Database database, ArrayList<L> labels) throws IllegalStateException {
    this.setLabels(labels);
    this.database = database;
  }

  /**
   * Provides a class distribution for the given instance. The distribution is
   * the relative value for each possible class among the k nearest neighbors of
   * the given instance in the previously specified database.
   */
  @Override
  public double[] classDistribution(O instance) throws IllegalStateException {
    try {
      double[] distribution = new double[getLabels().size()];
      int[] occurences = new int[getLabels().size()];

      KNNQuery<O, D> knnq = database.getKNNQuery(getDistanceQuery(), k);
      List<DistanceResultPair<D>> query = knnq.getKNNForObject(instance, k);
      Relation<ClassLabel> crep = database.getRelation(SimpleTypeInformation.get(ClassLabel.class));
      for(DistanceResultPair<D> neighbor : query) {
        // noinspection unchecked
        int index = Collections.binarySearch(getLabels(), (AssociationID.CLASS.getType().cast(crep.get(neighbor.getID()))));
        if(index >= 0) {
          occurences[index]++;
        }
      }
      for(int i = 0; i < distribution.length; i++) {
        distribution[i] = ((double) occurences[i]) / (double) query.size();
      }
      return distribution;
    }
    catch(NullPointerException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public String model() {
    return "lazy learner - provides no model";
  }

  @Override
  public Result run(@SuppressWarnings("unused") Database database) throws IllegalStateException {
    // TODO Implement sensible default behavior.
    return null;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}