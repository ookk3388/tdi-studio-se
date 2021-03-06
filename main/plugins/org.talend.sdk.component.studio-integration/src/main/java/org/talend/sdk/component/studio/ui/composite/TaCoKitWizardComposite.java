/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.sdk.component.studio.ui.composite;

import java.util.Objects;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IElementParameter;
import org.talend.sdk.component.studio.metadata.model.TaCoKitConfigurationModel;
import org.talend.sdk.component.studio.metadata.model.TaCoKitConfigurationModel.ValueModel;
import org.talend.sdk.component.studio.model.parameter.TaCoKitElementParameter;
import org.talend.sdk.component.studio.model.parameter.TaCoKitElementParameter.IValueChangedListener;

/**
 * Stores {@link TaCoKitConfigurationModel} and sets Configuration Model Updater listener for each
 * {@link TaCoKitElementParameter}
 */
public class TaCoKitWizardComposite extends TaCoKitComposite {

    private final TaCoKitConfigurationModel configurationModel;

    private final IValueChangedListener configurationUpdater;

    private final boolean isNew;

    public TaCoKitWizardComposite(final Composite parentComposite, final int styles, final EComponentCategory section,
            final Element element, final TaCoKitConfigurationModel model, final boolean isCompactView,
            final Color backgroundColor, final boolean isNew) {
        super(parentComposite, styles, section, element, isCompactView, backgroundColor);
        this.configurationModel = model;
        this.isNew = isNew;
        configurationUpdater = new ConfigurationModelUpdater();
        init();
    }

    @Override
    protected void postInit() {
        elem.getElementParameters().stream()
                .filter(Objects::nonNull)
                .filter(TaCoKitElementParameter.class::isInstance)
                .map(TaCoKitElementParameter.class::cast)
                .filter(TaCoKitElementParameter::isRedrawable)
                .forEach(p -> p.registerListener("show", getRedrawListener()));
    }

    @Override
    protected void preDispose() {
        elem.getElementParameters().stream()
                .filter(Objects::nonNull)
                .filter(TaCoKitElementParameter.class::isInstance)
                .map(TaCoKitElementParameter.class::cast)
                .filter(TaCoKitElementParameter::isRedrawable)
                .forEach(p -> p.unregisterListener("show", getRedrawListener()));
    }

    private void init() {
        elem
                .getElementParameters()
                .stream()
                .filter(p -> p instanceof TaCoKitElementParameter)
                .map(p -> (TaCoKitElementParameter) p)
                .filter(TaCoKitElementParameter::isPersisted)
                .filter(p -> !EParameterFieldType.SCHEMA_TYPE.equals(p.getFieldType()))
                .forEach(parameter -> {
                    parameter.addValueChangeListener(configurationUpdater);
                    if (isNew) {
                        parameter.setValue(parameter.getValue());
                        return;
                    }
                    try {
                        ValueModel valueModel = configurationModel.getValue(parameter.getName());
                        if (valueModel != null) {
                            if (valueModel.getConfigurationModel() != configurationModel) {
                                parameter.setReadOnly(true);
                            }
                            parameter.setValue(valueModel.getValue());
                        }
                    } catch (Exception e) {
                        ExceptionHandler.process(e);
                    }
                });
    }

    /**
     * Removes {@link ConfigurationModelUpdater} from every {@link TaCoKitElementParameter}, where it was registered
     */
    @Override
    public synchronized void dispose() {
        elem
                .getElementParameters()
                .stream()
                .filter(p -> p instanceof TaCoKitElementParameter)
                .map(p -> (TaCoKitElementParameter) p)
                .forEach(p -> p.removeValueChangeListener(configurationUpdater));
        super.dispose();
    }

    /**
     * Overrides parent method as Property Type widget should not be shown in wizard pages
     *
     * @return last Composite added
     */
    @Override
    protected Composite addCommonWidgets() {
        return addSchemas(composite, null);
    }

    /**
     * Overrides implementation from parent class.
     * This is a quickfix to skip schema in repository widget
     *
     * @param parent    Composite on which widget will be added
     * @param parameter ElementParameter(Model) associated with widget
     */
    @Override
    protected void addWidgetIfActive(final Composite parent, final IElementParameter parameter) {
        if (doShow(parameter) && !EParameterFieldType.SCHEMA_TYPE.equals(parameter.getFieldType())) {
            addWidget(parent, parameter, null);
        }
    }

    private class ConfigurationModelUpdater implements IValueChangedListener {

        /**
         * Updates {@link TaCoKitConfigurationModel} each time some of {@link TaCoKitElementParameter} is changed
         *
         * @param elementParameter changed {@link TaCoKitElementParameter}
         * @param oldValue         parameter old value
         * @param newValue         parameter new value
         */
        @Override
        public void onValueChanged(final TaCoKitElementParameter elementParameter, final Object oldValue,
                final Object newValue) {
            configurationModel.setValue(elementParameter);
        }

    }

}
