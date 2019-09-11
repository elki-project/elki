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
package elki.application.lucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import elki.application.AbstractApplication;
import elki.utilities.exceptions.AbortException;
import elki.utilities.io.FileUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Simple text loader, assuming a folder with plain text files.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SimpleTextLoader extends AbstractApplication {
  /**
   * Index and source folders.
   */
  File index, source;

  /**
   * Constructor.
   *
   * @param index Index folder
   * @param source Source folder
   */
  public SimpleTextLoader(File index, File source) {
    super();
    this.index = index;
    this.source = source;
  }

  @Override
  public void run() {
    try {
      final Directory dir = FSDirectory.open(index);
      IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, new StandardAnalyzer(Version.LUCENE_36));
      IndexWriter writer = new IndexWriter(dir, config);

      final URI suri = source.toURI();
      for (File inf : source.listFiles()) {
        Document doc = new Document();
        String id = suri.relativize(inf.toURI()).getPath();
        String text = FileUtil.slurp(new FileInputStream(inf));
        doc.add(new Field("id", id, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("contents", text, Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);
      }
      writer.close();
    } catch (IOException e) {
      throw new AbortException("I/O error in lucene.", e);
    }
  }

  public static class Par extends AbstractApplication.Par {
    /**
     * Parameter for the index to build.
     */
    public static final OptionID INDEX_ID = new OptionID("lucene.index", "Lucene index folder.");

    /**
     * Parameter for the source folder.
     */
    public static final OptionID SOURCE_ID = new OptionID("lucene.source", "Source file folder.");

    File index, source;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new FileParameter(SOURCE_ID, FileParameter.FileType.INPUT_FILE) //
          .grab(config, x -> source = x);
      new FileParameter(INDEX_ID, FileParameter.FileType.OUTPUT_FILE) //
          .grab(config, x -> index = x);
    }

    @Override
    public SimpleTextLoader make() {
      return new SimpleTextLoader(index, source);
    }
  }

  /**
   * Main method.
   *
   * @param args Command line parameters.
   */
  public static void main(String[] args) {
    runCLIApplication(SimpleTextLoader.class, args);
  }
}
