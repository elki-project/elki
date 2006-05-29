package de.lmu.ifi.dbs.featureextraction.image;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes the extracted festures to output.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
abstract class FeatureWriter
{   
    /**
     * The name of the output directory
     */
    protected String outputDir;
    
    /**
     * The file extension (e.g. ".txt")
     */
    protected String fileExtension;
    
    /**
     * The prefix name for all feature files.
     */
    protected String namePrefix;
    
    /**
     * A string representation of the class ids.
     */
    protected String classIDs;
    
    /**
     * The writers for the features.
     */
    protected BufferedWriter[] featureWriters;

    /**
     * Creates a new FeatureWriter and initializes it with the specified
     * parameters.
     * 
     * @param descInfo
     *            an array of feature descriptors
     * @param classIDs
     *            a string representation of the class ids of the images
     */
    public FeatureWriter(DescriptorInfo[] descInfo, String outputDir, String classIDs, String fileExtension) throws IOException
    {
   	  this.outputDir = outputDir;
        this.classIDs = classIDs;
        this.fileExtension = fileExtension;
        
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
        
   	  featureWriters = new BufferedWriter[descInfo.length];
  	     for(int i = 0; i < descInfo.length; i++)
        {
  	   	   featureWriters[i] = new BufferedWriter(new FileWriter(outputDir + File.separator + descInfo[i].name + fileExtension));
        }
    }

    /**
     * Flush the streams.
     */
    public void flush() throws IOException
    {
        for (BufferedWriter writer : featureWriters)
        {
            writer.flush();
        }
    }

    /**
     * Close the streams.
     * 
     * @throws java.io.IOException
     */
    public void close() throws IOException
    {
        for (BufferedWriter writer : featureWriters)
        {
      	   writer.flush();
      	   writer.close();
        }
    }

    /**
     * Writes the features to output.
     * 
     * @param fileName
     *            the name of the underlying file
     * @param classID
     *            the class id of the underlying file
     */
    public void writeFeatures(DescriptorInfo[] descInfo, String fileName, Integer classID, String separator, String classPrefix) throws IOException
    {
   	  for(int i = 0; i < descInfo.length; i++)
   	  {
   		   writeFeature(descInfo[i].data, fileName, classID, separator, classPrefix, featureWriters[i]);
   	  }
    }
    
    /**
     * Writes the specified feature to output.
     *
     * @param feature     the feature to write
     * @param separator   the separator between the single attributes (e.g. comma or whitespace)
     * @param classPrefix the prefix for the class label
     * @param writer      the writer to write to
     * @throws IOException
     */
    private void writeFeature(double[] feature, String fileName, Integer classID, String separator, String classPrefix, BufferedWriter writer) throws IOException
    {
      writer.write(fileName);
      for (double f : feature)
      {
        writer.write(separator);
        writer.write(String.valueOf(f));
      }
      writer.write(separator + classPrefix + classID);
      writer.newLine();
    }

    /**
     * @see Object#finalize()
     */
    protected void finalize() throws Throwable
    {
        close();
        super.finalize();
    }
}
