/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.dboe.sys;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.dboe.DBOpEnvException;
import org.apache.jena.dboe.base.file.Location;

public class IO_DB {
    // Location == org.apache.jena.dboe.base.file.Location
    
    /** Convert a {@link Path}  to a {@link Location}. */
    public static Location asLocation(Path path) {
        Objects.requireNonNull(path, "IOX.asLocation(null)");
        if ( ! Files.isDirectory(path) )
            throw new RuntimeIOException("Path is not naming a directory: "+path);
        return Location.create(path.toString());
    }

    /** Convert a {@link Location} to a {@link Path}. */
    public static Path asPath(Location location) {
        if ( location.isMem() )
            throw new RuntimeIOException("Location is a memory location: "+location);
        return Paths.get(location.getDirectoryPath());
    }

    /** Convert a {@link Location} to a {@link File}. */
    public static File asFile(org.apache.jena.dboe.base.file.Location loc) {
        return new File(loc.getDirectoryPath());
    }

    /** Find the files in this directory that have namebase as a prefix and
     *  are then numbered.
     *  <p>
     *  Returns a sorted list from, low to high index.
     */
    public static List<Path> scanForDirByPattern(Path directory, String namebase, String nameSep) {
        Pattern pattern = Pattern.compile(Pattern.quote(namebase)+
                                          Pattern.quote(nameSep)+
                                          "[\\d]+");
        List<Path> paths = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, namebase + "*")) {
            for ( Path entry : stream ) {
                if ( !pattern.matcher(entry.getFileName().toString()).matches() ) {
                    throw new DBOpEnvException("Invalid filename for matching: "+entry.getFileName());
                    // Alternative: Skip bad trailing parts but more likely there is a naming problem.
                    //   LOG.warn("Invalid filename for matching: {} skipped", entry.getFileName());
                    //   continue;
                }
                // Follows symbolic links.
                if ( !Files.isDirectory(entry) )
                    throw new DBOpEnvException("Not a directory: "+entry);
                paths.add(entry);
            }
        }
        catch (IOException ex) {
            FmtLog.warn(IO_DB.class, "Can't inspect directory: (%s, %s)", directory, namebase);
            throw new DBOpEnvException(ex);
        }
        Comparator<Path> comp = (f1, f2) -> {
            int num1 = extractIndex(f1.getFileName().toString(), namebase, nameSep);
            int num2 = extractIndex(f2.getFileName().toString(), namebase, nameSep);
            return Integer.compare(num1, num2);
        };
        paths.sort(comp);
        //indexes.sort(Long::compareTo);
        return paths;
    }

    /** Given a filename in "base-NNNN" format, return the value of NNNN */
    public static int extractIndex(String name, String namebase, String nameSep) {
        int i = namebase.length()+nameSep.length();
        String numStr = name.substring(i);
        int num = Integer.parseInt(numStr);
        return num;
    }
}
