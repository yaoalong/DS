/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lab.mars.onem2m;

import org.lab.mars.onem2m.jute.M2mRecord;

public abstract class Op {
    private int type;
    private String path;

    // prevent untyped construction
    private Op(int type, String path) {
        this.type = type;
        this.path = path;
    }

    /**
     * Gets the integer type code for an Op. This code should be as from
     * ZooDefs.OpCode
     *
     * @return The type code.
     * @see ZooDefs.OpCode
     */
    public int getType() {
        return type;
    }

    /**
     * Gets the path for an Op.
     *
     * @return The path.
     */
    public String getPath() {
        return path;
    }

    /**
     * Encodes an op for wire transmission.
     *
     * @return An appropriate Record structure.
     */
    public abstract M2mRecord toRequestRecord();

    /**
     * Reconstructs the transaction with the chroot prefix.
     *
     * @return transaction with chroot.
     */
    abstract Op withChroot(String addRootPrefix);

    /**
     * Performs client path validations.
     *
     * @throws IllegalArgumentException
     *             if an invalid path is specified
     * @throws KeeperException.BadArgumentsException
     *             if an invalid create mode flag is specified
     */
    void validate() throws KeeperException {
    }

    // ////////////////
    // these internal classes are public, but should not generally be
    // referenced.
    //

}
