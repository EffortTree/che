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
package org.eclipse.che.ide.ext.java.client.action;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.JavaResources;
import org.eclipse.che.ide.ext.java.client.JavaUtils;
import org.eclipse.che.ide.newresource.AbstractNewResourceAction;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import org.eclipse.che.ide.ui.dialogs.input.InputDialog;
import org.eclipse.che.ide.ui.dialogs.input.InputValidator;

import javax.validation.constraints.NotNull;

import static com.google.common.base.Preconditions.checkState;

/**
 * Action to create new Java package.
 *
 * @author Artem Zatsarynnyi
 * @author Vlad Zhukovskyi
 */
@Singleton
public class NewPackageAction extends AbstractNewResourceAction {

    private final InputValidator nameValidator = new NameValidator();

    @Inject
    public NewPackageAction(JavaResources javaResources,
                            JavaLocalizationConstant localizationConstant,
                            AnalyticsEventLogger eventLogger,
                            DialogFactory dialogFactory,
                            CoreLocalizationConstant coreLocalizationConstant,
                            EventBus eventBus,
                            AppContext appContext) {
        super(localizationConstant.actionNewPackageTitle(),
              localizationConstant.actionNewPackageDescription(),
              javaResources.packageItem(), eventLogger, dialogFactory, coreLocalizationConstant, eventBus, appContext);
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent e) {
        e.getPresentation().setEnabledAndVisible(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        InputDialog inputDialog = dialogFactory.createInputDialog("New " + title, "Name:", new InputCallback() {
            @Override
            public void accepted(String value) {
                onAccepted(value);
            }
        }, null).withValidator(nameValidator);
        inputDialog.show();
    }

    private void onAccepted(String value) {

        final Resource resource = appContext.getResource();

        checkState(resource instanceof Container, "Parent should be a container");

        ((Container)resource).newFolder(value.replace('.', '/'));
    }

    private class NameValidator implements InputValidator {
        @Nullable
        @Override
        public Violation validate(String value) {
            try {
                JavaUtils.checkPackageName(value);
            } catch (final IllegalStateException e) {
                return new Violation() {
                    @Nullable
                    @Override
                    public String getMessage() {
                        String errorMessage = e.getMessage();
                        if (errorMessage == null || errorMessage.isEmpty()) {
                            return coreLocalizationConstant.invalidName();
                        }
                        return errorMessage;
                    }

                    @Nullable
                    @Override
                    public String getCorrectedValue() {
                        return null;
                    }
                };
            }
            return null;
        }
    }
}
