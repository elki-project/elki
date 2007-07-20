package de.lmu.ifi.dbs.featureextraction.image;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Writes the extracted festures in txt format to output.
 * 
 * @author Elke Achtert 
 */
class FeatureTxtWriter extends FeatureWriter
{

    /**
     * Creates a new FeatureTxtWriter and initializes it with the specified
     * parameters.
     * 
     * @param outputDir
     *            the name of the output directory
     * @param classIDs
     *            a string representation of the class ids of the images
     * @throws java.io.IOException
     */
    FeatureTxtWriter(DescriptorInfo[] descInfo, String outputDir, String classIDs) throws IOException
    {
   	  super(descInfo, outputDir, classIDs, ".txt");

   	  for (int i = 0; i < descInfo.length; i++)
        {
            writeHeader(featureWriters[i], descInfo[i].name, descInfo[i].data.length);
        }
    }

    /**
     * Writes the header of the arff-file
     * 
     * @param writer
     *            the writer to write on
     * @param featureName
     *            the name of the feature to be written
     * @param numAttributes
     *            the number of attributes of the feature
     * @throws java.io.IOException
     */
    private void writeHeader(BufferedWriter writer, String featureName,
            int numAttributes) throws IOException
    {

        writer.write("#########################################################");
        writer.newLine();
        writer.write("### Feature " + featureName);
        writer.newLine();
        writer.write("### attribute id string");
        writer.newLine();
        for (int j = 0; j < numAttributes; j++)
        {
            writer.write("### attribute d" + j + " numeric");
            writer.newLine();
        }
        writer.write("### attribute class {" + classIDs + "}");
        writer.newLine();
        writer.write("#########################################################");
        writer.newLine();
        writer.newLine();
    }

}
