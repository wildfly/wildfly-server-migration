/*
 * Copyright 2022 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.migration.core.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Convenience operations related to text Files.
 * @author emmartins
 */
public interface TextFiles {
    /**
     * Reads a text file.
     * @param path the text file's path
     * @return the text file as a String
     * @throws IOException
     */
    static String read(Path path) throws IOException {
        Objects.requireNonNull(path);
        final byte[] ba = Files.readAllBytes(path);
        return new String(ba);
    }
    /**
     * Writes a text file.
     * @param path the text file's path
     * @param content  the text file's content to write
     * @throws IOException
     */
    static void write(Path path, String content) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(content);
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
