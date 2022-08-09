/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2022 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.api.v4.git;
import org.apache.commons.io.FileUtils;
import org.eclipse.dirigible.api.v3.security.UserFacade;
import org.eclipse.dirigible.commons.api.scripting.IScriptingFacade;
import org.eclipse.dirigible.core.git.GitConnectorException;
import org.eclipse.dirigible.core.git.project.ProjectMetadataManager;
import org.eclipse.dirigible.core.git.utils.GitFileUtils;
import org.eclipse.dirigible.core.workspace.api.IProject;
import org.eclipse.dirigible.core.workspace.api.IWorkspace;

import org.eclipse.dirigible.core.git.*;
import org.eclipse.dirigible.core.workspace.json.ProjectDescriptor;
import org.eclipse.dirigible.core.workspace.json.WorkspaceGitHelper;
import org.eclipse.dirigible.core.workspace.service.WorkspacesCoreService;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class GitFacade.
 */
public class GitFacade implements IScriptingFacade {

    /** The Constant workspacesCoreService. */
    private static final WorkspacesCoreService workspacesCoreService = new WorkspacesCoreService();

    /**
     * Inits the repository.
     *
     * @param username the username
     * @param email the email
     * @param workspaceName the workspace name
     * @param projectName the project name
     * @param repositoryName the repository name
     * @param commitMessage the commit message
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitAPIException the git API exception
     * @throws GitConnectorException the git connector exception
     */
    public static void initRepository(String username, String email, String workspaceName, String projectName, String repositoryName, String commitMessage) throws IOException, GitAPIException, GitConnectorException {
        IWorkspace workspaceObject = workspacesCoreService.getWorkspace(workspaceName);
        IProject projectObject = workspaceObject.getProject(projectName);
        ensureProjectJsonIsCreatedForProject(workspaceObject, projectName);
        String user = UserFacade.getName();
        File gitDirectory = GitFileUtils.getGitDirectoryByRepositoryName(workspaceName, repositoryName);
        boolean isExistingGitRepository = gitDirectory != null;

        if (!isExistingGitRepository) {
            gitDirectory = GitFileUtils.createGitDirectory(user, workspaceName, repositoryName);
        } else {
            throw new RefAlreadyExistsException("Git repository already exists");
        }
        GitFileUtils.copyProjectToDirectory(projectObject, gitDirectory);

        projectObject.delete();

        File projectGitDirectory = new File(gitDirectory, projectObject.getName());

        GitConnectorFactory.initRepository(gitDirectory.getCanonicalPath(), false);
        GitFileUtils.importProjectFromGitRepositoryToWorkspace(projectGitDirectory, projectObject.getPath());

        //the code below is needed because otherwise getHistory method will throw an error in the git perspective
        IGitConnector gitConnector = GitConnectorFactory.getConnector(gitDirectory.getCanonicalPath());
        gitConnector.add(IGitConnector.GIT_ADD_ALL_FILE_PATTERN);
        gitConnector.commit(commitMessage, username, email, true);
    }

    /**
     * Ensure project json is created for project.
     *
     * @param workspace the workspace
     * @param projectName the project name
     */
    private static void ensureProjectJsonIsCreatedForProject(IWorkspace workspace, String projectName) {
        ProjectMetadataManager projectMetadataManager = new ProjectMetadataManager();
        projectMetadataManager.ensureProjectMetadata(workspace, projectName);
    }

    /**
     * Commit.
     *
     * @param username the username
     * @param email the email
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @param commitMessage the commit message
     * @param add the add
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static void commit(String username, String email, final String workspaceName, String repositoryName, String commitMessage, Boolean add) throws GitAPIException, IOException, GitConnectorException {
        String user = UserFacade.getName();
        File tempGitDirectory = GitFileUtils.getGitDirectory(user, workspaceName, repositoryName);
        boolean isExistingGitRepository = tempGitDirectory != null;
        if (!isExistingGitRepository) {
            throw new RefNotFoundException("Repository not found");
        }

        IGitConnector gitConnector = GitConnectorFactory.getConnector(tempGitDirectory.getCanonicalPath());
        gitConnector.add(IGitConnector.GIT_ADD_ALL_FILE_PATTERN);
        gitConnector.commit(commitMessage, username, email, add);
    }

    /**
     * Gets the git repositories.
     *
     * @param workspaceName the workspace name
     * @return the git repositories
     */
    public static List<ProjectDescriptor> getGitRepositories(String workspaceName) {
        String user = UserFacade.getName();
        IWorkspace workspaceObject = workspacesCoreService.getWorkspace(workspaceName);
        if (!workspaceObject.exists()) {
            return null;
        }

        List<ProjectDescriptor> gitRepositories = new ArrayList<ProjectDescriptor>();
        File gitDirectory = GitFileUtils.getGitDirectory(user, workspaceName);
        if (gitDirectory != null) {
            for (File next : gitDirectory.listFiles()) {
                if (!next.isFile()) {
                    gitRepositories.add(WorkspaceGitHelper.describeProject(next));
                }
            }
        }
        return gitRepositories;
    }

    /**
     * Gets the history.
     *
     * @param repositoryName the repository name
     * @param workspaceName the workspace name
     * @param path the path
     * @return the history
     * @throws GitConnectorException the git connector exception
     */
    public static List<GitCommitInfo> getHistory(String repositoryName, String workspaceName, String path) throws GitConnectorException {
        try {
            List<GitCommitInfo> history = getConnector(workspaceName, repositoryName).getHistory(path);
            return history;
        } catch (Exception e) {
            throw new GitConnectorException(e);
        }
    }

    /**
     * Delete repository.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @throws GitConnectorException the git connector exception
     */
    public static void deleteRepository(String workspaceName, String repositoryName) throws GitConnectorException {
        try {

            File gitRepository = GitFileUtils.getGitDirectoryByRepositoryName(workspaceName, repositoryName);
            if (gitRepository == null) {
                throw new RefNotFoundException("Repository not found");
            }

            IWorkspace workspaceApi = workspacesCoreService.getWorkspace(workspaceName);
            IProject[] workspaceProjects = workspaceApi.getProjects().toArray(new IProject[0]);
            for (IProject next : workspaceProjects) {
                if (next.exists()) {
                    next.delete();
                }
            }

            FileUtils.deleteDirectory(gitRepository);
        } catch (IOException | RefNotFoundException e) {
            throw new GitConnectorException("Unable to delete Git repository [" + repositoryName + "]", e);
        }
    }

    /**
     * Clone repository.
     *
     * @param workspaceName the workspace name
     * @param repositoryUri the repository uri
     * @param username the username
     * @param password the password
     * @param branch the branch
     * @return the i git connector
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitAPIException the git API exception
     */
    public static IGitConnector cloneRepository(String workspaceName, String repositoryUri, String username, String password, String branch) throws IOException, GitAPIException {
        String user = UserFacade.getName();
        File gitDirectory = GitFileUtils.createGitDirectory(user, workspaceName, repositoryUri);
        return GitConnectorFactory.cloneRepository(gitDirectory.getCanonicalPath(), repositoryUri, username, password, branch);
    }

    /**
     * Pull.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @param username the username
     * @param password the password
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static void pull(String workspaceName, String repositoryName, String username, String password) throws GitAPIException, IOException, GitConnectorException {
        getConnector(workspaceName, repositoryName).pull(username, password);
    }

    /**
     * Push.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @param username the username
     * @param password the password
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static void push(String workspaceName, String repositoryName, String username, String password) throws GitAPIException, IOException, GitConnectorException {
        getConnector(workspaceName, repositoryName).push(username, password);
    }

    /**
     * Checkout.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @param branchName the branch name
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static void checkout(String workspaceName, String repositoryName, String branchName) throws GitAPIException, IOException, GitConnectorException {
        getConnector(workspaceName, repositoryName).checkout(branchName);
    }

    /**
     * Creates the branch.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @param branchName the branch name
     * @param startingPoint the starting point
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static void createBranch(String workspaceName, String repositoryName, String branchName, String startingPoint) throws GitAPIException, IOException, GitConnectorException {
        getConnector(workspaceName, repositoryName).createBranch(branchName, startingPoint);
    }

    /**
     * Hard reset.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static void hardReset(String workspaceName, String repositoryName) throws GitAPIException, IOException, GitConnectorException {
        getConnector(workspaceName, repositoryName).hardReset();
    }

    /**
     * Rebase.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @param branchName the branch name
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static void rebase(String workspaceName, String repositoryName, String branchName) throws GitAPIException, IOException, GitConnectorException {
        getConnector(workspaceName, repositoryName).rebase(branchName);
    }

    /**
     * Status.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @return the status
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static Status status(String workspaceName, String repositoryName) throws GitAPIException, IOException, GitConnectorException {
        return getConnector(workspaceName, repositoryName).status();
    }

    /**
     * Gets the branch.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @return the branch
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static String getBranch(String workspaceName, String repositoryName) throws GitAPIException, IOException, GitConnectorException {
        return getConnector(workspaceName, repositoryName).getBranch();
    }

    /**
     * Gets the local branches.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @return the local branches
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static List<GitBranch> getLocalBranches(String workspaceName, String repositoryName) throws GitAPIException, IOException, GitConnectorException {
        return getConnector(workspaceName, repositoryName).getLocalBranches();
    }

    /**
     * Gets the remote branches.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @return the remote branches
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static List<GitBranch> getRemoteBranches(String workspaceName, String repositoryName) throws GitAPIException, IOException, GitConnectorException {
        return getConnector(workspaceName, repositoryName).getRemoteBranches();
    }

    /**
     * Gets the unstaged changes.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @return the unstaged changes
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static List<GitChangedFile> getUnstagedChanges(String workspaceName, String repositoryName) throws GitAPIException, IOException, GitConnectorException {
        return getConnector(workspaceName, repositoryName).getUnstagedChanges();
    }

    /**
     * Gets the staged changes.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @return the staged changes
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static List<GitChangedFile> getStagedChanges(String workspaceName, String repositoryName) throws GitAPIException, IOException, GitConnectorException {
        return getConnector(workspaceName, repositoryName).getStagedChanges();
    }

    /**
     * Gets the file content.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @param filePath the file path
     * @param revStr the rev str
     * @return the file content
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    public static String getFileContent(String workspaceName, String repositoryName, String filePath, String revStr) throws GitAPIException, IOException, GitConnectorException {
        return getConnector(workspaceName, repositoryName).getFileContent(filePath, revStr);
    }

    /**
     * Gets the connector.
     *
     * @param workspaceName the workspace name
     * @param repositoryName the repository name
     * @return the connector
     * @throws GitConnectorException the git connector exception
     */
    private static IGitConnector getConnector(String workspaceName, String repositoryName) throws GitConnectorException {
        try {
            String user = UserFacade.getName();
            File gitDirectory = GitFileUtils.getGitDirectory(user, workspaceName, repositoryName);
            IGitConnector gitConnector = GitConnectorFactory.getConnector(gitDirectory.getCanonicalPath());
            return gitConnector;
        } catch (IOException e) {
            throw new GitConnectorException(e);
        }
    }
}
