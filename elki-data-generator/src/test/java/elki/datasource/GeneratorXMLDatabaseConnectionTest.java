/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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
package elki.datasource;

import static org.junit.Assert.assertNotNull;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.data.type.TypeUtil;
import elki.database.AbstractDatabase;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.utilities.ELKIBuilder;

/**
 * Regression test of the data generator.
 * 
 * @author Erich Schubert
 */
public class GeneratorXMLDatabaseConnectionTest {
  @Test
  public void testGenerator() throws URISyntaxException {
    String fn = "elki/testdata/unittests/3clusters-and-noise-2d.xml";
    Path res = Paths.get(AbstractSimpleAlgorithmTest.class.getClassLoader().getResource(fn).toURI());
    Database db = new ELKIBuilder<>(StaticArrayDatabase.class) //
        .with(AbstractDatabase.Par.DATABASE_CONNECTION_ID, GeneratorXMLDatabaseConnection.class) //
        .with(GeneratorXMLDatabaseConnection.Par.CONFIGFILE_ID, res) //
        .build();
    db.initialize();
    assertNotNull(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
  }
}
