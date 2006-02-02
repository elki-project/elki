package de.lmu.ifi.dbs.featureextraction.image;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

/**
 * Writes the extracted festures to output.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class FeatureArffWriter {
  /**
   * The writer for the color histogram features.
   */
  private BufferedWriter colorHistogramWriter;

  /**
   * The writer for the color moments features.
   */
  private BufferedWriter colorMomentsWriter;

  /**
   * The writers for the 13 texture features of each orientation.
   */
  private BufferedWriter[] textureFeatureWriters = new BufferedWriter[13];

  /**
   * Creates a new FeatureArffWriter and initializes it with the specified parameters.
   *
   * @param outputDir  the name of the output directory
   * @param namePrefix the prefix name for all feature files
   * @param classIDs   the class ids of the images
   * @throws IOException
   */
  FeatureArffWriter(String outputDir, String namePrefix, Set<Integer> classIDs) throws IOException {
    String classIDsString = classIDs.toString();
    classIDsString = classIDsString.substring(1, classIDsString.length() - 1);

    // color histogram
    String dirName = outputDir + File.separator + ImageDescriptor.featureNames[0] + File.separator;
    File dir = new File(dirName);
    if (!dir.exists()) {
      dir.mkdir();
    }
    colorHistogramWriter = new BufferedWriter(new FileWriter(dirName + namePrefix + ".arff"));
    writeHeader(colorHistogramWriter, namePrefix, ImageDescriptor.featureNames[0],
                ImageDescriptor.numAttributes[0], classIDsString);

    // color moments
    dirName = outputDir + File.separator + ImageDescriptor.featureNames[1] + File.separator;
    dir = new File(dirName);
    if (!dir.exists()) {
      dir.mkdir();
    }
    colorMomentsWriter = new BufferedWriter(new FileWriter(dirName + namePrefix + ".arff"));
    writeHeader(colorMomentsWriter, namePrefix, ImageDescriptor.featureNames[1],
                ImageDescriptor.numAttributes[1], classIDsString);

    // texture features
    for (int i = 0; i < textureFeatureWriters.length; i++) {
      // parent directory
      dirName = outputDir + File.separator + ImageDescriptor.featureNames[i + 2] +
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
                  ImageDescriptor.featureNames[i + 2],
                  ImageDescriptor.numAttributes[i + 2], classIDsString);
    }
  }

  /**
   * Flush the streams.
   */
  void flush() throws IOException {
    colorHistogramWriter.flush();
    colorMomentsWriter.flush();
    for (BufferedWriter textureFeatureWriter : textureFeatureWriters) {
      textureFeatureWriter.flush();
    }
  }

  /**
   * Close the streams.
   *
   * @throws IOException
   */
  void close() throws IOException {
    colorHistogramWriter.flush();
    colorHistogramWriter.close();
    colorMomentsWriter.flush();
    colorMomentsWriter.close();

    for (BufferedWriter textureFeatureWriter : textureFeatureWriters) {
      textureFeatureWriter.flush();
      textureFeatureWriter.close();
    }
  }

  /**
   * Writes the features of the specified image descriptor to output.
   *
   * @param descriptor the descriptor holding the features
   */
  void writeFeatures(ImageDescriptor descriptor) throws IOException {
    // color histogram
    descriptor.writeColorHistogram(colorHistogramWriter);
    // color moments
    descriptor.writeColorMoments(colorMomentsWriter);
    // texture features
    descriptor.writeTextureFeatures(textureFeatureWriters);
  }

  /**
   * @see java.lang.Object#finalize()
   */
  protected void finalize() throws Throwable {
    close();
    super.finalize();
  }

  /**
   * Writes the header of the arff-file
   *
   * @param writer        the writer to write on
   * @param namePrefix    the prefix of the realtion name
   * @param featureName   the name of the feature to be written
   * @param numAttributes the number of attributes of the feature
   * @param classIDs      the ids of the classes
   * @throws IOException
   */
  private void writeHeader(BufferedWriter writer, String namePrefix,
                           String featureName, int numAttributes, String classIDs) throws IOException {

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
