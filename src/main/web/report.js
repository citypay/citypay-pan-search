angular.module("CPPS", []).controller("CPPS", ['$scope', '$http',
    function ($scope, $http) {

        $scope.data = { loading: true };
        $scope.filename = 'example-report.json';

        $scope.load = function () {
            $http.get($scope.filename).then(function (data) {
                $scope.data = data.data;
            });
        };



    }]);