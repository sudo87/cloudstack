package com.cloud.hypervisor.xenserver.resource.wrapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;

import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.vm.DiskProfile;

@RunWith(MockitoJUnitRunner.class)
public class CitrixRequestWrapperTest {

    @Mock
    protected CitrixResourceBase citrixResourceBase;
    @Mock
    protected RebootAnswer rebootAnswer;
    @Mock
    protected CreateAnswer createAnswer;

    @Test
    public void testWrapperInstance() {
        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);
    }

    @Test
    public void testUnknownCommand() {
        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        try {
            wrapper.execute(new NotAValidCommand(), citrixResourceBase);
        } catch (final Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    public void testExecuteRebootRouterCommand() {
        final RebootRouterCommand rebootRouterCommand = new RebootRouterCommand("Test", "127.0.0.1");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(rebootRouterCommand, citrixResourceBase);

        verify(citrixResourceBase, times(2)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testExecuteCreateCommand() {
        final CreateCommand createCommand = new CreateCommand(new DiskProfile(null), "", new StoragePoolVO(), false);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(createCommand, citrixResourceBase);

        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testCheckConsoleProxyLoadCommand() {
        final CheckConsoleProxyLoadCommand consoleProxyCommand = new CheckConsoleProxyLoadCommand();

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(consoleProxyCommand, citrixResourceBase);

        assertFalse(answer.getResult());
    }

    @Test
    public void testWatchConsoleProxyLoadCommand() {
        final WatchConsoleProxyLoadCommand watchConsoleProxyCommand = new WatchConsoleProxyLoadCommand(0, 0, "", "", 0);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(watchConsoleProxyCommand, citrixResourceBase);

        assertFalse(answer.getResult());
    }

    @Test
    public void testReadyCommandCommand() {
        final ReadyCommand readyCommand = new ReadyCommand();

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(readyCommand, citrixResourceBase);

        assertFalse(answer.getResult());
    }

    @Test
    public void testGetHostStatsCommandCommand() {
        final GetHostStatsCommand statsCommand = new GetHostStatsCommand(null, null, 0);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(statsCommand, citrixResourceBase);

        assertTrue(answer.getResult());
    }

    @Test
    public void testGetVmStatsCommandCommand() {
        final GetVmStatsCommand statsCommand = new GetVmStatsCommand(new ArrayList<String>(), null, null);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(statsCommand, citrixResourceBase);

        assertTrue(answer.getResult());
    }

    @Test
    public void testGetVmDiskStatsCommandCommand() {
        final GetVmDiskStatsCommand statsCommand = new GetVmDiskStatsCommand(new ArrayList<String>(), null, null);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(statsCommand, citrixResourceBase);

        assertTrue(answer.getResult());
    }

    @Test
    public void testCheckHealthCommandCommand() {
        final CheckHealthCommand statsCommand = new CheckHealthCommand();

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(statsCommand, citrixResourceBase);

        assertFalse(answer.getResult());
    }

    @Test
    public void testStopCommandCommand() {
        final StopCommand statsCommand = new StopCommand("Test", false, false);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(statsCommand, citrixResourceBase);

        assertFalse(answer.getResult());
    }
}

class NotAValidCommand extends Command {

    @Override
    public boolean executeInSequence() {
        return false;
    }

}