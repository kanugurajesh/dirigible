/**
 * Copyright (c) 2010-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   SAP - initial API and implementation
 */
package org.eclipse.dirigible.runtime.git.model;

import io.swagger.annotations.ApiModelProperty;

/**
 * The Git Checkout Model.
 */
public class GitCheckoutModel extends BaseGitModel {

	@ApiModelProperty(hidden = true)
	private String project;
	
	@ApiModelProperty(value = "Whether to publish the project(s) after checkout", example = "true")
	private boolean publish;

	/**
	 * Gets the project.
	 *
	 * @return the project
	 */
	public String getProject() {
		return project;
	}

	/**
	 * Sets the project.
	 *
	 * @param project the new project
	 */
	public void setProject(String project) {
		this.project = project;
	}
	
	/**
	 * Checks if is publish.
	 *
	 * @return true, if is publish
	 */
	public boolean isPublish() {
		return publish;
	}

	/**
	 * Sets the publish.
	 *
	 * @param publish the new publish
	 */
	public void setPublish(boolean publish) {
		this.publish = publish;
	}

}
