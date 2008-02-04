package de.lmu.ifi.dbs.algorithm.classifier;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Classifier to classifiy instances based on the prior probability of classes in the database.
 *
 * @author Arthur Zimek
 */
public class PriorProbabilityClassifier<O extends DatabaseObject, L extends ClassLabel<L>> extends AbstractClassifier<O,L> {
  /**
   * The generated serial version UID.
   */
  private static final long serialVersionUID = -2276467915841161140L;

  /**
   * Holds the prior probabilities.
   */
  protected double[] distribution;

  /**
   * Index of the most abundant class.
   */
  protected int prediction;

  /**
   * Holds the database the prior probabilities are based on.
   */
  protected Database<O> database;

  /**
   * Provides a classifier always predicting the prior probabilities.
   */
  public PriorProbabilityClassifier() {
    super();
  }

  /**
   * Learns the prior probability for all classes.
   *
   * @see Classifier#buildClassifier(de.lmu.ifi.dbs.database.Database, de.lmu.ifi.dbs.data.ClassLabel[])
   */
  public void buildClassifier(Database<O> database, L[] classLabels) throws IllegalStateException {
    this.setLabels(classLabels);
    this.database = database;
    distribution = new double[getLabels().length];
    int[] occurences = new int[getLabels().length];
    for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      String label = (String) database.getAssociation(CLASS, iter.next());
      int index = Arrays.binarySearch(getLabels(), label);
      if (index > -1) {
        occurences[index]++;
      }
      else {
        throw new IllegalStateException("inconsistent state of database - found new label: " + label);
      }
    }
    double size = database.size();
    for (int i = 0; i < distribution.length; i++) {
      distribution[i] = ((double) occurences[i] / size);
    }
    prediction = Util.getIndexOfMaximum(distribution);
  }

  /**
   * Returns the index of the most abundant class.
   * According to the prior class probability distribution,
   * this is the index of the class showing maximum prior probability.
   *
   * @see Classifier#classify(DatabaseObject) 
   */
  @Override
  public int classify(O instance) throws IllegalStateException {
    return prediction;
  }

  /**
   * Returns the distribution of the classes' prior probabilities.
   *
   * @see Classifier#classDistribution(DatabaseObject)
   */
  public double[] classDistribution(O instance) throws IllegalStateException {
    return distribution;
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("Prior Probability Classifier", "Prior Probability Classifier", "Classifier to predict simply prior probabilities for all classes as defined by their relative abundance in a given database.", "");
  }

  /**
   * @see Classifier#model()
   */
  public String model() {
    StringBuffer output = new StringBuffer();
    for (int i = 0; i < distribution.length; i++) {
      output.append(getLabels()[i]);
      output.append(" : ");
      output.append(distribution[i]);
      output.append('\n');
    }
    return output.toString();
  }

}
