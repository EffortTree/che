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
package org.eclipse.che.ide.api.resources;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.resources.marker.Marker;
import org.eclipse.che.ide.api.workspace.Workspace;
import org.eclipse.che.ide.resource.Path;

/**
 * The client side analog of file system files and directories. There are exactly three types of resources:
 * files, folders and projects.
 * <p/>
 * Workspace root is representing by {@link Container}. In which only {@link Project} is allowed to be created.
 * <p/>
 * File resources are similar to files in that they hold data directly. Folder resources are analogous to
 * directories in that they hold other resources but cannot directly hold data. Project resources group files
 * and folders into reusable clusters.
 * <p/>
 * Features of resources:
 * <ul>
 * <li>{@code Resource} objects are handles to state maintained by a workspace. That is, resources objects
 * do not actually contain data themselves but rather represent resource state and give it behaviour.</li>
 * <li>Resources are identified by type and their {@code path}, which is similar to a file system path.
 * The name of the resource is the last segment of its path. A resource's parent is located by removing
 * the last segment (the resource's name) from the resource's full path.</li>
 * </ul>
 * <p/>
 * To obtain already initialized resource in workspace you just need to inject {@link Workspace} into your
 * component and call {@link Workspace#getProjects()} or {@link Workspace#getWorkspaceRoot()}.
 * <p/>
 * Note. This interface is not intended to be implemented by clients.
 *
 * @author Vlad Zhukovskyi
 * @see Container
 * @see File
 * @see Folder
 * @see Project
 * @see Workspace
 * @see Workspace#getProjects()
 * @see Workspace#getWorkspaceRoot()
 * @since 4.0.0-RC14
 */
@Beta
public interface Resource extends Comparable<Resource> {
    /**
     * Type constant that describes {@code File} resource.
     *
     * @see Resource#getResourceType()
     * @see File
     * @since 4.0.0-RC14
     */
    int FILE = 0x1;

    /**
     * Type constant that describes {@code Folder} resource.
     *
     * @see Resource#getResourceType()
     * @see Folder
     * @since 4.0.0-RC14
     */
    int FOLDER = 0x2;

    /**
     * Type constant that describes {@code Project} resource.
     *
     * @see Resource#getResourceType()
     * @see Project
     * @since 4.0.0-RC14
     */
    int PROJECT = 0x4;

    /**
     * Copies resource to given {@code destination} path. Copy operation performs asynchronously and result of current
     * operation will be provided in {@code Promise} result. Destination path should have write access.
     * <p/>
     * Copy operation produces new {@link Resource} which is already cached.
     * <p/>
     * Fires following events:
     * {@link ResourceChangedEvent} when resource has successfully copied. This event provides information about copied
     * resource and source resource.
     * <p/>
     * Example of usage:
     * <pre>
     *     Resource resource = ... ;
     *     Path copyTo = ... ;
     *
     *     resource.copy(copyTo).then(new Operation<Resource>() {
     *          public void apply(Resource copiedResource) throws OperationException {
     *              //do something with copiedResource
     *          }
     *     })
     * </pre>
     *
     * @param destination
     *         the destination path
     * @return {@link Promise} with copied {@link Resource}
     * @throws IllegalStateException
     *         if this resource could not be copied. Reasons include:
     *         <ul>
     *         <li>Resource already exists</li>
     *         <li>Resource with path '/path' isn't a project</li>
     *         </ul>
     * @throws IllegalArgumentException
     *         if current resource can not be copied. Reasons include:
     *         <ul>
     *         <li>Workspace root is not allowed to be copied</li>
     *         </ul>
     * @see ResourceChangedEvent
     * @see Resource
     * @since 4.0.0-RC14
     */
    Promise<Resource> copy(Path destination);

    /**
     * Copies resource to given {@code destination} path. Copy operation performs asynchronously and result of current
     * operation will be provided in {@code Promise} result. Destination path should have write access.
     * <p/>
     * Copy operation produces new {@link Resource} which is already cached.
     * <p/>
     * Fires following events:
     * {@link ResourceChangedEvent} when resource has successfully copied. This event provides information about copied
     * resource and source resource.
     * <p/>
     * Passing {@code force} argument as true method will ignore existed resource on the server and overwrite them.
     * <p/>
     * Example of usage:
     * <pre>
     *     Resource resource = ... ;
     *     Path copyTo = ... ;
     *
     *     resource.copy(copyTo, true).then(new Operation<Resource>() {
     *          public void apply(Resource copiedResource) throws OperationException {
     *              //do something with copiedResource
     *          }
     *     })
     * </pre>
     *
     * @param destination
     *         the destination path
     * @param force
     *         overwrite existed resource on the server
     * @return {@link Promise} with copied {@link Resource}
     * @throws IllegalStateException
     *         if this resource could not be copied. Reasons include:
     *         <ul>
     *         <li>Resource already exists</li>
     *         <li>Resource with path '/path' isn't a project</li>
     *         </ul>
     * @throws IllegalArgumentException
     *         if current resource can not be copied. Reasons include:
     *         <ul>
     *         <li>Workspace root is not allowed to be copied</li>
     *         </ul>
     * @see ResourceChangedEvent
     * @see Resource
     * @since 4.0.0-RC14
     */
    Promise<Resource> copy(Path destination, boolean force);

    /**
     * Moves resource to given new {@code destination}. Move operation performs asynchronously and result of current
     * operation will be displayed in {@code Promise} result.
     * <p/>
     * Move operation produces new {@link Resource} which is already cached.
     * <p/>
     * Fires following events:
     * {@link ResourceChangedEvent} when resource has successfully moved. This event provides information about moved
     * resource.
     * <p/>
     * Before moving mechanism remembers deepest depth which was read and tries to restore it after move.
     * <p/>
     * Example of usage:
     * <pre>
     *     Resource resource = ... ;
     *     Path moveTo = ... ;
     *
     *     resource.move(moveTo).then(new Operation<Resource>() {
     *          public void apply(Resource movedResource) throws OperationException {
     *              //do something with movedResource
     *          }
     *     })
     * </pre>
     *
     * @param destination
     *         the destination path
     * @return {@code Promise} with move moved {@link Resource}
     * @throws IllegalStateException
     *         if this resource could not be moved. Reasons include:
     *         <ul>
     *         <li>Resource already exists</li>
     *         <li>Resource with path '/path' isn't a project</li>
     *         </ul>
     * @throws IllegalArgumentException
     *         if current resource can not be moved. Reasons include:
     *         <ul>
     *         <li>Workspace root is not allowed to be moved</li>
     *         </ul>
     * @see ResourceChangedEvent
     * @see Resource
     * @since 4.0.0-RC14
     */
    Promise<Resource> move(Path destination);

    /**
     * Moves resource to given new {@code destination}. Move operation performs asynchronously and result of current
     * operation will be displayed in {@code Promise} result.
     * <p/>
     * Move operation produces new {@link Resource} which is already cached.
     * <p/>
     * Fires following events:
     * {@link ResourceChangedEvent} when resource has successfully moved. This event provides information about moved
     * resource.
     * <p/>
     * Before moving mechanism remembers deepest depth which was read and tries to restore it after move.
     * <p/>
     * Passing {@code force} argument as true method will ignore existed resource on the server and overwrite them.
     * <p/>
     * Example of usage:
     * <pre>
     *     Resource resource = ... ;
     *     Path moveTo = ... ;
     *
     *     resource.move(moveTo, true).then(new Operation<Resource>() {
     *          public void apply(Resource movedResource) throws OperationException {
     *              //do something with movedResource
     *          }
     *     })
     * </pre>
     *
     * @param destination
     *         the destination path
     * @return {@code Promise} with move moved {@link Resource}
     * @throws IllegalStateException
     *         if this resource could not be moved. Reasons include:
     *         <ul>
     *         <li>Resource already exists</li>
     *         <li>Resource with path '/path' isn't a project</li>
     *         </ul>
     * @throws IllegalArgumentException
     *         if current resource can not be moved. Reasons include:
     *         <ul>
     *         <li>Workspace root is not allowed to be moved</li>
     *         </ul>
     * @see ResourceChangedEvent
     * @see Resource
     * @since 4.0.0-RC14
     */
    Promise<Resource> move(Path destination, boolean force);

    /**
     * Deletes current resource.
     * Delete operation performs asynchronously and result of current operation will be displayed in {@code Promise}
     * result as {@code void}.
     * <p/>
     * Fires following events:
     * {@link ResourceChangedEvent} when resource has successfully removed.
     * <p/>
     * Example of usage:
     * <pre>
     *     Resource resource = ... ;
     *
     *     resource.delete().then(new Operation<Void>() {
     *         public void apply(Void ignored) throws OperationException {
     *             //do something
     *         }
     *     })
     * </pre>
     *
     * @return {@code Promise} with {@code void}
     * @throws IllegalArgumentException
     *         if current resource can not be removed. Reasons include:
     *         <ul>
     *         <li>Workspace root is not allowed to be removed</li>
     *         </ul>
     * @see ResourceChangedEvent
     * @since 4.0.0-RC14
     */
    Promise<Void> delete();

    /**
     * Returns the full, absolute path of this resource relative to the project's root.
     * e.g. {@code "/project_name/path/to/resource"}.
     *
     * @return the absolute path of this resource
     * @see Path
     * @since 4.0.0-RC14
     */
    Path getLocation();

    /**
     * Returns the name of the resource.
     * The name of a resource is synonymous with the last segment of its full (or project-relative) path.
     *
     * @return the name of the resource
     * @since 4.0.0-RC14
     */
    String getName();

    /**
     * Returns the resource which is the parent of this resource or {@link Optional#absent()} if such parent
     * doesn't exists. (This means that this resource is 'root' project).
     *
     * @return the resource parent {@link Container}
     * @see Container
     * @since 4.0.0-RC14
     */
    Optional<Container> getParent();

    /**
     * Returns the {@code Project} which contains this resource.
     * Returns itself for projects. A resource's project is the one named by the first segment of its full path.
     * <p/>
     * By design, each node should be bound to specified {@link Project}.
     *
     * @return the project
     * @throws IllegalStateException
     *         if related project wasn't found. Reason include:
     *         <ul>
     *         <li>Related project wasn't found</li>
     *         </ul>
     * @see Project
     * @since 4.0.0-RC14
     */
    Project getRelatedProject();

    /**
     * Returns the type of this resource.
     * Th returned value will be on of {@code FILE}, {@code FOLDER}, {@code PROJECT}.
     * <p/>
     * <ul>
     * <li>All resources of type {@code FILE} implement {@code File}.</li>
     * <li>All resources of type {@code FOLDER} implement {@code Folder}.</li>
     * <li>All resources of type {@code PROJECT} implement {@code Project}.</li>
     * </ul>
     *
     * @return the type of this resource
     * @see #FILE
     * @see #FOLDER
     * @see #PROJECT
     * @since 4.0.0-RC14
     */
    int getResourceType();

    /**
     * Returns the URL of this resource. The URL allows to download locally current resource.
     * <p/>
     * For container based resource the URL link will allow download container as zip archive.
     *
     * @return the URL of the resource
     * @throws IllegalArgumentException
     *         if URL is requested on workspace root. Reasons include:
     *         <ul>
     *         <li>Workspace root doesn't have export URL</li>
     *         </ul>
     * @since 4.0.0-RC14
     */
    String getURL();

    /**
     * Returns the marker handle with given {@code type} for the resource. The resource is not checked to see if
     * it has such a marker. The returned marker need not exist.
     *
     * @param type
     *         the known marker type
     * @return the {@link Optional} with specified registered marker
     * @see Marker#getType()
     * @see #getMarkers()
     * @since 4.0.0-RC14
     */
    Optional<Marker> getMarker(String type);

    /**
     * Returns all markers of the specified type on this resource. If there is no marker bound to the resource, then
     * empty array will be returned.
     *
     * @return the array of markers
     * @see #getMarker(String)
     * @since 4.0.0-RC14
     */
    Marker[] getMarkers();

    /**
     * Bound given {@code marker} to current resource. if such marker is already bound to the resource it will be overwritten.
     *
     * @param marker
     *         the resource marker
     * @since 4.0.0-RC14
     */
    void addMarker(Marker marker);

    /**
     * Delete specified marker with given {@code type}.
     *
     * @param type
     *         the marker type
     * @return true if specified marker removed
     * @see Marker#getType()
     * @since 4.0.0-RC14
     */
    boolean deleteMarker(String type);

    /** {@inheritDoc} */
    @Override
    boolean equals(Object other);

    /** {@inheritDoc} */
    @Override
    int hashCode();
}
