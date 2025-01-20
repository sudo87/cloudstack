package com.cloud.test;

import java.util.ArrayList;
import java.util.List;

public class TimeOfDayManagerImpl implements TimeOfDayManager {

    public TimeOfDayManagerImpl() {
        super();
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(GetTimeOfDayCmd.class);
        return cmdList;
    }
}
