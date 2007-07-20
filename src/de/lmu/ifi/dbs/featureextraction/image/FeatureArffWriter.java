package de.lmu.ifi.dbs.featureextraction.image;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Writes the extracted festures in arff format to output.
 *
 * @author Elke Achtert 
 */
class FeatureArffWriter extends FeatureWriter {

  /**
   * Creates a new FeatureArffWriter and initializes it with the specified parameters.
   *
   * @param outputDir  the name of the output directory
   * @param namePrefix the prefix name for all feature files
   * @param classIDs   a string representation of the class ids of the images
   * @throws IOException
   */
  FeatureArffWriter(DescriptorInfo[] descInfo, String outputDir, String namePrefix, String classIDs) throws IOException {
    super(descInfo, outputDir, classIDs, ".arff");
    
    for (int i = 0; i < descInfo.length; i++) {
      writeHeader(featureWriters[i], namePrefix, descInfo[i].name, descInfo[i].data.length);
    }
  }

  /**
   * Writes the header of the arff-file
   *
   * @param writer        the writer to write on
   * @param namePrefix    the prefix of the realtion name
   * @param featureName   the name of the feature to be written
   * @param numAttributes the number of attributes of the feature
   * @throws IOException
   */
  private void writeHeader(BufferedWriter writer, String namePrefix,
                           String featureName, int numAttributes) throws IOException {

    writer.write("@relation " + namePrefix + "_" + featureName);
    writer.newLine();
    writer.write("@attribute id string");
    writer.newLine();
    for (int j = 0; j < numAttributes; j++) {
      writer.write("@attribute d" + j + " numeric");
      writer.newLine();
    }
    writer.write("@attribute class {" + classIDs + "}");
    writer.newLine();
    writer.newLine();
    writer.write("@data");
    writer.newLine();
  }

}
