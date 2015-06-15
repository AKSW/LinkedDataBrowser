angular.module('lodb.widget.main.picture', [])
    .controller('pictureCtrl', function ($scope, responseData, $http, config) {
        $scope.myInterval = 10000;
        var pictures = angular.fromJson(responseData);
        var slides = $scope.slides = [];
        for (var picture in pictures) {
            slides.push({image: pictures[picture], text: pictures[picture]});
        }
        var googleApiUrl = 'http://ajax.googleapis.com/ajax/services/search/images';
        var query = config.uri.substring(config.uri.lastIndexOf("/") + 1, config.uri.length);
        $http.jsonp(googleApiUrl, {
            params: {
                'v': '1.0',
                'q': query,
                'callback': 'JSON_CALLBACK'
            }, headers: {
                'Access-Control-Allow-Origin': "http://ajax.googleapis.com",
            }
        })
            .success(function (data) {
                if (data) {
                    var picList= data.responseData.results;
                    for (var pic in picList) {
                        slides.push({image: picList[pic].url, text: picList[pic].url});
                    }
                }
            });
    });