import { response, rs } from "@dirigible/http";
import { workspace, lifecycle } from "@dirigible/platform";
import { user } from "@dirigible/security";

class HttpError extends Error {

    constructor(message, status) {
        super(message);
        this.status = status;
    }
}

//class ForbiddenError extends HttpError {
//
//    constructor(message) {
//        super(message, response.FORBIDDEN);
//    }
//}

class BadRequestError extends HttpError {

    constructor(message) {
        super(message, response.BAD_REQUEST);
    }
}


rs.service()
    .resource("model/{workspace}/{project}")
    .post(onGenerateModel)
    .before((context) => {
        //if (!user.isInRole("Developer")) {
        //    throw new ForbiddenError("Forbidden");
        //} else 
        if (!context.queryParameters.path) {
            throw new BadRequestError("Missing 'path' query parameter");
        }
    })
    .catch(onErrorOccurred)
    .execute();

function onErrorOccurred(ctx, err, request, response) {
    let status = err.status || response.INTERNAL_SERVER_ERROR;
    let message = err.message || "Internal Server Error";
    response.setStatus(status);
    response.println(JSON.stringify({
        status: status,
        message: message
    }));
}

function onGenerateModel(context, request, response) {
    let workspace = context.pathParameters.workspace;
    let project = context.pathParameters.project;
    let path = context.queryParameters.path;

    let model = getModel(workspace, project, path);

    let templatePayload = request.getJSON();

    let template = require(templatePayload.template);
    let parameters = templatePayload.parameters;
    parameters.projectName = project;
    parameters.workspaceName = workspace;
    parameters.filePath = path;
    parameters.templateId = templatePayload.template;

    let generatedFiles = template.generate(model, parameters);

    cleanGenFolder(workspace, project);

    for (let i = 0; i < generatedFiles.length; i++) {
        createFile(workspace, project, generatedFiles[i].path, generatedFiles[i].content);
    }
    let gen = path;
    if (gen.indexOf(".") > 0) {
        gen = gen.substring(0, path.indexOf("."))
    }

    createFile(workspace, project, gen + ".gen", JSON.stringify(parameters, null, 2));

    lifecycle.publish(workspace, project);

    response.setStatus(response.CREATED);

    response.flush();
    response.close();
}

function getModel(workspaceName, projectName, path) {
    let projectWorkspace = workspace.getWorkspace(workspaceName);
    if (!projectWorkspace.exists()) {
        throw new BadRequestError(`Workspace '${workspaceName}' does not exist.`);
    }
    let project = projectWorkspace.getProject(projectName);
    if (!project.exists()) {
        throw new BadRequestError(`Project '${projectName}' does not exist in Workspace '${workspaceName}'.`);
    }
    let model = project.getFile(path);
    if (!model.exists()) {
        throw new BadRequestError(`Model file '${path}' does not exist in Project '${projectName}' in Workspace '${workspaceName}'.`);
    }
    return model.getText();
}

function cleanGenFolder(workspaceName, projectName) {
    let projectWorkspace = workspace.getWorkspace(workspaceName);
    let project = projectWorkspace.getProject(projectName);

    if (project.existsFolder("gen")) {
        project.deleteFolder("gen");
    }
    lifecycle.unpublish(projectName);
}

function createFile(workspaceName, projectName, path, content) {
    let projectWorkspace = workspace.getWorkspace(workspaceName);
    let project = projectWorkspace.getProject(projectName);

    let pathSegments = path.split("/");
    let fileName = pathSegments[pathSegments.length - 1];
    let folder = null;
    for (let i = 0; i < pathSegments.length - 1; i++) {
        if (folder == null) {
            if (!project.existsFolder(pathSegments[i])) {
                folder = project.createFolder(pathSegments[i])
            }
            folder = project.getFolder(pathSegments[i])
            continue;
        }
        if (!folder.existsFolder(pathSegments[i])) {
            folder = folder.createFolder(pathSegments[i]);
        } else {
            folder = folder.getFolder(pathSegments[i]);
        }
    }
    let file = null;
    if (folder == null) {
        file = project.createFile(fileName);
    } else {
        file = folder.createFile(fileName);
    }
    file.setText(content);
}