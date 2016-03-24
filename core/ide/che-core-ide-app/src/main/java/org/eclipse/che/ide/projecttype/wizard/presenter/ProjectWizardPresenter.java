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
package org.eclipse.che.ide.projecttype.wizard.presenter;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDto;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistry;
import org.eclipse.che.ide.api.wizard.Wizard;
import org.eclipse.che.ide.api.wizard.WizardPage;
import org.eclipse.che.ide.projecttype.wizard.ProjectWizard;
import org.eclipse.che.ide.projecttype.wizard.ProjectWizardFactory;
import org.eclipse.che.ide.projecttype.wizard.categoriespage.CategoriesPagePresenter;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.CREATE;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.IMPORT;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.UPDATE;

/**
 * Presenter for project wizard.
 *
 * @author Evgen Vidolob
 * @author Oleksii Orel
 * @author Sergii Leschenko
 * @author Artem Zatsarynnyi
 * @author Vlad Zhukovskyi
 */
@Singleton
public class ProjectWizardPresenter implements Wizard.UpdateDelegate,
                                               ProjectWizardView.ActionDelegate,
                                               CategoriesPagePresenter.ProjectTypeSelectionListener,
                                               CategoriesPagePresenter.ProjectTemplateSelectionListener {

    private final ProjectWizardView                  view;
    private final ProjectWizardFactory               projectWizardFactory;
    private final ProjectWizardRegistry              wizardRegistry;
    private final Provider<CategoriesPagePresenter>  categoriesPageProvider;
    private final Map<ProjectTypeDto, ProjectWizard> wizardsCache;
    private       CategoriesPagePresenter            categoriesPage;
    private       ProjectWizard                      wizard;
    private       ProjectWizard                      importWizard;
    private       WizardPage                         currentPage;

    private ProjectWizardMode wizardMode;

    @Inject
    public ProjectWizardPresenter(ProjectWizardView view,
                                  ProjectWizardFactory projectWizardFactory,
                                  ProjectWizardRegistry wizardRegistry,
                                  Provider<CategoriesPagePresenter> categoriesPageProvider) {
        this.view = view;
        this.projectWizardFactory = projectWizardFactory;
        this.wizardRegistry = wizardRegistry;
        this.categoriesPageProvider = categoriesPageProvider;
        wizardsCache = new HashMap<>();
        view.setDelegate(this);
    }

    @Override
    public void onBackClicked() {
        final WizardPage prevPage = wizard.navigateToPrevious();
        if (prevPage != null) {
            showPage(prevPage);
        }
    }

    @Override
    public void onNextClicked() {
        final WizardPage nextPage = wizard.navigateToNext();
        if (nextPage != null) {
            showPage(nextPage);
        }
    }

    @Override
    public void onSaveClicked() {
        view.setLoaderVisibility(true);
        wizard.complete(new Wizard.CompleteCallback() {
            @Override
            public void onCompleted() {
                view.close();
            }

            @Override
            public void onFailure(Throwable e) {
                view.setLoaderVisibility(false);
            }
        });
    }

    @Override
    public void onCancelClicked() {
        view.close();
    }

    @Override
    public void updateControls() {
        view.setPreviousButtonEnabled(wizard.hasPrevious());
        view.setNextButtonEnabled(wizard.hasNext() && currentPage.isCompleted());
        view.setFinishButtonEnabled(wizard.canComplete());
    }

    /** Open the project wizard for creating a new project. */
    public void show() {
        resetState();
        wizardMode = CREATE;
        showDialog(null);
    }

    /** Open the project wizard with given mode. */
    public void show(@NotNull MutableProjectConfig project, ProjectWizardMode wizardMode) {
        resetState();
        this.wizardMode = wizardMode;
        showDialog(project);
    }

    /** Open the project wizard for updating the given {@code project}. */
    public void show(@NotNull MutableProjectConfig project) {
        resetState();
        wizardMode = UPDATE;
//        projectPath = project.getPath();
        showDialog(project);
    }

    /** Open the project wizard for creating module from the given {@code folder}. */
//    public void show(@NotNull ItemReference folder) {
//        resetState();
//        wizardMode = CREATE_MODULE;
//        projectPath = folder.getPath();
//        final ProjectConfigDto dataObject = dtoFactory.createDto(ProjectConfigDto.class)
//                                                      .withName(folder.getName());
//
//        showDialog(dataObject);
//    }

    private void resetState() {
        wizardsCache.clear();
        categoriesPage = categoriesPageProvider.get();
        wizardMode = null;
        categoriesPage.setProjectTypeSelectionListener(this);
        categoriesPage.setProjectTemplateSelectionListener(this);
//        projectPath = null;
        importWizard = null;
    }

    private void showDialog(@Nullable MutableProjectConfig dataObject) {
        wizard = createDefaultWizard(dataObject, wizardMode);
        final WizardPage<MutableProjectConfig> firstPage = wizard.navigateToFirst();
        if (firstPage != null) {
            showPage(firstPage);
            view.showDialog(wizardMode);
        }
    }

    @Override
    public void onProjectTypeSelected(ProjectTypeDto projectType) {
        final MutableProjectConfig prevData = wizard.getDataObject();
        wizard = getWizardForProjectType(projectType, prevData);
        wizard.navigateToFirst();
        final MutableProjectConfig newProject = wizard.getDataObject();

        // some values should be shared between wizards for different project types
        newProject.setPath(prevData.getPath());
        newProject.setName(prevData.getName());
        newProject.setDescription(prevData.getDescription());
        newProject.setMixins(prevData.getMixins());
        if (wizardMode == UPDATE) {
            newProject.setAttributes(prevData.getAttributes());
        }

        // set dataObject's values from projectType
        newProject.setType(projectType.getId());
//        newProject.setRecipe(projectType.getDefaultRecipe());
    }

    @Override
    public void onProjectTemplateSelected(ProjectTemplateDescriptor projectTemplate) {
        final MutableProjectConfig prevData = wizard.getDataObject();
        wizard = importWizard == null ? importWizard = createDefaultWizard(null, IMPORT) : importWizard;
        wizard.navigateToFirst();
        final MutableProjectConfig dataObject = wizard.getDataObject();

        // some values should be shared between wizards for different project types
        dataObject.setName(prevData.getName());
        dataObject.setDescription(prevData.getDescription());

        // set dataObject's values from projectTemplate
        dataObject.setType(projectTemplate.getProjectType());
        dataObject.setSource(projectTemplate.getSource());
    }

    /** Creates or returns project wizard for the specified projectType with the given dataObject. */
    private ProjectWizard getWizardForProjectType(@NotNull ProjectTypeDto projectType, @NotNull MutableProjectConfig configDto) {
        if (wizardsCache.containsKey(projectType)) {
            return wizardsCache.get(projectType);
        }

        final ProjectWizardRegistrar wizardRegistrar = wizardRegistry.getWizardRegistrar(projectType.getId());
        if (wizardRegistrar == null) {
            // should never occur
            throw new IllegalStateException("WizardRegistrar for the project type " + projectType.getId() + " isn't registered.");
        }

        List<Provider<? extends WizardPage<MutableProjectConfig>>> pageProviders = wizardRegistrar.getWizardPages();
        final ProjectWizard projectWizard = createDefaultWizard(configDto, wizardMode);
        for (Provider<? extends WizardPage<MutableProjectConfig>> provider : pageProviders) {
            projectWizard.addPage(provider.get(), 1, false);
        }

        wizardsCache.put(projectType, projectWizard);
        return projectWizard;
    }

    /** Creates and returns 'default' project wizard with pre-defined pages only. */
    private ProjectWizard createDefaultWizard(@Nullable MutableProjectConfig dataObject, @NotNull ProjectWizardMode mode) {
        if (dataObject == null) {
            dataObject = new MutableProjectConfig();
        }

        final ProjectWizard projectWizard = projectWizardFactory.newWizard(dataObject, mode/*, projectPath*/);
        projectWizard.setUpdateDelegate(this);

        // add pre-defined pages - first and last
        projectWizard.addPage(categoriesPage);
        return projectWizard;
    }

    private void showPage(@NotNull WizardPage wizardPage) {
        currentPage = wizardPage;
        updateControls();
        view.showPage(currentPage);
    }
}
