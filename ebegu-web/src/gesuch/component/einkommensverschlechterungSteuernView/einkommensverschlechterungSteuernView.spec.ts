import '../../../bootstrap.ts';
import 'angular-mocks';
import {EbeguWebGesuch} from '../../gesuch.module';
import GesuchModelManager from '../../service/gesuchModelManager';
import TSGesuchsteller from '../../../models/TSGesuchsteller';
import TSGesuchstellerContainer from '../../../models/TSGesuchstellerContainer';
import {TSEingangsart} from '../../../models/enums/TSEingangsart';
import TSFamiliensituationContainer from '../../../models/TSFamiliensituationContainer';

describe('einkommensverschlechterungSteuernView', function () {

    let gesuchModelManager: GesuchModelManager;

    beforeEach(angular.mock.module(EbeguWebGesuch.name));

    let component: any;
    let scope: angular.IScope;
    let $componentController: any;

    beforeEach(angular.mock.inject(function ($injector: any) {
        $componentController = $injector.get('$componentController');
        gesuchModelManager = $injector.get('GesuchModelManager');
        let $rootScope = $injector.get('$rootScope');
        scope = $rootScope.$new();
    }));

    beforeEach(function () {
        gesuchModelManager.initGesuch(false, TSEingangsart.PAPIER);
        gesuchModelManager.getGesuch().familiensituationContainer = new TSFamiliensituationContainer();
        gesuchModelManager.getGesuch().gesuchsteller1 = new TSGesuchstellerContainer(new TSGesuchsteller());
    });

    it('should be defined', function () {
        /*
         To initialise your component controller you have to setup your (mock) bindings and
         pass them to $componentController.
         */
        let bindings: {};
        component = $componentController('einkommensverschlechterungSteuernView', {$scope: scope}, bindings);
        expect(component).toBeDefined();
    });
});
