package org.eclipse.dirigible.runtime.ide.publisher.processor;

import javax.inject.Inject;

import org.eclipse.dirigible.core.publisher.api.PublisherException;
import org.eclipse.dirigible.core.publisher.definition.PublishRequestDefinition;
import org.eclipse.dirigible.core.publisher.service.PublishCoreService;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processing the Registry Service incoming requests 
 *
 */
public class PublisherProcessor {
	
	private Logger logger = LoggerFactory.getLogger(PublisherProcessor.class);
	
	@Inject
	private PublishCoreService publishCoreService;
	
	@Inject
	private IRepository repository;
	
	
	public long requestPublishing(String user, String workspace, String path) throws PublisherException {
		StringBuilder workspacePath = generateWorkspacePath(user, workspace, null, null);
		PublishRequestDefinition publishRequestDefinition = publishCoreService.createPublishRequest(workspacePath.toString(), path, IRepositoryStructure.REGISTRY_PUBLIC);
		logger.info("Publishing request created [{}]", publishRequestDefinition.getId());
		return publishRequestDefinition.getId();
	}
	
	public PublishRequestDefinition getPublishingRequest(long id) throws PublisherException {
		PublishRequestDefinition publishRequestDefinition = publishCoreService.getPublishRequest(id);
		return publishRequestDefinition;
	}
	
	public boolean existsWorkspace(String user, String workspace) {
		StringBuilder workspacePath = generateWorkspacePath(user, workspace, null, null);
		ICollection collection = repository.getCollection(workspacePath.toString());
		return collection.exists();
	}

	private StringBuilder generateWorkspacePath(String user, String workspace, String project, String path) {
		StringBuilder relativePath = new StringBuilder(IRepositoryStructure.USERS)
				.append(IRepositoryStructure.SEPARATOR)
				.append(user)
				.append(IRepositoryStructure.SEPARATOR)
				.append(workspace);
		if (project != null) {
			relativePath.append(IRepositoryStructure.SEPARATOR)
				.append(project);
		}
		if (path != null) {
			relativePath.append(IRepositoryStructure.SEPARATOR)
				.append(path);
		}
		return relativePath;
	}

}
