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
package org.apache.cloudstack.api.command.user.network;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.User;

@APICommand(name = "updateNetwork", description = "Updates a network", responseObject = NetworkResponse.class, responseView = ResponseView.Restricted, entityType = {Network.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateNetworkCmd extends BaseAsyncCustomIdCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateNetworkCmd.class.getName());

    private static final String s_name = "updatenetworkresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = NetworkResponse.class,
            required=true, description="the ID of the network")
    protected Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the new name for the network")
    private String name;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "the new display text for the network")
    private String displayText;

    @Parameter(name = ApiConstants.NETWORK_DOMAIN, type = CommandType.STRING, description = "network domain")
    private String networkDomain;

    @Parameter(name = ApiConstants.CHANGE_CIDR, type = CommandType.BOOLEAN, description = "Force update even if cidr type is different")
    private Boolean changeCidr;

    @Parameter(name = ApiConstants.NETWORK_OFFERING_ID, type = CommandType.UUID, entityType = NetworkOfferingResponse.class, description = "network offering ID")
    private Long networkOfferingId;

    @Parameter(name = ApiConstants.GUEST_VM_CIDR, type = CommandType.STRING, description = "CIDR for Guest VMs,Cloudstack allocates IPs to Guest VMs only from this CIDR")
    private String guestVmCidr;

    @Parameter(name = ApiConstants.DISPLAY_NETWORK,
               type = CommandType.BOOLEAN,
 description = "an optional field, whether to the display the network to the end user or not.", authorized = {RoleType.Admin})
    private Boolean displayNetwork;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getNetworkName() {
        return name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getNetworkDomain() {
        return networkDomain;
    }

    public Long getNetworkOfferingId() {
        return networkOfferingId;
    }

    public Boolean getChangeCidr() {
        if (changeCidr != null) {
            return changeCidr;
        }
        return false;
    }

    public String getGuestVmCidr() {
        return guestVmCidr;
    }

    public Boolean getDisplayNetwork() {
        return displayNetwork;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Network network = _networkService.getNetwork(id);
        if (network == null) {
            throw new InvalidParameterValueException("Networkd id=" + id + " doesn't exist");
        } else {
            return _networkService.getNetwork(id).getAccountId();
        }
    }

    @Override
    public void execute() throws InsufficientCapacityException, ConcurrentOperationException {
        User callerUser = _accountService.getActiveUser(CallContext.current().getCallingUserId());
        Account callerAccount = _accountService.getActiveAccountById(callerUser.getAccountId());
        Network network = _networkService.getNetwork(id);
        if (network == null) {
            throw new InvalidParameterValueException("Couldn't find network by id");
        }

        Network result =
            _networkService.updateGuestNetwork(getId(), getNetworkName(), getDisplayText(), callerAccount, callerUser, getNetworkDomain(), getNetworkOfferingId(),
                getChangeCidr(), getGuestVmCidr(), getDisplayNetwork(), getCustomId());

        if (result != null) {
            NetworkResponse response = _responseGenerator.createNetworkResponse(ResponseView.Restricted, result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update network");
        }
    }

    @Override
    public String getEventDescription() {
        StringBuilder eventMsg = new StringBuilder("Updating network: " + getId());
        if (getNetworkOfferingId() != null) {
            Network network = _networkService.getNetwork(getId());
            if (network == null) {
                throw new InvalidParameterValueException("Networkd id=" + id + " doesn't exist");
            }
            if (network.getNetworkOfferingId() != getNetworkOfferingId()) {
                NetworkOffering oldOff = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());
                NetworkOffering newOff = _entityMgr.findById(NetworkOffering.class, getNetworkOfferingId());
                if (newOff == null) {
                    throw new InvalidParameterValueException("Networkd offering id supplied is invalid");
                }

                eventMsg.append(". Original network offering id: " + oldOff.getUuid() + ", new network offering id: " + newOff.getUuid());
            }
        }

        return eventMsg.toString();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_UPDATE;
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return id;
    }

    @Override
    public void checkUuid() {
        if (getCustomId() != null) {
            _uuidMgr.checkUuid(getCustomId(), Network.class);
        }
    }
}