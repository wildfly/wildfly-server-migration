/*
 * Copyright 2017 Red Hat, Inc.
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

package org.jboss.migration.core.jboss;

import org.jboss.dmr.ModelNode;

import static org.jboss.as.domain.management.ModelDescriptionConstants.*;

/**
 * @author emmartins
 */
public class ResolvablePath {

    private final String path;
    private final String relativeTo;

    public ResolvablePath(String path, String relativeTo) {
        if (path == null && relativeTo == null) {
            throw new IllegalArgumentException();
        }
        this.path = path;
        this.relativeTo = relativeTo;
    }

    public ResolvablePath(ModelNode modelNode) {
        this((modelNode.hasDefined(PATH) ? modelNode.get(PATH).asString() : null), (modelNode.hasDefined(RELATIVE_TO) ? modelNode.get(RELATIVE_TO).asString() : null));
    }

    public String getPath() {
        return path;
    }

    public String getRelativeTo() {
        return relativeTo;
    }

    public static ResolvablePath fromPathExpression(String path) {
        path = path.trim();
        if (path.indexOf("${") == 0) {
            int relativeToEnd = path.indexOf('}');
            if (relativeToEnd < 2) {
                throw new IllegalArgumentException("invalid expression path '"+path+"'");
            }
            int nextStart = path.indexOf("${",2);
            if (nextStart > 0 && nextStart < relativeToEnd) {
                throw new IllegalArgumentException("nested expression path '"+path+"'");
            }
            final String relativeTo = path.substring(2, relativeToEnd);
            if (path.length() > relativeToEnd+1) {
                // path and relative to
                String relativePath = path.substring(relativeToEnd+1);
                if (relativePath.charAt(0) == '/' || relativePath.charAt(0) == '\\') {
                    relativePath = relativePath.substring(1);
                }
                return new ResolvablePath(relativePath, relativeTo);
            } else {
                // just a named path
                return new ResolvablePath(null, relativeTo);
            }
        } else {
            return new ResolvablePath(path, null);
        }
    }

    @Override
    public String toString() {
        return "path='"+String.valueOf(path)+"', relativeTo='"+String.valueOf(relativeTo);
    }
}
