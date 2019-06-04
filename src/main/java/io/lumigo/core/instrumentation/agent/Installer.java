package io.lumigo.core.instrumentation.agent;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import io.lumigo.core.configuration.Configuration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
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
            String agentPath = findAgentPath();
            Logger.info("Loading agent jar: {}", agentPath);
            for (VirtualMachineDescriptor vmd : vms) {
                try {
                    VirtualMachine vm = VirtualMachine.attach(vmd.id());
                    try {
                        if (agentPath.contains("lib")) {
                            vm.loadAgent(agentPath, "lib");
                        } else {
                            vm.loadAgent(agentPath);
                        }
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

    public static String findAgentPath() {
        if (Files.notExists(Paths.get("/var/task/lumigo-agent.jar"))) {
            Logger.info(
                    "Agent jar was not found under /var/task/lumigo-agent.jar, try to find it under /var/task/lib");
            try (Stream<Path> paths = Files.walk(Paths.get("/var/task/lib"))) {
                Path lumigoAgentPath =
                        paths.filter(p -> p.toFile().getAbsolutePath().contains("lumigo-agent"))
                                .findFirst()
                                .get();
                return lumigoAgentPath.toFile().getAbsolutePath();
            } catch (Exception e) {
                Logger.error(e);
                return "/var/task/lumigo-agent.jar";
            }
        } else {
            return "/var/task/lumigo-agent.jar";
        }
    }
}
