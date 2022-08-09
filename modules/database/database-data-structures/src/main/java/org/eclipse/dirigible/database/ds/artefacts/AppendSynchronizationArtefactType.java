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
package org.eclipse.dirigible.database.ds.artefacts;

import org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizationArtefactType;

/**
 * The Class AppendSynchronizationArtefactType.
 */
public class AppendSynchronizationArtefactType extends AbstractSynchronizationArtefactType {

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	@Override
	public String getId() {
		return "Data Append";
	}

	/**
	 * Gets the artefact state message.
	 *
	 * @param state the state
	 * @return the artefact state message
	 */
	@Override
	protected String getArtefactStateMessage(ArtefactState state) {
		switch (state) {
		case FAILED_CREATE:
			return "Create data append has failed";
		case FAILED_CREATE_UPDATE:
			return "Create or update data append has failed";
		case FAILED_UPDATE:
			return "Update data append has failed";
		case SUCCESSFUL_CREATE:
			return "Create data append was successful";
		case SUCCESSFUL_CREATE_UPDATE:
			return "Create or update data append was successful";
		case SUCCESSFUL_UPDATE:
			return "Update data append was successful";
		default:
			return state.getValue();
		}
	}

}
