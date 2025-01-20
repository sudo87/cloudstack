package com.cloud.test;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GetTimeOfDayCmdResponse extends BaseResponse {
    @SerializedName(ApiConstants.IS_ASYNC)
    @Param(description = "true if api is async")
    private Boolean isAsync;

    @SerializedName("timeOfDay")
    @Param(description = "The time of day from CloudStack")
    private String timeOfDay;

    @SerializedName("exampleEcho")
    @Param(description = "An upper cased String")
    private String exampleEcho;


    public GetTimeOfDayCmdResponse() {
        this.isAsync = false;

        SimpleDateFormat dateFormatYYYYMMDD = new SimpleDateFormat("yyyMMdd hh:mm:ss");
        this.setTimeOfDay(new StringBuilder(dateFormatYYYYMMDD.format(new Date())).toString());
    }

    public Boolean getAsync() {
        return isAsync;
    }

    public void setAsync(Boolean async) {
        isAsync = async;
    }

    public void setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public void setExampleEcho(String exampleEcho) {
        this.exampleEcho = exampleEcho;
    }
}
