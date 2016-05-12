// File generated by hadoop record compiler. Do not edit.
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

package org.lab.mars.onem2m.server.quorum;

import org.lab.mars.onem2m.jute.*;

public class M2mLearnerInfo implements M2mRecord {
    /**
     *
     */
    private static final long serialVersionUID = -8382915221680967079L;
    private long serverid;
    private int protocolVersion;

    public M2mLearnerInfo() {
    }

    public M2mLearnerInfo(long serverid, int protocolVersion) {
        this.serverid = serverid;
        this.protocolVersion = protocolVersion;
    }

    public long getServerid() {
        return serverid;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void serialize(M2mOutputArchive a_, String tag)
            throws java.io.IOException {
        a_.startRecord(this, tag);
        a_.writeLong(serverid, "serverid");
        a_.writeInt(protocolVersion, "protocolVersion");
        a_.endRecord(this, tag);
    }

    public void deserialize(M2mInputArchive a_, String tag)
            throws java.io.IOException {
        a_.startRecord(tag);
        serverid = a_.readLong("serverid");
        protocolVersion = a_.readInt("protocolVersion");
        a_.endRecord(tag);
    }

    public void write(java.io.DataOutput out) throws java.io.IOException {
        M2mBinaryOutputArchive archive = new M2mBinaryOutputArchive(out);
        serialize(archive, "");
    }

    public void readFields(java.io.DataInput in) throws java.io.IOException {
        M2mBinaryInputArchive archive = new M2mBinaryInputArchive(in);
        deserialize(archive, "");
    }


}
