// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.teamfoundation.plugin.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.microsoft.teamfoundation.build.webapi.model.Build;
import com.microsoft.teamfoundation.build.webapi.model.BuildResult;
import com.microsoft.teamfoundation.build.webapi.model.BuildStatus;
import com.microsoft.teamfoundation.distributedtask.webapi.TaskHttpClient;
import com.microsoft.teamfoundation.distributedtask.webapi.model.*;

import com.microsoft.teamfoundation.plugin.ActualBuild;
import com.microsoft.teamfoundation.plugin.TfsBuildFacade;

/**
 * This class is a facade to update TFS build from Jenkins.
 *
 * All updates to TFS build should go through this class.  Also deliberately this
 * class only contains IDs and does not keep state.  All build update operation are PATCH
 * based, and this object maybe out of sync with what is really happening on TFS,
 * so we GET the object from the IDs and then PATCH the server
 */
public class TfsBuildFacadeImpl implements TfsBuildFacade {

    private static final Logger logger = Logger.getLogger(TfsBuildFacadeImpl.class.getName());

    /*
     * Constants
     */
    private static final String JOB_RECORD_TYPE = "Job";
    private static final String TASK_RECORD_TYPE = "Task";

    /*
     * The Jenkins build which is running
     */
    private ActualBuild actualBuild;

    /*
     * The ID of the build container running on TFS
     */
    private int tfsBuildId;

    /*
     * The ID of the orchestration plan for this jenkins build on TFS
     */
    private UUID planId;

    /*
     * The ID of the Team Project for this jenkins build on TFS
     */
    private UUID projectId;

    /*
     * The ID of the timeline for this jenkins build on TFS
     */
    private UUID timelineId;

    /*
     * The ID of the job record
     */
    private UUID jobRecordId;

    /*
     * The TFS REST client
     */
    private TfsClient client;

    /* should only be instantiated from TfsBuildFacadeFactoryImpl from same package */
    /* default */
    TfsBuildFacadeImpl(final Build tfsBuild, final ActualBuild actualBuild, final TfsClient tfsClient) {
        this.tfsBuildId = tfsBuild.getId();
        this.actualBuild = actualBuild;
        this.client = tfsClient;
        this.projectId = tfsBuild.getProject().getId();

        this.planId = tfsBuild.getOrchestrationPlan().getPlanId();
        TaskOrchestrationPlan plan = getTaskClient().getPlan(projectId, "build", planId);
        this.timelineId = plan.getTimeline().getId();

        //populate timeline record
        TimelineRecord jobRecord = getTimelineJobRecord(getTimelineId());
        if (jobRecord == null) {
            jobRecord = createTimelineJobRecord();
            jobRecord.setName(getActualBuild().getDisplayName());

            updateSingleRecord(jobRecord);
        }

        if (jobRecord.getLog() == null) {
            createLogForTimelineRecord(jobRecord);
        }

        // populate rest of the fields
        this.jobRecordId = jobRecord.getId();
    }

    /**
     * Get the build container ID on TFS
     *
     * @return teamfoundation build id
     */
    @Override
    public int getTfsBuildId() {
        return tfsBuildId;
    }

    @Override
    public UUID createTaskRecord(String taskName) {
        TimelineRecord jobRecord = getTimelineJobRecord(getTimelineId());
        if (jobRecord != null) {
            TimelineRecord record = createTimelineTaskRecord(jobRecord);
            record.setName(taskName);
            createLogForTimelineRecord(record);

            updateSingleRecord(record);
            return record.getId();
        }

        return null;
    }

    /**
     * Update TFS Build status to started with starting time
     */
    @Override
    public void startBuild() {
        Build build = queryTfsBuild();

        if (build != null) {
            build.setStartTime(getActualBuild().getStartTime());
            build.setStatus(BuildStatus.IN_PROGRESS);

            getClient().getBuildClient().updateBuild(build, build.getProject().getId(), build.getId());
        }
    }

    /**
     * Update TFS Build status to finished with status
     */
    @Override
    public void finishBuild() {
        // first finish the job record
        finishTaskRecord(getJobRecordId(), getActualBuild().getFinishTime(),
                convertToTfsTaskResult(getActualBuild().getBuildResult()));

        Build b = queryTfsBuild();
        b.setFinishTime(getActualBuild().getFinishTime());

        BuildResult tfsResult = getActualBuild().getBuildResult();

        b.setResult(tfsResult);
        b.setStatus(BuildStatus.COMPLETED);

        String commitSha1 = getActualBuild().getBuildSourceCommit();
        logger.info("Setting TFS build sourceVersion to: " + commitSha1);
        b.setSourceVersion(commitSha1);

        getClient().getBuildClient().updateBuild(b, b.getProject().getId(), b.getId());
    }

    @Override
    public void startTaskRecord(UUID taskId, Date startTime, String workerName) {
        TimelineRecord record = getTimelineRecordById(taskId);
        if (record != null) {
            record.setState(TimelineRecordState.IN_PROGRESS);
            record.setStartTime(startTime != null ? startTime : new Date());
            record.setWorkerName(workerName);

            updateSingleRecord(record);
        }
    }

    @Override
    public void finishTaskRecord(UUID taskId, Date finishTime, TaskResult result) {
        TimelineRecord record = getTimelineRecordById(taskId);
        if (record != null) {
            record.setState(TimelineRecordState.COMPLETED);
            record.setFinishTime(finishTime != null ? finishTime : new Date());
            record.setResult(result);

            updateSingleRecord(record);
        }
    }

    @Override
    public void uploadJobLog(List<String> logLines) throws IOException {
        uploadTaskLog(getJobRecordId(), logLines);
    }

    @Override
    public void uploadTaskLog(UUID taskId, List<String> logLines) throws IOException {
        if (logLines == null || logLines.size() == 0 || taskId == null) {
            return;
        }

        uploadTaskLog(taskId, getByteArrayInputStream(logLines));
    }

    @Override
    public void uploadTaskLog(UUID taskId, InputStream io) {
        if (io == null || taskId == null) {
            return;
        }

        TimelineRecord record = getTimelineRecordById(taskId);
        if (record != null) {
            getTaskClient().appendLog(io, getProjectId(), "build", getPlanId(), record.getLog().getId());
        }
    }

    /**
     * Posting lines to TFS build console
     *
     * @param lines
     */
    @Override
    public void postConsoleLog(List<String> lines) {
        if (lines == null || lines.size() == 0) {
            return;
        }

        // post console feed
        getTaskClient().postLines(getProjectId(), "build", lines, getPlanId(), getTimelineId(), getJobRecordId());
    }

    private InputStream getByteArrayInputStream(List<String> lines) throws IOException {
        // assuming each line is 256-bytes long to avoid grow constantly
        ByteArrayOutputStream os = new ByteArrayOutputStream(lines.size() * 256);
        byte[] newLine = String.format("%n").getBytes(Charset.defaultCharset());
        for (String line : lines) {
            os.write(line.getBytes(Charset.defaultCharset()));
            os.write(newLine);
        }

        return new ByteArrayInputStream(os.toByteArray());
    }

    private TimelineRecord getTimelineJobRecord(UUID timelineId) {
        List<TimelineRecord> records = queryTfsTimelineRecords(timelineId);
        for (TimelineRecord record : records) {
            if (record.getType().equalsIgnoreCase(JOB_RECORD_TYPE)) {
                return record;
            }
        }

        return null;
    }

    private TimelineRecord getTimelineRecordById(UUID taskId) {
        List<TimelineRecord> records = queryTfsTimelineRecords(getTimelineId());
        for (TimelineRecord record : records) {
            if (record.getId().equals(taskId)) {
                return record;
            }
        }

        return null;
    }

    private List<TimelineRecord> queryTfsTimelineRecords(UUID timelineId) {
        List<TimelineRecord> records = getTaskClient().getRecords(getProjectId(), "build", getPlanId(), timelineId);
        if (records == null) {
            records = new ArrayList<TimelineRecord>();
        }

        return records;
    }

    private Build queryTfsBuild() {
        return getClient().getBuildClient().getBuild(getTfsBuildId(), null);
    }

    /**
     * Create a log reference for the job record
     *
     * @return jobId
     */
    private void createLogForTimelineRecord(TimelineRecord record) {
        TaskLog log = createTfsLog("logs\\" + record.getId().toString());

        logger.info("Setting up record " + record.getType() + " log path: " + log.getPath() + ", log id: " + log.getId());
        record.setLog(log);
    }

    private TaskLog createTfsLog(String path) {
        TaskLog log = new TaskLog();
        log.setPath(path);

        // Note that we should use the TaskLog object returned from the server,
        // but not that we passed as the parameter.
        return getTaskClient().createLog(getProjectId(), "build", log, getPlanId());
    }

    private void updateSingleRecord(TimelineRecord record) {
        updateRecords(Collections.singletonList(record), getTimelineId());
    }

    private void updateRecords(List<TimelineRecord> timelineRecords, UUID timelineId) {
        getTaskClient().updateRecords(getProjectId(), "build", timelineRecords, getPlanId(), timelineId);
    }

    private TimelineRecord createTimelineJobRecord() {
        TimelineRecord jobRecord = new TimelineRecord();
        jobRecord.setId(UUID.randomUUID());
        jobRecord.setType(JOB_RECORD_TYPE);
        jobRecord.setState(TimelineRecordState.PENDING);

        return jobRecord;
    }

    private TimelineRecord createTimelineTaskRecord(TimelineRecord jobRecord) {
        TimelineRecord taskRecord = new TimelineRecord();
        taskRecord.setId(UUID.randomUUID());
        taskRecord.setType(TASK_RECORD_TYPE);
        taskRecord.setParentId(jobRecord.getId());
        taskRecord.setState(TimelineRecordState.PENDING);

        return taskRecord;
    }

    private TaskResult convertToTfsTaskResult(BuildResult buildResult) {
        if (buildResult == BuildResult.SUCCEEDED) {
            return TaskResult.SUCCEEDED;
        }

        if (buildResult == BuildResult.CANCELED) {
            return TaskResult.CANCELED;
        }

        // Assume FAILURE (and other cases that aren't successful)
        return TaskResult.FAILED;
    }

    private UUID getPlanId() {
        return planId;
    }

    private UUID getProjectId() {
        return projectId;
    }

    private UUID getTimelineId() {
        return timelineId;
    }

    private UUID getJobRecordId() {
        return jobRecordId;
    }

    private ActualBuild getActualBuild() {
        return actualBuild;
    }

    private TfsClient getClient() {
        return client;
    }

    private TaskHttpClient getTaskClient() {
        return getClient().getTaskHttpClient();
    }
}
