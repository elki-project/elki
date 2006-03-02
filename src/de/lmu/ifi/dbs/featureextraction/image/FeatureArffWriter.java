package de.lmu.ifi.dbs.featureextraction.image;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes the extracted festures in arff format to output.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
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
  FeatureArffWriter(String outputDir, String namePrefix, String classIDs) throws IOException {
    super(classIDs);

    // color histogram
    String dirName = outputDir + File.separator + ImageDescriptor.featureNames[0] + File.separator;
    File dir = new File(dirName);
    if (!dir.exists()) {
      dir.mkdir();
    }
    colorHistogramWriter = new BufferedWriter(new FileWriter(dirName + namePrefix + ".arff"));
    writeHeader(colorHistogramWriter, namePrefix, ImageDescriptor.featureNames[0],
                ImageDescriptor.numAttributes[0]);

    // color moments
    for (int i = 0; i < colorMomentsWriters.length; i++) {
      // parent directory
      dirName = outputDir + File.separator + ImageDescriptor.featureNames[i + 1] +
                File.separator;
      dir = new File(dirName);
      if (!dir.exists()) {
        dir.mkdir();
      }

      dirName += File.separator;
      dir = new File(dirName);
      if (!dir.exists()) {
        dir.mkdir();
      }

      colorMomentsWriters[i] = new BufferedWriter(new FileWriter(dirName + namePrefix + ".arff"));
      writeHeader(colorMomentsWriters[i], namePrefix,
                  ImageDescriptor.featureNames[i + 1],
                  ImageDescriptor.numAttributes[i + 1]);
    }

    // texture features
    for (int i = 0; i < textureFeatureWriters.length; i++) {
      // parent directory
      dirName = outputDir + File.separator + ImageDescriptor.featureNames[i + 4] +
                File.separator;
      dir = new File(dirName);
      if (!dir.exists()) {
        dir.mkdir();
      }

      dirName += File.separator;
      dir = new File(dirName);
      if (!dir.exists()) {
        dir.mkdir();
      }

      textureFeatureWriters[i] = new BufferedWriter(new FileWriter(dirName + namePrefix + ".arff"));
      writeHeader(textureFeatureWriters[i], namePrefix,
                  ImageDescriptor.featureNames[i + 4],
                  ImageDescriptor.numAttributes[i + 4]);
    }
    
    // roughness statictics
    for (int i = 0; i < roughnessStatsWriters.length; i++) {
      // parent directory
      dirName = outputDir + File.separator + ImageDescriptor.featureNames[i + 17] +
                File.separator;
      dir = new File(dirName);
      if (!dir.exists()) {
        dir.mkdir();
      }

      dirName += File.separator;
      dir = new File(dirName);
      if (!dir.exists()) {
        dir.mkdir();
      }

      roughnessStatsWriters[i] = new BufferedWriter(new FileWriter(dirName + namePrefix + ".arff"));
      writeHeader(roughnessStatsWriters[i], namePrefix,
                  ImageDescriptor.featureNames[i + 17],
                  ImageDescriptor.numAttributes[i + 17]);
    }
    
    // facet-orientation statictics
    for (int i = 0; i < facetStatsWriters.length; i++) {
      // parent directory
      dirName = outputDir + File.separator + ImageDescriptor.featureNames[i + 25] +
                File.separator;
      dir = new File(dirName);
      if (!dir.exists()) {
        dir.mkdir();
      }

      dirName += File.separator;
      dir = new File(dirName);
      if (!dir.exists()) {
        dir.mkdir();
      }

      facetStatsWriters[i] = new BufferedWriter(new FileWriter(dirName + namePrefix + ".arff"));
      writeHeader(facetStatsWriters[i], namePrefix,
                  ImageDescriptor.featureNames[i + 25],
                  ImageDescriptor.numAttributes[i + 25]);
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
