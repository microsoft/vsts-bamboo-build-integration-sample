// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.teamfoundation.plugin;

import com.microsoft.teamfoundation.build.webapi.model.BuildResult;

import java.util.Date;

/**
 *  This interface should encapsulate all information about the real build happening on
 *  third party software that TFS build requires
 */
public interface ActualBuild {

    /**
     * Get the display name of this build, this name will appear as the top level timeline record on TFS
     *
     * @return display name
     */
    String getDisplayName();

    /**
     * Get actual build status, could be success / failed / cancelled
     *
     * @return build status
     */
    BuildResult getBuildResult();

    /**
     * Get the SCM branch information used in this build
     *
     * @return branch display name
     */
    String getBuildSourceBranch();

    /**
     * Get the SCM revision information used in this build
     *
     * @return commit/revision string
     */
    String getBuildSourceCommit();

    /**
     * Get when this build is started
     *
     * @return Date start time
     */
    Date getStartTime();

    /**
     * Get when this build is finished
     *
     * This time is probably not super accurate in some cases, as our plugin needs to run before the job is really
     * completed, so this is usually the time when our post build extension point is run
     *
     * @return Date finish time
     */
    Date getFinishTime();

    /**
     * Get the agent where this build is been built
     *
     * @return String Agent display name
     */
    String getWorkerName();
}
