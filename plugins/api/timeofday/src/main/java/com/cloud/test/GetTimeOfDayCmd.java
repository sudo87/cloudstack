package com.cloud.test;

import com.cloud.exception.*;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.*;

@APICommand(
        name="getTimeOfDay",
        description = "Get Cloudstack time",
        responseObject = GetTimeOfDayCmdResponse.class,
        includeInApiDoc = true,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin}
)
public class GetTimeOfDayCmd extends BaseCmd {
    private static final String s_name = "gettimeofdayresponse";

    @Parameter(name="example", type = CommandType.STRING, required = false, description = "just example", validations = {ApiArgValidator.NotNullOrEmpty})
    private String example;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        GetTimeOfDayCmdResponse response = new GetTimeOfDayCmdResponse();
        response.setExampleEcho(example);

        response.setObjectName("timeofday"); // the inner part of the json structure
        response.setResponseName(getCommandName()); // the outer part of the json structure

        this.setResponseObject(response);

    }

    @Override
    public String getCommandName() {
       return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
