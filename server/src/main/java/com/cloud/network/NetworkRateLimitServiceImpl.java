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

package com.cloud.network;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.admin.network.ListNetworkRateLimitsCmd;
import org.apache.cloudstack.api.response.NetworkRateLimitResponse;
import org.apache.cloudstack.network.NetworkRateLimitService;
import org.springframework.stereotype.Component;

import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.network.Networks.TrafficType;

/**
 * DB-only implementation of {@link NetworkRateLimitService}.
 * <p>
 * Reads bandwidth limits from:
 * <ol>
 *   <li>Per-network dynamic override stored in {@code network_details} (key: {@code network.rate.mbps}).</li>
 *   <li>VPC instance override stored in {@code vpc.public_network_rate}.</li>
 *   <li>VPC offering {@code public_network_rate} as fallback for VPC public interfaces.</li>
 *   <li>Network offering {@code nw_rate} as fallback for guest-tier interfaces.</li>
 * </ol>
 * Live-querying from the hypervisor / VR is a planned phase-2 addition;
 * {@link NetworkRateLimitResponse#getLiveValue()} is always {@code false} in this release.
 */
@Component
public class NetworkRateLimitServiceImpl implements NetworkRateLimitService {

    @Inject
    NetworkDao networksDao;

    @Inject
    NetworkDetailsDao networkDetailsDao;

    @Inject
    VpcDao vpcDao;

    @Inject
    VpcOfferingDao vpcOfferingDao;

    @Inject
    DomainRouterDao routerDao;

    @Inject
    NicDao nicDao;

    @Inject
    VMInstanceDao vmInstanceDao;

    @Inject
    NetworkModel networkModel;

    @Override
    public List<NetworkRateLimitResponse> listNetworkRateLimits(ListNetworkRateLimitsCmd cmd) {
        List<NetworkRateLimitResponse> results = new ArrayList<>();

        String type = cmd.getType();
        Long filterNetworkId = cmd.getNetworkId();
        Long filterVpcId = cmd.getVpcId();
        Long filterVmId = cmd.getVmId();

        boolean doNetwork = (type == null || "network".equalsIgnoreCase(type));
        boolean doVpc = (type == null || "vpc".equalsIgnoreCase(type));
        boolean doVr = (type == null || "vr".equalsIgnoreCase(type));
        boolean doVm = (type == null || "vm".equalsIgnoreCase(type));

        // ── Networks (guest-tier interface on VR) ────────────────────────────
        if (doNetwork) {
            List<NetworkVO> networks = new ArrayList<>();
            if (filterNetworkId != null) {
                NetworkVO nw = networksDao.findById(filterNetworkId);
                if (nw != null) networks.add(nw);
            } else if (filterVpcId != null) {
                networks.addAll(networksDao.listByVpc(filterVpcId));
            } else {
                networks.addAll(networksDao.listAll());
            }
            for (NetworkVO nw : networks) {
                if (nw.getTrafficType() != TrafficType.Guest) continue;
                NetworkRateLimitResponse entry = new NetworkRateLimitResponse();
                entry.setResourceType("Network");
                entry.setResourceId(nw.getUuid());
                entry.setResourceName(nw.getName());
                entry.setTrafficType(TrafficType.Guest.name());
                entry.setLiveValue(false);
                Integer rate = resolveGuestNetworkRate(nw);
                entry.setIngressRateMb(rate);
                entry.setEgressRateMb(rate);
                results.add(entry);
            }
        }

        // ── VPCs (public interface of VR) ────────────────────────────────────
        if (doVpc) {
            List<VpcVO> vpcs = new ArrayList<>();
            if (filterVpcId != null) {
                VpcVO vpc = vpcDao.findById(filterVpcId);
                if (vpc != null) vpcs.add(vpc);
            } else {
                vpcs.addAll(vpcDao.listAll());
            }
            for (VpcVO vpc : vpcs) {
                NetworkRateLimitResponse entry = new NetworkRateLimitResponse();
                entry.setResourceType("VPC");
                entry.setResourceId(vpc.getUuid());
                entry.setResourceName(vpc.getName());
                entry.setTrafficType(TrafficType.Public.name());
                entry.setLiveValue(false);
                Integer rate = resolveVpcPublicRate(vpc);
                entry.setIngressRateMb(rate);
                entry.setEgressRateMb(rate);
                results.add(entry);
            }
        }

        // ── Virtual Routers (per-NIC view) ───────────────────────────────────
        if (doVr) {
            List<DomainRouterVO> routers = routerDao.listAll();
            for (DomainRouterVO router : routers) {
                List<NicVO> nics = nicDao.listByVmId(router.getId());
                for (NicVO nic : nics) {
                    NetworkVO nw = networksDao.findById(nic.getNetworkId());
                    if (nw == null) continue;
                    TrafficType tt = nw.getTrafficType();
                    if (tt != TrafficType.Guest && tt != TrafficType.Public) continue;

                    NetworkRateLimitResponse entry = new NetworkRateLimitResponse();
                    entry.setResourceType("VR");
                    entry.setResourceId(router.getUuid());
                    entry.setResourceName(router.getInstanceName());
                    entry.setInterfaceIp(nic.getIPv4Address());
                    entry.setTrafficType(tt.name());
                    entry.setLiveValue(false);

                    Integer rate = networkModel.getNetworkRate(nw.getId(), router.getId());
                    entry.setIngressRateMb(rate != null && rate > 0 ? rate : null);
                    entry.setEgressRateMb(rate != null && rate > 0 ? rate : null);
                    results.add(entry);
                }
            }
        }

        // ── Virtual Machines (default NIC) ───────────────────────────────────
        if (doVm) {
            List<VMInstanceVO> vms = new ArrayList<>();
            if (filterVmId != null) {
                VMInstanceVO vm = vmInstanceDao.findById(filterVmId);
                if (vm != null) vms.add(vm);
            } else {
                vms.addAll(vmInstanceDao.listAll());
            }
            for (VMInstanceVO vm : vms) {
                if (vm.getType() != com.cloud.vm.VirtualMachine.Type.User) continue;
                List<NicVO> nics = nicDao.listByVmId(vm.getId());
                for (NicVO nic : nics) {
                    if (!nic.isDefaultNic()) continue;
                    NetworkVO nw = networksDao.findById(nic.getNetworkId());
                    if (nw == null) continue;

                    NetworkRateLimitResponse entry = new NetworkRateLimitResponse();
                    entry.setResourceType("VM");
                    entry.setResourceId(vm.getUuid());
                    entry.setResourceName(vm.getInstanceName());
                    entry.setInterfaceIp(nic.getIPv4Address());
                    entry.setTrafficType(TrafficType.Guest.name());
                    entry.setLiveValue(false);

                    Integer rate = networkModel.getNetworkRate(nw.getId(), vm.getId());
                    entry.setIngressRateMb(rate != null && rate > 0 ? rate : null);
                    entry.setEgressRateMb(rate != null && rate > 0 ? rate : null);
                    results.add(entry);
                    break; // Only one default NIC per VM
                }
            }
        }

        return results;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Resolves the effective guest-tier rate for a network.
     * Priority: dynamic detail override → network offering rate.
     */
    private Integer resolveGuestNetworkRate(NetworkVO nw) {
        NetworkDetailVO detail = networkDetailsDao.findDetail(nw.getId(), "network.rate.mbps");
        if (detail != null) {
            try {
                return Integer.parseInt(detail.getValue());
            } catch (NumberFormatException ignored) { /* fall through */ }
        }
        Integer rate = networkModel.getNetworkRate(nw.getId(), null);
        return (rate != null && rate > 0) ? rate : null;
    }

    /**
     * Resolves the effective public-interface rate for a VPC.
     * Priority: VPC instance override → VPC offering rate.
     */
    private Integer resolveVpcPublicRate(VpcVO vpc) {
        Integer rate = vpc.getPublicNetworkRate();
        if (rate == null) {
            VpcOffering offering = vpcOfferingDao.findById(vpc.getVpcOfferingId());
            if (offering != null) {
                rate = offering.getPublicNetworkRate();
            }
        }
        return (rate != null && rate > 0) ? rate : null;
    }
}
