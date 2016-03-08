/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lab.mars.onem2m.server.util;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.server.DSDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SerializeUtils {
    private static final Logger LOG = LoggerFactory
            .getLogger(SerializeUtils.class);


    public static void deserializeSnapshot(DSDatabase zkDatabase, M2mInputArchive ia)
            throws IOException {
        zkDatabase.deserialize(ia, "m2mData");
    }

    public static void serializeSnapshot(Long peerLast, DSDatabase zkDatabase,
                                         M2mOutputArchive oa) throws IOException {

        zkDatabase.serialize(peerLast, oa, "m2mData");
    }

}
