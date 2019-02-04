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
package de.lmu.ifi.dbs.elki.evaluation.classification;

import java.text.NumberFormat;
import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.ClassLabel;

/**
 * Provides a confusion matrix with some prediction performance measures that
 * can be derived from a confusion matrix.
 * 
 * @author Arthur Zimek
 * @since 0.7.0
 */
public class ConfusionMatrix {
  /**
   * Holds the confusion matrix. Must be a square matrix. The rows (first index)
   * give the values which classes are classified as row, the columns (second
   * index) give the values the class col has been classified as. Thus,
   * <code>confusion[predicted][real]</code> addresses the number of instances
   * of class <i>real</i> that have been assigned to the class <i>predicted</i>.
   */
  private int[][] confusion;

  /**
   * Holds the class labels.
   */
  private ArrayList<ClassLabel> labels;

  /**
   * Provides a confusion matrix for the given values.
   * 
   * @param labels the class labels - must conform the confusion matrix in
   *        length
   * @param confusion the confusion matrix. Must be a square matrix. The rows
   *        (first index) give the values which classes are classified as row,
   *        the columns (second index) give the values the class col has been
   *        classified as. Thus, <code>confusion[predicted][real]</code>
   *        addresses the number of instances of class <i>real</i> that have
   *        been assigned to the class <i>predicted</i>.
   * @throws IllegalArgumentException if the confusion matrix is not square or
   *         not complete or if the length of class labels does not conform the
   *         length of the confusion matrix
   */
  public ConfusionMatrix(ArrayList<ClassLabel> labels, int[][] confusion) throws IllegalArgumentException {
    for(int i = 0; i < confusion.length; i++) {
      if(confusion.length != confusion[i].length) {
        throw new IllegalArgumentException("Confusion matrix irregular: row-dimension = " + confusion.length + ", col-dimension in col" + i + " = " + confusion[i].length);
      }
    }
    if(confusion.length != labels.size()) {
      throw new IllegalArgumentException("Number of class labels does not match row dimension of confusion matrix.");
    }
    this.confusion = confusion;
    this.labels = labels;
  }

  /**
   * Provides the <i>true positive rate</i>. Aka <i>accuracy</i> or
   * <i>sensitivity</i> or <i>recall</i>: <code>TP / (TP+FN)</code>.
   * 
   * 
   * @return the true positive rate
   */
  public double truePositiveRate() {
    return ((double) truePositives()) / (double) totalInstances();
  }

  /**
   * Provides the <i>false positive rate</i> for the specified class.
   * 
   * 
   * @param classindex the index of the class to retrieve the false positive
   *        rate for
   * @return the false positive rate for the specified class
   */
  public double falsePositiveRate(int classindex) {
    int fp = falsePositives(classindex);
    int tn = trueNegatives(classindex);
    return ((double) fp) / ((double) (fp + tn));
  }

  /**
   * Provides the <i>false positive rate</i>. Aka <i>false alarm rate</i>:
   * <code>FP / (FP+TN)</code>.
   * 
   * 
   * @return the false positive rate
   */
  public double falsePositiveRate() {
    double fpr = 0;
    for(int i = 0; i < confusion.length; i++) {
      fpr += falsePositiveRate(i) * colSum(i);
    }
    return fpr / totalInstances();
  }

  /**
   * Provides the <i>positive predicted value</i> for the specified class.
   * 
   * 
   * @param classindex the index of the class to retrieve the positive predicted
   *        value for
   * @return the positive predicted value for the specified class
   */
  public double positivePredictedValue(int classindex) {
    int tp = truePositives(classindex);
    return (double) tp / ((double) (tp + falsePositives(classindex)));
  }

  /**
   * Provides the <i>positive predicted value</i>. Aka <i>precision</i> or
   * <i>specificity</i>: <code>TP / (TP+FP)</code>.
   * 
   * @return the positive predicted value
   */
  public double positivePredictedValue() {
    double ppv = 0;
    for(int i = 0; i < confusion.length; i++) {
      ppv += positivePredictedValue(i) * colSum(i);
    }
    return ppv / totalInstances();
  }

  /**
   * The number of correctly classified instances.
   * 
   * 
   * @return the number of correctly classified instances
   */
  public int truePositives() {
    int tp = 0;
    for(int i = 0; i < confusion.length; i++) {
      tp += truePositives(i);
    }
    return tp;
  }

  /**
   * The number of correctly classified instances belonging to the specified
   * class.
   * 
   * 
   * @param classindex the index of the class to retrieve the correctly
   *        classified instances of
   * @return the number of correctly classified instances belonging to the
   *         specified class
   */
  public int truePositives(int classindex) {
    return confusion[classindex][classindex];
  }

  /**
   * Provides the <i>true positive rate</i> for the specified class.
   * 
   * @param classindex the index of the class to retrieve the true positive rate
   *        for
   * @return the true positive rate
   */
  public double truePositiveRate(int classindex) {
    int tp = truePositives(classindex);
    return (double) tp / ((double) (tp + falseNegatives(classindex)));
  }

  /**
   * The number of true negatives of the specified class.
   * 
   * 
   * @param classindex the index of the class to retrieve the true negatives for
   * @return the number of true negatives of the specified class
   */
  public int trueNegatives(int classindex) {
    int tn = 0;
    for(int i = 0; i < confusion.length; i++) {
      for(int j = 0; j < confusion[i].length; j++) {
        if(i != classindex && j != classindex) {
          tn += confusion[i][j];
        }
      }
    }
    return tn;
  }

  /**
   * The false positives for the specified class.
   * 
   * 
   * @param classindex the index of the class to retrieve the false positives
   *        for
   * @return the false positives for the specified class
   */
  public int falsePositives(int classindex) {
    int fp = 0;
    for(int i = 0; i < confusion[classindex].length; i++) {
      if(i != classindex) {
        fp += confusion[classindex][i];
      }
    }
    return fp;
  }

  /**
   * The false negatives for the specified class.
   * 
   * 
   * @param classindex the index of the class to retrieve the false negatives
   *        for
   * @return the false negatives for the specified class
   */
  public int falseNegatives(int classindex) {
    int fn = 0;
    for(int i = 0; i < confusion.length; i++) {
      if(i != classindex) {
        fn += confusion[i][classindex];
      }
    }
    return fn;
  }

  /**
   * The total number of instances covered by this confusion matrix.
   * 
   * 
   * @return the total number of instances covered by this confusion matrix
   */
  public int totalInstances() {
    int total = 0;
    for(int i = 0; i < confusion.length; i++) {
      for(int j = 0; j < confusion[i].length; j++) {
        total += confusion[i][j];
      }
    }
    return total;
  }

  /**
   * The number of instances present in the specified row. I.e., classified as
   * class <code>classindex</code>.
   * 
   * 
   * @param classindex the index of the class the resulting number of instances
   *        has been classified as
   * @return the number of instances present in the specified row
   */
  public int rowSum(int classindex) {
    int s = 0;
    for(int i = 0; i < confusion[classindex].length; i++) {
      s += confusion[classindex][i];
    }
    return s;
  }

  /**
   * The number of instances present in the specified column. I.e., the
   * instances of class <code>classindex</code>.
   * 
   * 
   * @param classindex the index of the class theresulting number of instances
   *        belongs to
   * @return the number of instances present in the specified column
   */
  public int colSum(int classindex) {
    int s = 0;
    for(int i = 0; i < confusion.length; i++) {
      s += confusion[i][classindex];
    }
    return s;
  }

  /**
   * The number of instances belonging to class <code>trueClassindex</code> and
   * predicted as <code>predictedClassindex</code>.
   * 
   * 
   * @param trueClassindex the true class index
   * @param predictedClassindex the predicted class index
   * @return the number of instances belonging to class
   *         <code>trueClassindex</code> and predicted as
   *         <code>predictedClassindex</code>
   */
  public int value(int trueClassindex, int predictedClassindex) {
    return confusion[predictedClassindex][trueClassindex];
  }

  /**
   * Provides a String representation of this confusion matrix.
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    int max = 0;
    for(int i = 0; i < confusion.length; i++) {
      for(int j = 0; j < confusion[i].length; j++) {
        if(confusion[i][j] > max) {
          max = confusion[i][j];
        }
      }
    }
    String classPrefix = "C_";
    NumberFormat nf = NumberFormat.getInstance();
    nf.setParseIntegerOnly(true);
    int labelLength = Integer.toString(labels.size()).length();
    nf.setMaximumIntegerDigits(labelLength);
    nf.setMinimumIntegerDigits(labelLength);
    int cell = Math.max(Integer.toString(max).length(), labelLength + classPrefix.length());
    String separator = " ";
    StringBuilder representation = new StringBuilder();
    for(int i = 1; i <= labels.size(); i++) {
      representation.append(separator);
      String label = classPrefix + nf.format(i);
      int space = cell - labelLength - classPrefix.length();
      for(int s = 0; s <= space; s++) {
        representation.append(' ');
      }
      representation.append(label);
    }
    representation.append('\n');
    for(int row = 0; row < confusion.length; row++) {
      for(int col = 0; col < confusion[row].length; col++) {
        representation.append(separator);
        String entry = Integer.toString(confusion[row][col]);
        int space = cell - entry.length();
        for(int s = 0; s <= space; s++) {
          representation.append(' ');
        }
        representation.append(entry);
      }
      representation.append(separator);
      representation.append(classPrefix);
      representation.append(nf.format(row + 1));
      representation.append(": ");
      representation.append(labels.get(row));
      representation.append('\n');
    }
    return representation.toString();
  }
}