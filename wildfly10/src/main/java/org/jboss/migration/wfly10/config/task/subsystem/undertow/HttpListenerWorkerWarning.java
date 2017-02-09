package org.jboss.migration.wfly10.config.task.subsystem.undertow;

import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.migration.core.ServerMigrationTask;
import org.jboss.migration.core.ServerMigrationTaskContext;
import org.jboss.migration.core.ServerMigrationTaskName;
import org.jboss.migration.core.ServerMigrationTaskResult;
import org.jboss.migration.core.env.TaskEnvironment;
import org.jboss.migration.wfly10.config.management.SubsystemsManagement;
import org.jboss.migration.wfly10.config.task.subsystem.UpdateSubsystemTaskFactory;

public class HttpListenerWorkerWarning implements UpdateSubsystemTaskFactory.SubtaskFactory {
    public static final HttpListenerWorkerWarning INSTANCE = new HttpListenerWorkerWarning();
    public static final String TASK_NAME_NAME = "warning-http-listener-worker";
    public static final ServerMigrationTaskName TASK_NAME = new ServerMigrationTaskName.Builder(TASK_NAME_NAME).build();
    public static final String REMOTING_SUBSYSTEM_NAME = "remoting";
    public static final String MODEL_ATTRIBUTE_WORKER = "worker";
    public static final String MODEL_ATTRIBUTE_SERVER = "server";
    public static final String MODEL_ATTRIBUTE_HTTP_LISTENER = "http-listener";
    public static final String MODEL_ATTRIBUTE_HTTPS_LISTENER = "https-listener";
    public static final String MODEL_ATTRIBUTE_VALUE_DEFAULT = "default";
    public static final String MODEL_ATTRIBUTE_VAlUE_UNDEFINED = "undefined";

    @Override
    public ServerMigrationTask getServerMigrationTask(ModelNode config, UpdateSubsystemTaskFactory subsystem,
            SubsystemsManagement subsystemsManagement) {
        return new UpdateSubsystemTaskFactory.Subtask(config, subsystem, subsystemsManagement) {
            @Override
            public ServerMigrationTaskName getName() {
                return TASK_NAME;
            }

            @Override
            protected ServerMigrationTaskResult run(ModelNode config, final UpdateSubsystemTaskFactory subsystem,
                    final SubsystemsManagement subsystemsManagement, final ServerMigrationTaskContext context,
                    final TaskEnvironment taskEnvironment) throws Exception {
                // refresh subsystem config to see any changes possibly made during migration
                config = subsystemsManagement.getResource(subsystem.getName());
                if (config == null) {
                    return ServerMigrationTaskResult.SKIPPED;
                }

                final ModelNode remotingConfig = subsystemsManagement.getResource(REMOTING_SUBSYSTEM_NAME);
                final String remotingWorker = sanitazeWorkerName(remotingConfig.get(MODEL_ATTRIBUTE_WORKER).asString());
                // subsystem/server[1-*]/http(s)-listener[0-*]
                List<Property> servers = config.get(MODEL_ATTRIBUTE_SERVER).asPropertyList();
                for (Property server : servers) {
                    processServer(context, remotingWorker, server.getName(), server.getValue());
                }
                return ServerMigrationTaskResult.SUCCESS;
            }

            private void processServer(final ServerMigrationTaskContext context, final String remotingWorker,
                    final String serverName, final ModelNode value) {
                List<Property> lisnteners = value.get(MODEL_ATTRIBUTE_HTTP_LISTENER).asPropertyList();
                for (Property httpListener : lisnteners) {
                    processListener(context, remotingWorker, serverName, false, httpListener.getName(),
                            httpListener.getValue());
                }

                lisnteners = value.get(MODEL_ATTRIBUTE_HTTPS_LISTENER).asPropertyList();
                for (Property httpListener : lisnteners) {
                    processListener(context, remotingWorker, serverName, true, httpListener.getName(), httpListener.getValue());
                }
            }

            private void processListener(final ServerMigrationTaskContext context, final String remotingWorker,
                    final String serverName, final boolean isHttps, final String listenerName, ModelNode value) {
                final String listenerWorkerName = sanitazeWorkerName(value.get(MODEL_ATTRIBUTE_WORKER).asString());
                //TODO: set to remoting value and warn?
                if (!listenerWorkerName.equals(remotingWorker)) {
                    final String httpType = isHttps ? "HTTPS" : "HTTP";
                    context.getLogger().warnv(
                            "{0} listener worker value must match value present in remoting subsystem definition. Remoting: ''{1}''. Uddertow server: ''{2}'', listener: ''{3}'', worker: ''{4}''",
                            httpType, remotingWorker, serverName, listenerName,
                                    listenerWorkerName );
                }

            }

            private String sanitazeWorkerName(final String name){
                //this might be wrong
                if(name.equals(MODEL_ATTRIBUTE_VAlUE_UNDEFINED) || name.equals(MODEL_ATTRIBUTE_VALUE_DEFAULT)){
                    return MODEL_ATTRIBUTE_VALUE_DEFAULT;
                } else {
                    return name;
                }
            }
        };
    }
}