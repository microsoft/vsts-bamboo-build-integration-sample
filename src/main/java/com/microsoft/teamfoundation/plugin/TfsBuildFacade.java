// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.teamfoundation.plugin;

import com.microsoft.teamfoundation.distributedtask.webapi.model.TaskResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * This class is a facade to update TFS build from a third party real build.
 */
public interface TfsBuildFacade {

    /**
     * Get the Build container's id on TFS server
     *
     * The Build id is an unique id that identifies a single build on TFS
     *
     * @return build id on TFS
     */
    int getTfsBuildId();

    /**
     * Set the TFS build container's (as well as the job record associated with this build) state to "In Progress"
     */
    void startBuild();

    /**
     * Set the TFS build container's (as well as the job record associated with this build) state to "Completed"
     *
     * Set the build status according to the real build's status, could be successful, failed or cancelled
     */
    void finishBuild();

    /**
     * Create a task with the specified name on TFS side
     *
     * @param taskName
     * @return task id
     */
    UUID createTaskRecord(String taskName);

    /**
     * Mark the specified task associated with this build to "In Progress" on TFS side
     *
     * @param taskId  unique id to identify the task
     * @param startTime time the task is starting
     * @param workerName the agent who is responsible for this build task
     */
    void startTaskRecord(UUID taskId, Date startTime, String workerName);

    /**
     * Mark the specified task associated with this build to "Completed" on TFS side
     *
     * @param taskId  unique id to identify the task
     * @param finishTime time the task is finished
     */
    void finishTaskRecord(UUID taskId, Date finishTime, TaskResult result);

    /**
     * Upload logs associated with the overarching job
     *
     * @param logLines logs lines
     * @throws IOException
     */
    void uploadJobLog(List<String> logLines) throws IOException;

    /**
     * Upload logs associated with a specific task
     *
     * @param taskId  unique id to identify the task
     * @param logLines logs lines
     * @throws IOException
     */
    void uploadTaskLog(UUID taskId, List<String> logLines) throws IOException;

    /**
     * Upload logs associated with a specific task
     *
     * @param taskId  unique id to identify the task
     * @param io InputStream read the log from
     */
    void uploadTaskLog(UUID taskId, InputStream io);

    /**
     * Post specified lines to the TFS build console
     *
     * @param lines lines that appear on TFS build console
     */
    void postConsoleLog(List<String> lines);
}
