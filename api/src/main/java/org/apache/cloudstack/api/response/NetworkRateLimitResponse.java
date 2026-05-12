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

package org.apache.cloudstack.api.response;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

/**
 * One entry in the listNetworkRateLimits response. Describes the effective
 * bandwidth limit on a specific interface of a network resource.
 */
public class NetworkRateLimitResponse extends BaseResponse {

    @SerializedName("resourcetype")
    @Param(description = "Type of the throttled resource: VM, Network, VPC, or VR")
    private String resourceType;

    @SerializedName(ApiConstants.ID)
    @Param(description = "UUID of the resource")
    private String resourceId;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the resource")
    private String resourceName;

    @SerializedName("interfaceip")
    @Param(description = "IP address of the interface on which bandwidth shaping is applied")
    private String interfaceIp;

    @SerializedName(ApiConstants.TRAFFIC_TYPE)
    @Param(description = "Traffic type of the interface: Guest or Public")
    private String trafficType;

    @SerializedName("ingressratemb")
    @Param(description = "Inbound rate limit in Mbps (from DB); null = no limit")
    private Integer ingressRateMb;

    @SerializedName("egressratemb")
    @Param(description = "Outbound rate limit in Mbps (from DB); null = no limit")
    private Integer egressRateMb;

    @SerializedName("livevalue")
    @Param(description = "True if the value was obtained by querying the hypervisor live; false if DB-only")
    private Boolean liveValue;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getInterfaceIp() {
        return interfaceIp;
    }

    public void setInterfaceIp(String interfaceIp) {
        this.interfaceIp = interfaceIp;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public Integer getIngressRateMb() {
        return ingressRateMb;
    }

    public void setIngressRateMb(Integer ingressRateMb) {
        this.ingressRateMb = ingressRateMb;
    }

    public Integer getEgressRateMb() {
        return egressRateMb;
    }

    public void setEgressRateMb(Integer egressRateMb) {
        this.egressRateMb = egressRateMb;
    }

    public Boolean getLiveValue() {
        return liveValue;
    }

    public void setLiveValue(Boolean liveValue) {
        this.liveValue = liveValue;
    }
}
