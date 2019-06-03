package io.lumigo.core.instrumentation.agent;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import io.lumigo.core.configuration.Configuration;
import java.util.List;
import org.pmw.tinylog.Logger;

public class Installer {
    private static boolean firstStart = true;

    public static synchronized void install() {
        if (Configuration.getInstance().isAwsEnvironment()
                && Configuration.getInstance().isInstrumentationEnabled()
                && firstStart) {
            firstStart = false;
            Logger.info("Agent installation start");
            List<VirtualMachineDescriptor> vms = VirtualMachine.list();

            for (VirtualMachineDescriptor vmd : vms) {
                try {
                    VirtualMachine vm = VirtualMachine.attach(vmd.id());
                    try {
                        vm.loadAgent("/var/task/lumigo-agent.jar");
                    } finally {
                        vm.detach();
                    }
                } catch (Throwable e) {
                    Logger.error(e, "Fail to attach agent, no instrumentation");
                }
            }
        } else {
            Logger.info(
                    "Agent installation is skipped, isAwsEnvironment: {}, isInstrumentationEnabled: {}, isFirstStart: {}",
                    Configuration.getInstance().isAwsEnvironment(),
                    Configuration.getInstance().isInstrumentationEnabled(),
                    firstStart);
        }
    }
}
