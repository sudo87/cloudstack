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

package org.apache.cloudstack.api.command.admin.network;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkRateLimitResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.network.NetworkRateLimitService;


@APICommand(name = "listNetworkRateLimits",
        description = "Lists the effective network rate limits on interfaces of Networks, VPCs, " +
                "Virtual Routers and Virtual Machines. Admin only.",
        responseObject = NetworkRateLimitResponse.class,
        authorized = {RoleType.Admin},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.21.0")
public class ListNetworkRateLimitsCmd extends BaseListCmd {

    // ── Parameters ────────────────────────────────────────────────────────────

    @Parameter(name = "type",
            type = CommandType.STRING,
            description = "Filter by resource type. Allowed values: vm, network, vpc, vr. " +
                    "Leave empty to list all types.")
    private String type;

    @Parameter(name = ApiConstants.NETWORK_ID,
            type = CommandType.UUID,
            entityType = NetworkResponse.class,
            description = "UUID of a specific network to query (optional)")
    private Long networkId;

    @Parameter(name = ApiConstants.VPC_ID,
            type = CommandType.UUID,
            entityType = VpcResponse.class,
            description = "UUID of a specific VPC to query (optional)")
    private Long vpcId;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.UUID,
            entityType = UserVmResponse.class,
            description = "UUID of a specific VM to query (optional)")
    private Long vmId;

    // ── Service ───────────────────────────────────────────────────────────────

    @Inject
    NetworkRateLimitService networkRateLimitService;

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String getType() {
        return type;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public Long getVmId() {
        return vmId;
    }

    // ── Execution ─────────────────────────────────────────────────────────────

    @Override
    public void execute() {
        List<NetworkRateLimitResponse> entries = networkRateLimitService.listNetworkRateLimits(this);
        ListResponse<NetworkRateLimitResponse> response = new ListResponse<>();
        response.setResponses(entries, entries.size());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
