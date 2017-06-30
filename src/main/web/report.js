angular.module("CPPS", []).controller("CPPS", ['$scope', '$http',
    function ($scope, $http) {

        $scope.data = { loading: true };

        $http.get('example-report.json').then(function (data) {
            $scope.data = data.data;
        });

    }]);