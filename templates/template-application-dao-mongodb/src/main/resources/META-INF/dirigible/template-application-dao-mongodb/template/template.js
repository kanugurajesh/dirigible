/*
 * Generated by Eclipse Dirigible based on model and template.
 *
 * Do not modify the content as it may be re-generated again.
 */
const generateUtils = require("ide-generate-service/template/generateUtils");
const parameterUtils = require("ide-generate-service/template/parameterUtils");

exports.generate = function (model, parameters) {
    model = JSON.parse(model).model;
    let templateSources = exports.getTemplate(parameters).sources;
    parameterUtils.process(model, parameters)
    return generateUtils.generateFiles(model, parameters, templateSources);
};

exports.getTemplate = function (parameters) {
    return {
        name: "Application - DAO - MongoDB",
        description: "Application with DAO for MongoDB",
        extension: "model",
        sources: [{
            location: "/template-application-dao-mongodb/dao/entity.js.template",
            action: "generate",
            rename: "gen/dao/{{perspectiveName}}/{{name}}.js",
            engine: "velocity",
            collection: "models"
        }, {
            location: "/template-application-dao-mongodb/dao/utils/EntityUtils.js.template",
            action: "copy",
            rename: "gen/dao/utils/EntityUtils.js"
        }],
        parameters: []
    };
};