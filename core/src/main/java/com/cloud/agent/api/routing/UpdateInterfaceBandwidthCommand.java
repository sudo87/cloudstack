// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.agent.api.routing;

import com.cloud.agent.api.Command;

/**
 * Sent to a Virtual Router to dynamically apply bandwidth shaping on a
 * named network interface using Linux tc (traffic control). A value of 0
 * or negative for ingressMbps / egressMbps removes an existing limit.
 */
public class UpdateInterfaceBandwidthCommand extends Command {

    /** The interface IP used to identify which VR interface to shape. */
    private String interfaceIp;

    /**
     * Traffic type of the interface: "Public" or "Guest".
     * Informational — the VR script resolves the actual interface name from the IP.
     */
    private String trafficType;

    /** Ingress (inbound) rate limit in Mbps; &lt;= 0 means no limit. */
    private int ingressMbps;

    /** Egress (outbound) rate limit in Mbps; &lt;= 0 means no limit. */
    private int egressMbps;

    protected UpdateInterfaceBandwidthCommand() {
        // For serialization
    }

    public UpdateInterfaceBandwidthCommand(String interfaceIp, String trafficType, int ingressMbps, int egressMbps) {
        this.interfaceIp = interfaceIp;
        this.trafficType = trafficType;
        this.ingressMbps = ingressMbps;
        this.egressMbps = egressMbps;
    }

    public String getInterfaceIp() {
        return interfaceIp;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public int getIngressMbps() {
        return ingressMbps;
    }

    public int getEgressMbps() {
        return egressMbps;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
