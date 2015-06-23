/**
 * Created by dhaeb on 12.05.15.
 */

'use strict';

var controlerName = 'sample01Ctrl';
var adfDashboardChangedEventName = 'adfDashboardChanged';
var widgetsPath = '';

angular.module('linked_data_browser', [
    'adf', 'adf.structures.base',
    'adf.widget.markdown', 'adf.widget.linklist',
    'adf.widget.version', 'adf.widget.clock',
    'LocalStorageModule', 'ngRoute',
    'ldbSearchDirective','lodb.widget.main'
]).config(function(dashboardProvider, $routeProvider, localStorageServiceProvider){
    dashboardProvider.widgetsPath(widgetsPath);
    localStorageServiceProvider.setPrefix('adf');

        $routeProvider.when('/', {
            templateUrl: 'assets/angular-templates/adf.html',
            controller: controlerName
        }).when("/:endpoint*\/subject/:subject*", {
            templateUrl: 'assets/angular-templates/adf.html',
            controller: controlerName
        }).when("/:subject*", {
            templateUrl: 'assets/angular-templates/adf.html',
            controller: controlerName
        })
        .otherwise({
            redirectTo: '/'
        });
}).service('query_parameter', function(DEFAULT_ENDPOINT, DEFAULT_SUBJECT){
    var endpoint = DEFAULT_ENDPOINT;
    var subject = DEFAULT_SUBJECT;

    this.getEndpoint = function(){return endpoint;};
    this.setEndpoint = function(newendpoint){endpoint = newendpoint;};

    this.getSubject = function(){return subject;};
    this.setSubject = function(newsubject){subject = newsubject;};


}).service('json_builder', function(query_parameter){

    var initWidgetsJson = {};
    var left_width = "col-md-6";
    var right_width = left_width;
    var structure = "8-4 (6-6/12)";

    this.getInitStruktur = function(title, widgetsContent){
        var widgets = {};
        widgets.title = title;
        widgets.structure = structure;
        widgets.rows = [];
        widgets.rows[0] = {};
        widgets.rows[0].columns = [];
        widgets.rows[0].columns[0] = {};
        widgets.rows[0].columns[0].styleClass=left_width;
        widgets.rows[0].columns[0].widgets=[];
        widgets.rows[0].columns[1] = {};
        widgets.rows[0].columns[1].styleClass=right_width;
        widgets.rows[0].columns[1].widgets=[];


        var counterLeft = 0;
        var counterRight = 0;
        var counter = 0;
        var column = 0;
        for(var widgetKey in widgetsContent){
            if(widgetsContent[widgetKey].orientation == "l"){
                counter = counterLeft;
                counterLeft = counterLeft+1;
                column = 0;
            }else {
                 counter = counterRight;
                 counterRight = counterRight+1;
                 column = 1;
            }

            if(widgetsContent[widgetKey].config == undefined){
                widgetsContent[widgetKey].config = {};
            }
            widgetsContent[widgetKey].config.uri=query_parameter.getSubject();
            widgetsContent[widgetKey].config.endpoint = query_parameter.getEndpoint();

            widgets.rows[0].columns[column].widgets[counter]= widgetsContent[widgetKey];
        }
        return widgets;
    }


}).service('widget_builder', function(query_parameter,json_builder){

    var t_scope = null;
    var widgetMainTitle = query_parameter.getSubject().substring(query_parameter.getSubject().lastIndexOf('/')+1);

    this.create = function(scope,widgetsContent){
        t_scope = scope;
        t_scope.editable = false;
        t_scope.name = name;
        t_scope.collapsible = true;
        t_scope.maximizable = false;

        t_scope.$watch("query_parameter.getSubject()", function(newValue, oldValue){
            if(newValue !== undefined){
                query_parameter.setSubject(newValue);
                widgetMainTitle = query_parameter.getSubject().substring(query_parameter.getSubject().lastIndexOf('/')+1);
                t_scope.model = json_builder.getInitStruktur(widgetMainTitle,widgetsContent);
            }
        });

        t_scope.$watch("query_parameter.getEndpoint()", function(newValue, oldValue){
            if(newValue !== undefined){
                query_parameter.setEndpoint(newValue);
                widgetMainTitle = query_parameter.getSubject().substring(query_parameter.getSubject().lastIndexOf('/')+1);
                t_scope.model = json_builder.getInitStruktur(widgetMainTitle,widgetsContent);
            }
        });


    }

}).controller(controlerName, function($scope,widget_builder, localStorageService, $routeParams, DEFAULT_ENDPOINT, DEFAULT_SUBJECT, query_parameter){
    $scope.$routeParams = $routeParams;
    $scope.query_parameter = query_parameter; // important to watch the value changes!
    query_parameter.setSubject("subject" in $routeParams ? $routeParams.subject : query_parameter.getSubject());
    query_parameter.setEndpoint("endpoint" in $routeParams ? $routeParams.endpoint : query_parameter.getEndpoint());

    var widgetsContent = [];

    var descriptionWidget = {};
    descriptionWidget.title = "Description";
    descriptionWidget.orientation = "l";
    descriptionWidget.type = "fox";
    descriptionWidget.config = {};
    descriptionWidget.config.url = '/nl_from_subject';
    descriptionWidget.config.transform = function(j){return j.nl;};
    widgetsContent.push(descriptionWidget);

    var descriptionWidget2 = {};
        descriptionWidget2.title = "Description2";
        descriptionWidget2.orientation = "l";
        descriptionWidget2.type = "fox";
        descriptionWidget2.config = {};
        descriptionWidget2.config.url = '/nl_from_subject';
        descriptionWidget2.config.transform = function(j){return j.nl;};
        widgetsContent.push(descriptionWidget2);

    var pictureWidget = {};
    pictureWidget.title = "Picture";
    pictureWidget.orientation = "r";
    pictureWidget.type = "picture";
    pictureWidget.config = {};
    pictureWidget.config.url = '/pictures_from_subject';
    pictureWidget.config.endpoint = query_parameter.getEndpoint();
    widgetsContent.push(pictureWidget);

    var descriptionWidget3 = {};
            descriptionWidget3.title = "Description3";
            descriptionWidget3.orientation = "r";
            descriptionWidget3.type = "fox";
            descriptionWidget3.config = {};
            descriptionWidget3.config.url = '/nl_from_subject';
            descriptionWidget3.config.transform = function(j){return j.nl;};
            widgetsContent.push(descriptionWidget3);

    widget_builder.create($scope,widgetsContent);

});
