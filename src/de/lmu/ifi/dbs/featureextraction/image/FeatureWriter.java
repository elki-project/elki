package de.lmu.ifi.dbs.featureextraction.image;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Writes the extracted festures to output.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
abstract class FeatureWriter {
  /**
   * The writer for the color histogram features.
   */
  BufferedWriter colorHistogramWriter;

  /**
   * The writer for the color moments features.
   */
  BufferedWriter colorMomentsWriter;

  /**
   * The writers for the 13 texture features of each orientation.
   */
  BufferedWriter[] textureFeatureWriters = new BufferedWriter[13];

  /**
   * A string representation of the class ids of the images.
   */
  String classIDs;

  /**
   * Creates a new FeatureTxtWriter and initializes it with the specified parameters.
   *
   * @param classIDs   a string representation of the class ids of the images
   */
  FeatureWriter(String classIDs) {
    this.classIDs = classIDs;
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
   * @throws java.io.IOException
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
  void writeFeatures(String separator, String classPrefix, ImageDescriptor descriptor) throws IOException {
    // color histogram
    descriptor.writeColorHistogram(separator, classPrefix, colorHistogramWriter);
    // color moments
    descriptor.writeColorMoments(separator, classPrefix, colorMomentsWriter);
    // texture features
    descriptor.writeTextureFeatures(separator, classPrefix, textureFeatureWriters);
  }

  /**
   * @see Object#finalize()
   */
  protected void finalize() throws Throwable {
    close();
    super.finalize();
  }
}
