#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# update_bandwidth.sh — dynamically apply (or remove) tc bandwidth shaping on a VR interface
#
# Usage:
#   update_bandwidth.sh <interface_ip> <ingress_kbps> <egress_kbps>
#
#   <interface_ip>  : IP address assigned to the interface to shape (used to resolve the interface name)
#   <ingress_kbps>  : Inbound  (ingress) limit in kbps; 0 = remove limit
#   <egress_kbps>   : Outbound (egress)  limit in kbps; 0 = remove limit
#
# Examples:
#   update_bandwidth.sh 203.0.113.1 102400 102400   # Cap public NIC at 100 Mbps
#   update_bandwidth.sh 203.0.113.1 0 0             # Remove all shaping

set -e

INTERFACE_IP=$1
INGRESS_KBPS=$2
EGRESS_KBPS=$3

if [[ -z "$INTERFACE_IP" || -z "$INGRESS_KBPS" || -z "$EGRESS_KBPS" ]]; then
    echo "Usage: $0 <interface_ip> <ingress_kbps> <egress_kbps>"
    exit 1
fi

# Resolve the interface name from the IP address
IFACE=$(ip -o addr show | awk -v ip="$INTERFACE_IP" '$4 ~ ("^" ip "/") {print $2; exit}')

if [[ -z "$IFACE" ]]; then
    echo "ERROR: Could not find interface with IP $INTERFACE_IP"
    exit 2
fi

echo "Applying bandwidth shaping on $IFACE (ingress=${INGRESS_KBPS}kbps egress=${EGRESS_KBPS}kbps)"

# ── Remove existing qdiscs ───────────────────────────────────────────────────
tc qdisc del dev "$IFACE" root     2>/dev/null || true
tc qdisc del dev "$IFACE" ingress  2>/dev/null || true

# ── Egress shaping (outgoing traffic from VR's perspective) ─────────────────
if [[ "$EGRESS_KBPS" -gt 0 ]]; then
    # TBF (Token Bucket Filter): simple, low-jitter rate limiter
    BURST_BYTES=$(( EGRESS_KBPS * 128 ))   # ~1/8 of 1 second at rate
    [[ $BURST_BYTES -lt 32768 ]] && BURST_BYTES=32768
    tc qdisc add dev "$IFACE" root tbf \
        rate "${EGRESS_KBPS}kbit"  \
        burst "${BURST_BYTES}"     \
        latency 400ms
    echo "  Egress  : ${EGRESS_KBPS} kbps applied"
else
    echo "  Egress  : no limit (removed)"
fi

# ── Ingress policing (incoming traffic from VR's perspective) ────────────────
if [[ "$INGRESS_KBPS" -gt 0 ]]; then
    BURST_BYTES=$(( INGRESS_KBPS * 128 ))
    [[ $BURST_BYTES -lt 32768 ]] && BURST_BYTES=32768
    tc qdisc  add    dev "$IFACE" handle ffff: ingress
    tc filter add    dev "$IFACE" parent ffff: protocol ip u32 \
        match u32 0 0 \
        police rate "${INGRESS_KBPS}kbit" burst "${BURST_BYTES}" drop flowid :1
    echo "  Ingress : ${INGRESS_KBPS} kbps policed"
else
    echo "  Ingress : no limit (removed)"
fi

exit 0
