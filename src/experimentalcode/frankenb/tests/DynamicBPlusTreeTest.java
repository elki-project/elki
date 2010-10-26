/**
 * 
 */
package experimentalcode.frankenb.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.Test;

import experimentalcode.frankenb.model.ConstantSizeIntegerSerializer;
import experimentalcode.frankenb.model.DynamicBPlusTree;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class DynamicBPlusTreeTest {

  /**
   */
  @Test
  public void testPutGet() throws IOException {
    DynamicBPlusTree<Integer, String> bPlusTree = null;
    try {

      File directoryFile = File.createTempFile("bplustreetest", "dir");
      File dataFile = File.createTempFile("bplustreetest", "dat");
      
      directoryFile.deleteOnExit();
      dataFile.deleteOnExit();
      
      bPlusTree = new DynamicBPlusTree<Integer, String> (
          directoryFile,
          dataFile,
          new ConstantSizeIntegerSerializer(),
          new StringSerializer(),
          50
      );
      
      Random random = new Random(System.currentTimeMillis());
      Map<Integer, String> entries = new HashMap<Integer, String>();
      for (int i = 0; i < 1000; ++i) {
        int key = random.nextInt(10000);
        
        StringBuilder sb = new StringBuilder();
        int testStringLength = 5 + random.nextInt(100);
        for (int j = 0; j < testStringLength; ++j) {
          boolean lowerCase = random.nextBoolean();
          char c = (char)(97 + random.nextInt(26));
          sb.append((lowerCase ? Character.toLowerCase(c) : Character.toUpperCase(c)));
        }
        
        String value = sb.toString();
        
        bPlusTree.put(key, value);
        entries.put(key, value);
      }
      
      assertEquals(entries.size(), bPlusTree.getSize());
      
      for (Entry<Integer, String> entry : entries.entrySet()) {
        String result = bPlusTree.get(entry.getKey());
        assertEquals(entry.getValue(), result);
      }
      
    } finally {
      if (bPlusTree != null)
        bPlusTree.close();    
    }
  }


}
