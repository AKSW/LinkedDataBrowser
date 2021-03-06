angular.module('lodb.widget.main.fox', [])
    .controller('foxCtrl', function ($scope, $http, responseData, config, widget) {
        if(responseData == ""){
            config.removeWidget(widget);
        }
        $scope.content = responseData;
        $http.post("fox_proxy", // in the moment, we use a proxy to overcome the same origin policy - maybe our app will be deployed side by side with fox so that we can change that
            {
                "input":  encodeURIComponent($scope.content),
                "type": "text",
                "task": "ner",
                "output": "JSON-LD",
                "returnHtml": true
            }).success(function (data) {
                if (data) {
                    $scope.content = decodeURIComponent(data.input);
                }
            }).error(function(){
                console.log("Fox cannot be loaded!");
            });
    }).filter("sanitize", ['$sce', function($sce) {
        return function(htmlCode){
            return $sce.trustAsHtml(htmlCode);
        }
    }]).filter("ldbnize_link", ['$sce', 'DEFAULT_ENDPOINT', function($sce) { // change link to a dbpedia resource into a link to the ldb
        return function(htmlTags){

            function startsWith(s) {
                var regex = /^(\/#\/)/i;
                return regex.test(s);
            };

            if(htmlTags !== undefined){
                htmlTags = "<div>" + htmlTags + "</div>"
                var selector = "a";
                htmlTags = $(htmlTags);
                htmlTags.find(selector).attr("href", function(i, oldHref) {
                    if(oldHref === undefined || startsWith(oldHref)){
                        return oldHref;
                    } else {
                        return "/#/" + oldHref;
                    }
                }).attr("target", function(i, target) {
                        var parent = "_self";
                        if(target !== undefined || target != parent){
                            return parent;
                        }
                        return target;
                });
                return htmlTags.html();
            } else {
                return htmlTags;
            }
        }
    }]);