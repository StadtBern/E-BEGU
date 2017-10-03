import '../../../bootstrap.ts';
import 'angular-mocks';
import {EbeguWebAdmin} from '../../admin.module';
import IScope = angular.IScope;

describe('adminView', function () {

    beforeEach(angular.mock.module(EbeguWebAdmin.name));

    let component: any;
    let scope: IScope;
    let $componentController: any;

    beforeEach(angular.mock.inject(function ($injector: any) {
        $componentController = $injector.get('$componentController');
        let $rootScope = $injector.get('$rootScope');
        scope = $rootScope.$new();
    }));

    it('should be defined', function () {
        /*
         To initialise your component controller you have to setup your (mock) bindings and
         pass them to $componentController.
         */
        let bindings = {};
        component = $componentController('dvTraegerschaftView', {$scope: scope}, bindings);
        expect(component).toBeDefined();
    });
});
