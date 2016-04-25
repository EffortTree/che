/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.git.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.git.shared.AddRequest;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.BranchCreateRequest;
import org.eclipse.che.api.git.shared.BranchDeleteRequest;
import org.eclipse.che.api.git.shared.BranchListRequest;
import org.eclipse.che.api.git.shared.CheckoutRequest;
import org.eclipse.che.api.git.shared.CloneRequest;
import org.eclipse.che.api.git.shared.CommitRequest;
import org.eclipse.che.api.git.shared.Commiters;
import org.eclipse.che.api.git.shared.ConfigRequest;
import org.eclipse.che.api.git.shared.DiffRequest;
import org.eclipse.che.api.git.shared.FetchRequest;
import org.eclipse.che.api.git.shared.GitUrlVendorInfo;
import org.eclipse.che.api.git.shared.InitRequest;
import org.eclipse.che.api.git.shared.LogRequest;
import org.eclipse.che.api.git.shared.LogResponse;
import org.eclipse.che.api.git.shared.MergeRequest;
import org.eclipse.che.api.git.shared.MergeResult;
import org.eclipse.che.api.git.shared.PullRequest;
import org.eclipse.che.api.git.shared.PullResponse;
import org.eclipse.che.api.git.shared.PushRequest;
import org.eclipse.che.api.git.shared.PushResponse;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.api.git.shared.RemoteAddRequest;
import org.eclipse.che.api.git.shared.RemoteListRequest;
import org.eclipse.che.api.git.shared.RepoInfo;
import org.eclipse.che.api.git.shared.ResetRequest;
import org.eclipse.che.api.git.shared.Revision;
import org.eclipse.che.api.git.shared.RmRequest;
import org.eclipse.che.api.git.shared.ShowFileContentRequest;
import org.eclipse.che.api.git.shared.ShowFileContentResponse;
import org.eclipse.che.api.git.shared.Status;
import org.eclipse.che.api.git.shared.StatusFormat;
import org.eclipse.che.api.machine.gwt.client.DevMachine;
import org.eclipse.che.api.machine.gwt.client.WsAgentStateController;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.HTTPHeader;
import org.eclipse.che.ide.rest.StringMapUnmarshaller;
import org.eclipse.che.ide.rest.StringUnmarshaller;
import org.eclipse.che.ide.ui.loaders.request.LoaderFactory;
import org.eclipse.che.ide.websocket.Message;
import org.eclipse.che.ide.websocket.MessageBuilder;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.RequestCallback;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.gwt.http.client.RequestBuilder.POST;
import static org.eclipse.che.api.git.shared.StatusFormat.PORCELAIN;
import static org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.createFromAsyncRequest;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.MimeType.TEXT_PLAIN;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENTTYPE;
import static org.eclipse.che.ide.util.Arrays.isNullOrEmpty;

/**
 * Implementation of the {@link GitServiceClient}.
 *
 * @author Ann Zhuleva
 * @author Valeriy Svydenko
 */
@Singleton
public class GitServiceClientImpl implements GitServiceClient {
    public static final String ADD               = "/add";
    public static final String BRANCH_LIST       = "/branch-list";
    public static final String CHECKOUT          = "/checkout";
    public static final String BRANCH_CREATE     = "/branch-create";
    public static final String BRANCH_DELETE     = "/branch-delete";
    public static final String BRANCH_RENAME     = "/branch-rename";
    public static final String CLONE             = "/clone";
    public static final String COMMIT            = "/commit";
    public static final String CONFIG            = "/config";
    public static final String DIFF              = "/diff";
    public static final String FETCH             = "/fetch";
    public static final String INIT              = "/init";
    public static final String LOG               = "/log";
    public static final String SHOW              = "/show";
    public static final String MERGE             = "/merge";
    public static final String STATUS            = "/status";
    public static final String RO_URL            = "/read-only-url";
    public static final String PUSH              = "/push";
    public static final String PULL              = "/pull";
    public static final String REMOTE_LIST       = "/remote-list";
    public static final String REMOTE_ADD        = "/remote-add";
    public static final String REMOTE_DELETE     = "/remote-delete";
    public static final String REMOVE            = "/rm";
    public static final String RESET             = "/reset";
    public static final String COMMITERS         = "/commiters";
    public static final String DELETE_REPOSITORY = "/delete-repository";

    /** Loader to be displayed. */
    private final AsyncRequestLoader     loader;
    private final WsAgentStateController wsAgentStateController;
    private final DtoFactory             dtoFactory;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final AsyncRequestFactory    asyncRequestFactory;
    private final AppContext             appContext;

    @Inject
    protected GitServiceClientImpl(LoaderFactory loaderFactory,
                                   WsAgentStateController wsAgentStateController,
                                   DtoFactory dtoFactory,
                                   AsyncRequestFactory asyncRequestFactory,
                                   DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                   AppContext appContext) {
        this.appContext = appContext;
        this.loader = loaderFactory.newLoader();
        this.wsAgentStateController = wsAgentStateController;
        this.dtoFactory = dtoFactory;
        this.asyncRequestFactory = asyncRequestFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void init(String workspaceId, ProjectConfigDto project, boolean bare, final RequestCallback<Void> callback)
            throws WebSocketException {
        InitRequest initRequest = dtoFactory.createDto(InitRequest.class);
        initRequest.setBare(bare);
        initRequest.setWorkingDir(project.getName());

        String url = "/git/" + workspaceId + INIT + "?projectPath=" + project.getPath();

        MessageBuilder builder = new MessageBuilder(POST, url);
        builder.data(dtoFactory.toJson(initRequest)).header(CONTENTTYPE, APPLICATION_JSON);
        Message message = builder.build();

        sendMessageToWS(message, callback);
    }

    @Override
    public Promise<Void> init(String wsId, Path project, boolean bare) {
        InitRequest initRequest = dtoFactory.createDto(InitRequest.class);
        initRequest.setBare(bare);
        initRequest.setWorkingDir(project.toString());

        String url = extPath + "/git/" + wsId + INIT + "?projectPath=" + project;

        return asyncRequestFactory.createPostRequest(url, initRequest).send();
    }

    /** {@inheritDoc} */
    @Override
    public void cloneRepository(String workspaceId,
                                ProjectConfigDto project,
                                String remoteUri,
                                String remoteName,
                                RequestCallback<RepoInfo> callback) throws WebSocketException {
        CloneRequest cloneRequest = dtoFactory.createDto(CloneRequest.class)
                                              .withRemoteName(remoteName)
                                              .withRemoteUri(remoteUri)
                                              .withWorkingDir(project.getPath());

        String params = "?projectPath=" + project.getPath();

        String url = "/git/" + workspaceId + CLONE + params;

        MessageBuilder builder = new MessageBuilder(POST, url);
        builder.data(dtoFactory.toJson(cloneRequest))
               .header(CONTENTTYPE, APPLICATION_JSON)
               .header(ACCEPT, APPLICATION_JSON);
        Message message = builder.build();

        sendMessageToWS(message, callback);
    }

    private void sendMessageToWS(final @NotNull Message message, final @NotNull RequestCallback<?> callback) {
        wsAgentStateController.getMessageBus().then(new Operation<MessageBus>() {
            @Override
            public void apply(MessageBus arg) throws OperationException {
                try {
                    arg.send(message, callback);
                } catch (WebSocketException e) {
                    throw new OperationException(e.getMessage(), e);
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void statusText(String workspaceId, ProjectConfigDto project, StatusFormat format, AsyncRequestCallback<String> callback) {
        String url = extPath + "/git/" + workspaceId + STATUS;
        String params = "?projectPath=" + project.getPath() + "&format=" + format;

        asyncRequestFactory.createPostRequest(url + params, null)
                           .loader(loader)
                           .header(CONTENTTYPE, APPLICATION_JSON)
                           .header(ACCEPT, TEXT_PLAIN)
                           .send(callback);
    }

    @Override
    public Promise<String> statusText(String wsId, Path project, StatusFormat format) {
        String url = extPath + "/git/" + wsId + STATUS;
        String params = "?projectPath=" + project + "&format=" + format;

        return asyncRequestFactory.createPostRequest(url + params, null)
                                  .loader(loader)
                                  .header(CONTENTTYPE, APPLICATION_JSON)
                                  .header(ACCEPT, TEXT_PLAIN)
                                  .send(new StringUnmarshaller());
    }

    /** {@inheritDoc} */
    @Override
    public void add(String workspaceId,
                    ProjectConfigDto project,
                    boolean update,
                    @Nullable List<String> filePattern,
                    RequestCallback<Void> callback) throws WebSocketException {
        AddRequest addRequest = dtoFactory.createDto(AddRequest.class).withUpdate(update);
        if (filePattern == null) {
            addRequest.setFilepattern(AddRequest.DEFAULT_PATTERN);
        } else {
            addRequest.setFilepattern(filePattern);
        }
        String url = "/git/" + workspaceId + ADD + "?projectPath=" + project.getPath();

        MessageBuilder builder = new MessageBuilder(POST, url);
        builder.data(dtoFactory.toJson(addRequest))
               .header(CONTENTTYPE, APPLICATION_JSON);
        Message message = builder.build();

        sendMessageToWS(message, callback);
    }

    @Override
    public Promise<Void> add(final String wsId, final Path project, final boolean update, final Path[] paths) {
        return createFromAsyncRequest(new RequestCall<Void>() {
            @Override
            public void makeCall(final AsyncCallback<Void> callback) {
                final AddRequest addRequest = dtoFactory.createDto(AddRequest.class).withUpdate(update);

                if (paths == null) {
                    addRequest.setFilepattern(AddRequest.DEFAULT_PATTERN);
                } else {

                    final List<String> patterns = new ArrayList<>(); //need for compatible with server side
                    for (Path path : paths) {
                        patterns.add(path.toString());
                    }

                    addRequest.setFilepattern(patterns);
                }

                final String url = "/git/" + wsId + ADD + "?projectPath=" + project.toString();
                final Message message = new MessageBuilder(POST, url).data(dtoFactory.toJson(addRequest))
                                                                     .header(CONTENTTYPE, APPLICATION_JSON)
                                                                     .build();

                sendMessageToWS(message, new RequestCallback<Void>() {
                    @Override
                    protected void onSuccess(Void result) {
                        callback.onSuccess(result);
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void commit(String workspaceId,
                       ProjectConfigDto project,
                       String message,
                       boolean all,
                       boolean amend,
                       AsyncRequestCallback<Revision> callback) {
        CommitRequest commitRequest = dtoFactory.createDto(CommitRequest.class)
                                                .withMessage(message)
                                                .withAmend(amend)
                                                .withAll(all);
        String url = extPath + "/git/" + workspaceId + COMMIT + "?projectPath=" + project.getPath();

        asyncRequestFactory.createPostRequest(url, commitRequest).loader(loader).send(callback);
    }

    @Override
    public Promise<Revision> commit(String wsId, Path project, String message, boolean all, boolean amend) {
        CommitRequest commitRequest = dtoFactory.createDto(CommitRequest.class)
                                                .withMessage(message)
                                                .withAmend(amend)
                                                .withAll(all);
        String url = extPath + "/git/" + wsId + COMMIT + "?projectPath=" + project;

        return asyncRequestFactory.createPostRequest(url, commitRequest)
                                  .loader(loader)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(Revision.class));
    }

    @Override
    public void commit(final String workspaceId,
                       final ProjectConfigDto project,
                       final String message,
                       final List<String> files,
                       final boolean amend,
                       final AsyncRequestCallback<Revision> callback) {
        CommitRequest commitRequest = dtoFactory.createDto(CommitRequest.class)
                                                .withMessage(message)
                                                .withAmend(amend)
                                                .withAll(false)
                                                .withFiles(files);
        String url = extPath + "/git/" + workspaceId + COMMIT + "?projectPath=" + project.getPath();

        asyncRequestFactory.createPostRequest(url, commitRequest).loader(loader).send(callback);
    }

    @Override
    public Promise<Revision> commit(String wsId, Path project, String message, Path[] files, boolean amend) {

        List<String> paths = new ArrayList<>(files.length);

        for (Path file : files) {
            paths.add(file.toString());
        }

        CommitRequest commitRequest = dtoFactory.createDto(CommitRequest.class)
                                                .withMessage(message)
                                                .withAmend(amend)
                                                .withAll(false)
                                                .withFiles(paths);
        String url = extPath + "/git/" + wsId + COMMIT + "?projectPath=" + project;

        return asyncRequestFactory.createPostRequest(url, commitRequest)
                                  .loader(loader)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(Revision.class));
    }

    /** {@inheritDoc} */
    @Override
    public void config(String workspaceId,
                       ProjectConfigDto project,
                       @Nullable List<String> entries,
                       boolean all,
                       AsyncRequestCallback<Map<String, String>> callback) {
        ConfigRequest configRequest = dtoFactory.createDto(ConfigRequest.class)
                                                .withGetAll(all)
                                                .withConfigEntry(entries);
        String url = extPath + "/git/" + workspaceId + CONFIG + "?projectPath=" + project.getPath();

        asyncRequestFactory.createPostRequest(url, configRequest).loader(loader).send(callback);
    }

    @Override
    public Promise<Map<String, String>> config(String wsId, Path project, List<String> entries, boolean all) {
        ConfigRequest configRequest = dtoFactory.createDto(ConfigRequest.class)
                                                .withGetAll(all)
                                                .withConfigEntry(entries);
        String url = extPath + "/git/" + wsId + CONFIG + "?projectPath=" + project;

        return asyncRequestFactory.createPostRequest(url, configRequest).loader(loader).send(new StringMapUnmarshaller());
    }

    /** {@inheritDoc} */
    @Override
    public void push(String workspaceId,
                     ProjectConfigDto project,
                     List<String> refSpec,
                     String remote,
                     boolean force,
                     AsyncRequestCallback<PushResponse> callback) {
        PushRequest pushRequest = dtoFactory.createDto(PushRequest.class).withRemote(remote).withRefSpec(refSpec).withForce(force);
        String url = extPath + "/git/" + workspaceId + PUSH + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, pushRequest).send(callback);
    }

    @Override
    public Promise<PushResponse> push(String wsId, ProjectConfigDto project, List<String> refSpec, String remote, boolean force) {
        PushRequest pushRequest = dtoFactory.createDto(PushRequest.class)
                                            .withRemote(remote)
                                            .withRefSpec(refSpec)
                                            .withForce(force);
        return asyncRequestFactory.createPostRequest(extPath + "/git/" + wsId + PUSH + "?projectPath=" + project.getPath(), pushRequest)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(PushResponse.class));
    }

    @Override
    public Promise<PushResponse> push(String wsId, Path project, List<String> refSpec, String remote, boolean force) {
        PushRequest pushRequest = dtoFactory.createDto(PushRequest.class)
                                            .withRemote(remote)
                                            .withRefSpec(refSpec)
                                            .withForce(force);
        return asyncRequestFactory.createPostRequest(extPath + "/git/" + wsId + PUSH + "?projectPath=" + project, pushRequest)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(PushResponse.class));
    }

    /** {@inheritDoc} */
    @Override
    public void remoteList(String workspaceId,
                           ProjectConfigDto project,
                           @Nullable String remoteName,
                           boolean verbose,
                           AsyncRequestCallback<List<Remote>> callback) {
        RemoteListRequest remoteListRequest = dtoFactory.createDto(RemoteListRequest.class).withVerbose(verbose);
        if (remoteName != null) {
            remoteListRequest.setRemote(remoteName);
        }
        String url = extPath + "/git/" + workspaceId + REMOTE_LIST + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, remoteListRequest).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<List<Remote>> remoteList(String workspaceId, ProjectConfigDto project, @Nullable String remoteName, boolean verbose) {
        RemoteListRequest remoteListRequest = dtoFactory.createDto(RemoteListRequest.class).withVerbose(verbose);
        if (remoteName != null) {
            remoteListRequest.setRemote(remoteName);
        }
        String url = extPath + "/git/" + workspaceId + REMOTE_LIST + "?projectPath=" + project.getPath();
        return asyncRequestFactory.createPostRequest(url, remoteListRequest)
                                  .loader(loader)
                                  .send(dtoUnmarshallerFactory.newListUnmarshaller(Remote.class));
    }

    @Override
    public Promise<List<Remote>> remoteList(String wsId, Path project, String remote, boolean verbose) {
        RemoteListRequest remoteListRequest = dtoFactory.createDto(RemoteListRequest.class).withVerbose(verbose);
        if (remote != null) {
            remoteListRequest.setRemote(remote);
        }
        String url = extPath + "/git/" + wsId + REMOTE_LIST + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, remoteListRequest)
                                  .loader(loader)
                                  .send(dtoUnmarshallerFactory.newListUnmarshaller(Remote.class));
    }

    /** {@inheritDoc} */
    @Override
    public void branchList(String workspaceId,
                           ProjectConfigDto project,
                           @Nullable String remoteMode,
                           AsyncRequestCallback<List<Branch>> callback) {
        BranchListRequest branchListRequest = dtoFactory.createDto(BranchListRequest.class).withListMode(remoteMode);
        String url = extPath + "/git/" + workspaceId + BRANCH_LIST + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, branchListRequest).send(callback);
    }

    @Override
    public Promise<List<Branch>> branchList(String wsId, Path project, String mode) {
        BranchListRequest branchListRequest = dtoFactory.createDto(BranchListRequest.class).withListMode(mode);
        String url = extPath + "/git/" + wsId + BRANCH_LIST + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, branchListRequest).send(dtoUnmarshallerFactory.newListUnmarshaller(Branch.class));
    }

    @Override
    public Promise<Status> status(String workspaceId, ProjectConfigDto project) {
        final String params = "?projectPath=" + project.getPath() + "&format=" + PORCELAIN;
        final String url = extPath + "/git/" + workspaceId + STATUS + params;
        return asyncRequestFactory.createPostRequest(url, null)
                                  .loader(loader)
                                  .header(CONTENTTYPE, APPLICATION_JSON)
                                  .header(ACCEPT, APPLICATION_JSON)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(Status.class));
    }

    @Override
    public Promise<Status> getStatus(String wsId, Path project) {
        final String params = "?projectPath=" + project.toString() + "&format=" + PORCELAIN;
        final String url = extPath + "/git/" + wsId + STATUS + params;
        return asyncRequestFactory.createPostRequest(url, null)
                                  .loader(loader)
                                  .header(CONTENTTYPE, APPLICATION_JSON)
                                  .header(ACCEPT, APPLICATION_JSON)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(Status.class));
    }

    /** {@inheritDoc} */
    @Override
    public void status(String workspaceId, ProjectConfigDto project, AsyncRequestCallback<Status> callback) {
        String params = "?projectPath=" + project.getPath() + "&format=" + PORCELAIN;
        String url = extPath + "/git/" + workspaceId + STATUS + params;
        asyncRequestFactory.createPostRequest(url, null).loader(loader)
                           .header(CONTENTTYPE, APPLICATION_JSON)
                           .header(ACCEPT, APPLICATION_JSON)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void branchDelete(String workspaceId,
                             ProjectConfigDto project,
                             String name,
                             boolean force,
                             AsyncRequestCallback<String> callback) {
        BranchDeleteRequest branchDeleteRequest = dtoFactory.createDto(BranchDeleteRequest.class).withName(name).withForce(force);
        String url = extPath + "/git/" + workspaceId + BRANCH_DELETE + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, branchDeleteRequest).loader(loader).send(callback);
    }

    @Override
    public Promise<Void> branchDelete(String wsId, Path project, String name, boolean force) {
        BranchDeleteRequest branchDeleteRequest = dtoFactory.createDto(BranchDeleteRequest.class).withName(name).withForce(force);
        String url = extPath + "/git/" + wsId + BRANCH_DELETE + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, branchDeleteRequest).loader(loader).send();
    }

    /** {@inheritDoc} */
    @Override
    public void branchRename(String workspaceId,
                             ProjectConfigDto project,
                             String oldName,
                             String newName,
                             AsyncRequestCallback<String> callback) {
        String params = "?projectPath=" + project.getPath() + "&oldName=" + oldName + "&newName=" + newName;
        String url = extPath + "/git/" + workspaceId + BRANCH_RENAME + params;
        asyncRequestFactory.createPostRequest(url, null).loader(loader)
                           .header(CONTENTTYPE, MimeType.APPLICATION_FORM_URLENCODED)
                           .send(callback);
    }

    @Override
    public Promise<Void> branchRename(String wsId, Path project, String oldName, String newName) {
        String params = "?projectPath=" + project + "&oldName=" + oldName + "&newName=" + newName;
        String url = extPath + "/git/" + wsId + BRANCH_RENAME + params;
        return asyncRequestFactory.createPostRequest(url, null).loader(loader)
                                  .header(CONTENTTYPE, MimeType.APPLICATION_FORM_URLENCODED)
                                  .send();
    }

    /** {@inheritDoc} */
    @Override
    public void branchCreate(String workspaceId, ProjectConfigDto project, String name, String startPoint,
                             AsyncRequestCallback<Branch> callback) {
        BranchCreateRequest branchCreateRequest = dtoFactory.createDto(BranchCreateRequest.class).withName(name).withStartPoint(startPoint);
        String url = extPath + "/git/" + workspaceId + BRANCH_CREATE + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, branchCreateRequest).loader(loader).header(ACCEPT, APPLICATION_JSON).send(callback);
    }

    @Override
    public Promise<Branch> branchCreate(String wsId, Path project, String name, String startPoint) {
        BranchCreateRequest branchCreateRequest = dtoFactory.createDto(BranchCreateRequest.class).withName(name).withStartPoint(startPoint);
        String url = extPath + "/git/" + wsId + BRANCH_CREATE + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, branchCreateRequest)
                                  .loader(loader)
                                  .header(ACCEPT, APPLICATION_JSON)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(Branch.class));
    }

    /** {@inheritDoc} */
    @Override
    public void checkout(String workspaceId,
                         ProjectConfigDto project,
                         CheckoutRequest checkoutRequest,
                         AsyncRequestCallback<String> callback) {
        String url = extPath + "/git/" + workspaceId + CHECKOUT + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, checkoutRequest).loader(loader).send(callback);
    }

    @Override
    public Promise<Void> checkout(String wsId,
                                  Path project,
                                  CheckoutRequest request) {

        final String url = extPath + "/git/" + wsId + CHECKOUT + "?projectPath=" + project.toString();
        return asyncRequestFactory.createPostRequest(url, request).loader(loader).send();
    }

    /** {@inheritDoc} */
    @Override
    public void remove(String workspaceId,
                       ProjectConfigDto project,
                       List<String> items,
                       boolean cached,
                       AsyncRequestCallback<String> callback) {
        RmRequest rmRequest = dtoFactory.createDto(RmRequest.class).withItems(items).withCached(cached).withRecursively(true);
        String url = extPath + "/git/" + workspaceId + REMOVE + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, rmRequest).loader(loader).send(callback);
    }

    @Override
    public Promise<Void> remove(String wsId, Path project, Path[] items, boolean cached) {
        List<String> files = new ArrayList<>();

        if (items != null) {
            for (Path item : items) {
                files.add(item.toString());
            }
        }

        RmRequest rmRequest = dtoFactory.createDto(RmRequest.class).withItems(files).withCached(cached).withRecursively(true);
        String url = extPath + "/git/" + wsId + REMOVE + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, rmRequest).loader(loader).send();
    }

    /** {@inheritDoc} */
    @Override
    public void reset(String workspaceId,
                      ProjectConfigDto project,
                      String commit,
                      @Nullable ResetRequest.ResetType resetType,
                      @Nullable List<String> filePattern,
                      AsyncRequestCallback<Void> callback) {
        ResetRequest resetRequest = dtoFactory.createDto(ResetRequest.class).withCommit(commit);
        if (resetType != null) {
            resetRequest.setType(resetType);
        }
        if (filePattern != null) {
            resetRequest.setFilePattern(filePattern);
        }
        String url = extPath + "/git/" + workspaceId + RESET + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, resetRequest).loader(loader).send(callback);
    }

    @Override
    public Promise<Void> reset(String wsId, Path project, String commit, ResetRequest.ResetType resetType, Path[] files) {
        ResetRequest resetRequest = dtoFactory.createDto(ResetRequest.class).withCommit(commit);
        if (resetType != null) {
            resetRequest.setType(resetType);
        }
        if (files != null) {
            List<String> fileList = new ArrayList<>(files.length);
            for (Path file : files) {
                fileList.add(file.toString());
            }
            resetRequest.setFilePattern(fileList);
        }
        String url = extPath + "/git/" + wsId + RESET + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, resetRequest).loader(loader).send();
    }

    /** {@inheritDoc} */
    @Override
    public void log(String workspaceId, ProjectConfigDto project, List<String> fileFilter, boolean isTextFormat,
                    @NotNull AsyncRequestCallback<LogResponse> callback) {
        LogRequest logRequest = dtoFactory.createDto(LogRequest.class).withFileFilter(fileFilter);
        String url = extPath + "/git/" + workspaceId + LOG + "?projectPath=" + project.getPath();
        if (isTextFormat) {
            asyncRequestFactory.createPostRequest(url, logRequest).send(callback);
        } else {
            asyncRequestFactory.createPostRequest(url, logRequest).loader(loader).header(ACCEPT, APPLICATION_JSON).send(callback);
        }
    }

    @Override
    public Promise<LogResponse> log(String wsId, Path project, Path[] fileFilter, boolean plainText) {

        List<String> paths = null;

        if (!isNullOrEmpty(fileFilter)) {
            paths = new ArrayList<>(fileFilter.length);

            for (Path file : fileFilter) {
                paths.add(file.toString());
            }
        }

        LogRequest logRequest = dtoFactory.createDto(LogRequest.class)
                                          .withFileFilter(paths);
        String url = extPath + "/git/" + wsId + LOG + "?projectPath=" + project;
        if (plainText) {
            return asyncRequestFactory.createPostRequest(url, logRequest)
                                      .loader(loader)
                                      .send(dtoUnmarshallerFactory.newUnmarshaller(LogResponse.class));
        } else {
            return asyncRequestFactory.createPostRequest(url, logRequest)
                                      .loader(loader)
                                      .header(ACCEPT, APPLICATION_JSON)
                                      .send(dtoUnmarshallerFactory.newUnmarshaller(LogResponse.class));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void remoteAdd(String workspaceId,
                          ProjectConfigDto project,
                          String name,
                          String repositoryURL,
                          AsyncRequestCallback<String> callback) {
        RemoteAddRequest remoteAddRequest = dtoFactory.createDto(RemoteAddRequest.class).withName(name).withUrl(repositoryURL);
        String url = extPath + "/git/" + workspaceId + REMOTE_ADD + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, remoteAddRequest).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void remoteDelete(String workspaceId,
                             ProjectConfigDto project,
                             String name,
                             AsyncRequestCallback<String> callback) {
        String url = extPath + "/git/" + workspaceId + REMOTE_DELETE + '/' + name + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, null).loader(loader).send(callback);
    }

    @Override
    public Promise<Void> remoteDelete(String wsId, Path project, String name) {
        String url = extPath + "/git/" + wsId + REMOTE_DELETE + '/' + name + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, null).loader(loader).send();
    }

    /** {@inheritDoc} */
    @Override
    public void fetch(String workspaceId,
                      ProjectConfigDto project,
                      String remote,
                      List<String> refspec,
                      boolean removeDeletedRefs,
                      RequestCallback<String> callback) throws WebSocketException {
        FetchRequest fetchRequest = dtoFactory.createDto(FetchRequest.class)
                                              .withRefSpec(refspec)
                                              .withRemote(remote)
                                              .withRemoveDeletedRefs(removeDeletedRefs);

        String url = "/git/" + workspaceId + FETCH + "?projectPath=" + project.getPath();
        MessageBuilder builder = new MessageBuilder(POST, url);
        builder.data(dtoFactory.toJson(fetchRequest))
               .header(CONTENTTYPE, APPLICATION_JSON);
        Message message = builder.build();

        sendMessageToWS(message, callback);
    }

    @Override
    public Promise<Void> fetch(String wsId, Path project, String remote, List<String> refspec, boolean removeDeletedRefs) {
        FetchRequest fetchRequest = dtoFactory.createDto(FetchRequest.class)
                                              .withRefSpec(refspec)
                                              .withRemote(remote)
                                              .withRemoveDeletedRefs(removeDeletedRefs);
        String url = extPath + "/git/" + wsId + FETCH + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, fetchRequest).send();
    }

    /** {@inheritDoc} */
    @Override
    public void pull(String workspaceId,
                     ProjectConfigDto project,
                     String refSpec,
                     String remote,
                     AsyncRequestCallback<PullResponse> callback) {
        PullRequest pullRequest = dtoFactory.createDto(PullRequest.class).withRemote(remote).withRefSpec(refSpec);
        String url = extPath + "/git/" + workspaceId + PULL + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, pullRequest).send(callback);
    }

    @Override
    public Promise<PullResponse> pull(String wsId, Path project, String refSpec, String remote) {
        PullRequest pullRequest = dtoFactory.createDto(PullRequest.class).withRemote(remote).withRefSpec(refSpec);
        String url = extPath + "/git/" + wsId + PULL + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, pullRequest).send(dtoUnmarshallerFactory.newUnmarshaller(PullResponse.class));
    }

    /** {@inheritDoc} */
    @Override
    public void diff(String workspaceId,
                     ProjectConfigDto project,
                     List<String> fileFilter,
                     DiffRequest.DiffType type,
                     boolean noRenames,
                     int renameLimit,
                     String commitA,
                     String commitB, @NotNull AsyncRequestCallback<String> callback) {
        DiffRequest diffRequest = dtoFactory.createDto(DiffRequest.class)
                                            .withFileFilter(fileFilter)
                                            .withType(type)
                                            .withNoRenames(noRenames)
                                            .withCommitA(commitA)
                                            .withCommitB(commitB)
                                            .withRenameLimit(renameLimit);

        diff(workspaceId, diffRequest, project.getPath(), callback);
    }

    @Override
    public Promise<String> diff(String wsId,
                                Path project,
                                List<String> fileFilter,
                                DiffRequest.DiffType type,
                                boolean noRenames,
                                int renameLimit,
                                String commitA,
                                String commitB) {
        DiffRequest diffRequest = dtoFactory.createDto(DiffRequest.class)
                                            .withFileFilter(fileFilter)
                                            .withType(type)
                                            .withNoRenames(noRenames)
                                            .withCommitA(commitA)
                                            .withCommitB(commitB)
                                            .withRenameLimit(renameLimit);

        String url = extPath + "/git/" + wsId + DIFF + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, diffRequest).loader(loader).send(new StringUnmarshaller());
    }

    /** {@inheritDoc} */
    @Override
    public void showFileContent(String workspaceId,
                                @NotNull ProjectConfigDto project,
                                String file,
                                String version,
                                @NotNull AsyncRequestCallback<ShowFileContentResponse> callback) {
        ShowFileContentRequest showRequest = dtoFactory.createDto(ShowFileContentRequest.class).withFile(file).withVersion(version);
        String url = extPath + "/git/" + workspaceId + SHOW + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, showRequest).loader(loader).send(callback);
    }

    @Override
    public Promise<ShowFileContentResponse> showFileContent(String wsId, Path project, Path file, String version) {
        ShowFileContentRequest showRequest = dtoFactory.createDto(ShowFileContentRequest.class)
                                                       .withFile(file.toString())
                                                       .withVersion(version);
        String url = extPath + "/git/" + wsId + SHOW + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, showRequest)
                                  .loader(loader)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(ShowFileContentResponse.class));
    }

    /** {@inheritDoc} */
    @Override
    public void diff(String workspaceId,
                     ProjectConfigDto project,
                     List<String> fileFilter,
                     DiffRequest.DiffType type,
                     boolean noRenames,
                     int renameLimit,
                     String commitA,
                     boolean cached,
                     AsyncRequestCallback<String> callback) {
        DiffRequest diffRequest = dtoFactory.createDto(DiffRequest.class)
                                            .withFileFilter(fileFilter).withType(type)
                                            .withNoRenames(noRenames)
                                            .withCommitA(commitA)
                                            .withRenameLimit(renameLimit)
                                            .withCached(cached);

        diff(workspaceId, diffRequest, project.getPath(), callback);
    }

    @Override
    public Promise<String> diff(String wsId,
                                Path project,
                                List<String> files,
                                DiffRequest.DiffType type,
                                boolean noRenames,
                                int renameLimit,
                                String commitA,
                                boolean cached) {
        DiffRequest diffRequest = dtoFactory.createDto(DiffRequest.class)
                                            .withFileFilter(files)
                                            .withType(type)
                                            .withNoRenames(noRenames)
                                            .withCommitA(commitA)
                                            .withRenameLimit(renameLimit)
                                            .withCached(cached);

        String url = extPath + "/git/" + wsId + DIFF + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, diffRequest).loader(loader).send(new StringUnmarshaller());
    }

    /**
     * Make diff request.
     *
     * @param diffRequest
     *         request for diff
     * @param projectPath
     *         project path
     * @param callback
     *         callback
     */
    private void diff(String workspaceId, DiffRequest diffRequest, @NotNull String projectPath, AsyncRequestCallback<String> callback) {
        String url = extPath + "/git/" + workspaceId + DIFF + "?projectPath=" + projectPath;
        asyncRequestFactory.createPostRequest(url, diffRequest).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void merge(String workspaceId,
                      ProjectConfigDto project,
                      String commit,
                      AsyncRequestCallback<MergeResult> callback) {
        MergeRequest mergeRequest = dtoFactory.createDto(MergeRequest.class).withCommit(commit);
        String url = extPath + "/git/" + workspaceId + MERGE + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, mergeRequest).loader(loader)
                           .header(ACCEPT, APPLICATION_JSON)
                           .send(callback);
    }

    @Override
    public Promise<MergeResult> merge(String wsId, Path project, String commit) {
        MergeRequest mergeRequest = dtoFactory.createDto(MergeRequest.class).withCommit(commit);
        String url = extPath + "/git/" + wsId + MERGE + "?projectPath=" + project;
        return asyncRequestFactory.createPostRequest(url, mergeRequest)
                                  .loader(loader)
                                  .header(ACCEPT, APPLICATION_JSON)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(MergeResult.class));
    }

    /** {@inheritDoc} */
    @Override
    public void getGitReadOnlyUrl(String workspaceId, ProjectConfigDto project, AsyncRequestCallback<String> callback) {
        String url = extPath + "/git/" + workspaceId + RO_URL + "?projectPath=" + project.getPath();
        asyncRequestFactory.createGetRequest(url).send(callback);
    }

    @Override
    public Promise<String> getGitReadOnlyUrl(String wsId, Path project) {
        String url = extPath + "/git/" + wsId + RO_URL + "?projectPath=" + project;
        return asyncRequestFactory.createGetRequest(url).send(new StringUnmarshaller());
    }

    /** {@inheritDoc} */
    @Override
    public void getCommitters(String workspaceId, ProjectConfigDto project, AsyncRequestCallback<Commiters> callback) {
        String url = extPath + "/git/" + workspaceId + COMMITERS + "?projectPath=" + project.getPath();
        asyncRequestFactory.createGetRequest(url).header(ACCEPT, APPLICATION_JSON).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteRepository(String workspaceId, ProjectConfigDto project, AsyncRequestCallback<Void> callback) {
        String url = extPath + "/git/" + workspaceId + DELETE_REPOSITORY + "?projectPath=" + project.getPath();
        asyncRequestFactory.createGetRequest(url).loader(loader)
                           .header(CONTENTTYPE, APPLICATION_JSON).header(ACCEPT, TEXT_PLAIN)
                           .send(callback);
    }

    @Override
    public Promise<Void> deleteRepository(String wsId, Path project) {
        String url = extPath + "/git/" + wsId + DELETE_REPOSITORY + "?projectPath=" + project;
        return asyncRequestFactory.createGetRequest(url).loader(loader)
                                  .header(CONTENTTYPE, APPLICATION_JSON)
                                  .header(ACCEPT, TEXT_PLAIN)
                                  .send();
    }

    /** {@inheritDoc} */
    @Override
    public void getUrlVendorInfo(String workspaceId, @NotNull String vcsUrl, @NotNull AsyncRequestCallback<GitUrlVendorInfo> callback) {
        asyncRequestFactory.createGetRequest(extPath + "/git/" + workspaceId + "/git-service/info?vcsurl=" + vcsUrl)
                           .header(HTTPHeader.ACCEPT, MimeType.APPLICATION_JSON).send(
                callback);
    }
}
