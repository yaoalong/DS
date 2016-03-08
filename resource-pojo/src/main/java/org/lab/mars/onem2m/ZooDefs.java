package org.lab.mars.onem2m;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

public class ZooDefs {
    final public static String[] opNames = { "notification", "create",
            "delete", "exists", "getData", "setData", "getACL", "setACL",
            "getChildren", "getChildren2", "getMaxChildren", "setMaxChildren",
            "ping" };

    public interface OpCode {
        public final int notification = 0;

        public final int create = 1;

        public final int delete = 2;

        public final int getData = 4;

        public final int setData = 5;

        public final int sync = 9;

        public final int error = -1;
    }

    public interface Perms {
        int READ = 1 << 0;

        int WRITE = 1 << 1;

        int CREATE = 1 << 2;

        int DELETE = 1 << 3;

        int ADMIN = 1 << 4;

        int ALL = READ | WRITE | CREATE | DELETE | ADMIN;
    }

    public interface Ids {

    }
}
