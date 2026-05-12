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

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UpdateNicRateAnswer;
import com.cloud.agent.api.UpdateNicRateCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

/**
 * KVM command wrapper that dynamically updates the bandwidth limit on a
 * specific VM NIC without requiring a VM restart.
 *
 * <p>Execution flow:
 * <ol>
 *   <li>Locate the target NIC inside the running domain by MAC address.</li>
 *   <li>Mutate its {@code _networkRateKBps} field to the requested value
 *       ({@code rateMbps * 125 KBps}); a value {@code <= 0} removes the cap.</li>
 *   <li>Serialize the updated {@link InterfaceDef} to XML and call
 *       {@link Domain#updateDeviceFlags} with both {@code LIVE} and
 *       {@code CONFIG} flags so the change survives the next start.</li>
 * </ol>
 */
@ResourceWrapper(handles = UpdateNicRateCommand.class)
public final class LibvirtUpdateNicRateCommandWrapper extends CommandWrapper<UpdateNicRateCommand, Answer, LibvirtComputingResource> {

    /** Mbps → KBps conversion factor (same as VifDriverBase). */
    private static final int MBPS_TO_KBPS = 125;

    @Override
    public Answer execute(UpdateNicRateCommand command, LibvirtComputingResource libvirtComputingResource) {
        final String nicMac = command.getNicMacAddress();
        final String vmName = command.getVmName();
        final int rateMbps = command.getRateMbps();

        Domain vm = null;
        try {
            final LibvirtUtilitiesHelper helper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            final Connect conn = helper.getConnectionByVmName(vmName);
            vm = libvirtComputingResource.getDomain(conn, vmName);

            final InterfaceDef nic = libvirtComputingResource.getInterface(conn, vmName, nicMac);
            if (nic == null) {
                String msg = String.format("NIC with MAC %s not found in VM %s.", nicMac, vmName);
                logger.warn(msg);
                return new UpdateNicRateAnswer(command, false, msg);
            }

            // Convert Mbps → KBps; <=0 means "remove cap"
            int rateKBps = rateMbps > 0 ? rateMbps * MBPS_TO_KBPS : 0;
            nic.setNetworkRateKBps(rateKBps);

            // Apply both live and to the persistent config so it survives restart
            int flags = Domain.DeviceModifyFlags.LIVE | Domain.DeviceModifyFlags.CONFIG;
            vm.updateDeviceFlags(nic.toString(), flags);

            logger.info("Updated NIC {} on VM {} to {} Mbps ({} KBps).", nicMac, vmName, rateMbps, rateKBps);
            return new UpdateNicRateAnswer(command, true, "success");
        } catch (final LibvirtException e) {
            final String msg = String.format("Updating NIC rate failed for NIC %s on VM %s: %s.", nicMac, vmName, e.getMessage());
            logger.warn(msg, e);
            return new UpdateNicRateAnswer(command, false, msg);
        } finally {
            if (vm != null) {
                try {
                    vm.free();
                } catch (final LibvirtException l) {
                    logger.trace("Ignoring libvirt error while freeing domain.", l);
                }
            }
        }
    }
}
