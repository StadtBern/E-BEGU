import {EbeguWebGesuch} from '../../gesuch.module';
import {FreigabeViewController} from './freigabeView';
import TSGesuch from '../../../models/TSGesuch';
import WizardStepManager from '../../service/wizardStepManager';
import {DvDialog} from '../../../core/directive/dv-dialog/dv-dialog';
import {DownloadRS} from '../../../core/service/downloadRS.rest';
import TSDownloadFile from '../../../models/TSDownloadFile';
import GesuchModelManager from '../../service/gesuchModelManager';
import TestDataUtil from '../../../utils/TestDataUtil';
import {TSWizardStepName} from '../../../models/enums/TSWizardStepName';
import {TSWizardStepStatus} from '../../../models/enums/TSWizardStepStatus';
import {TSZustelladresse} from '../../../models/enums/TSZustelladresse';
import {ApplicationPropertyRS} from '../../../admin/service/applicationPropertyRS.rest';
import {EbeguWebAdmin} from '../../../admin/admin.module';
import AuthServiceRS from '../../../authentication/service/AuthServiceRS.rest';
import {ITimeoutService} from 'angular';
import IScope = angular.IScope;
import IPromise = angular.IPromise;
import IQService = angular.IQService;
import IHttpBackendService = angular.IHttpBackendService;
import IFormController = angular.IFormController;

describe('freigabeView', function () {

    let controller: FreigabeViewController;
    let $scope: IScope;
    let wizardStepManager: WizardStepManager;
    let dialog: DvDialog;
    let downloadRS: DownloadRS;
    let $q: IQService;
    let gesuchModelManager: GesuchModelManager;
    let $httpBackend: IHttpBackendService;
    let applicationPropertyRS: any;
    let authServiceRS: AuthServiceRS;
    let $timeout: ITimeoutService;

    let gesuch: TSGesuch;


    beforeEach(angular.mock.module(EbeguWebGesuch.name));

    beforeEach(angular.mock.module(EbeguWebAdmin.name));  //to inject applicationPropertyRS

    beforeEach(angular.mock.inject(function ($injector: any) {

    }));


    beforeEach(angular.mock.inject(function ($injector: any) {
        $scope = $injector.get('$rootScope');
        wizardStepManager = $injector.get('WizardStepManager');
        dialog = $injector.get('DvDialog');
        downloadRS = $injector.get('DownloadRS');
        $q = $injector.get('$q');
        gesuchModelManager = $injector.get('GesuchModelManager');
        $httpBackend = $injector.get('$httpBackend');
        applicationPropertyRS = $injector.get('ApplicationPropertyRS');
        authServiceRS = $injector.get('AuthServiceRS');
        $timeout = $injector.get('$timeout');

        spyOn(applicationPropertyRS , 'isDevMode').and.returnValue($q.when(false));
        spyOn(authServiceRS , 'isOneOfRoles').and.returnValue(true);
        spyOn(wizardStepManager, 'updateCurrentWizardStepStatus').and.returnValue({});

        controller = new FreigabeViewController(gesuchModelManager, $injector.get('BerechnungsManager'),
            wizardStepManager, dialog, downloadRS, $scope, applicationPropertyRS, authServiceRS, $timeout);
        controller.form = <IFormController>{};

        spyOn(controller, 'isGesuchValid').and.callFake(function () {
            return controller.form.$valid;
        });

        let form = TestDataUtil.createDummyForm();
        // $rootScope.form = form;
        controller.form = form;
    }));
    describe('canBeFreigegeben', function () {
        it('should return false when not all steps are true', function () {
            spyOn(wizardStepManager, 'areAllStepsOK').and.returnValue(false);
            spyOn(wizardStepManager, 'hasStepGivenStatus').and.returnValue(true);
            expect(controller.canBeFreigegeben()).toBe(false);
        });
        it('should return false when all steps are true but not all Betreuungen are accepted', function () {
            spyOn(wizardStepManager, 'areAllStepsOK').and.returnValue(true);
            spyOn(wizardStepManager, 'hasStepGivenStatus').and.returnValue(false);

            expect(controller.canBeFreigegeben()).toBe(false);

            expect(wizardStepManager.hasStepGivenStatus).toHaveBeenCalledWith(TSWizardStepName.BETREUUNG, TSWizardStepStatus.OK);
        });
        it('should return false when all steps are true and all Betreuungen are accepted and the Gesuch is ReadOnly', function () {
            spyOn(wizardStepManager, 'areAllStepsOK').and.returnValue(true);
            spyOn(wizardStepManager, 'hasStepGivenStatus').and.returnValue(true);
            spyOn(gesuchModelManager, 'isGesuchReadonly').and.returnValue(true);
            expect(controller.canBeFreigegeben()).toBe(false);
        });
        it('should return true when all steps are true and all Betreuungen are accepted and the Gesuch is not ReadOnly', function () {
            spyOn(wizardStepManager, 'areAllStepsOK').and.returnValue(true);
            spyOn(wizardStepManager, 'hasStepGivenStatus').and.returnValue(true);
            spyOn(gesuchModelManager, 'isGesuchReadonly').and.returnValue(false);
            spyOn(controller, 'isGesuchInStatus').and.returnValue(true);
            expect(controller.canBeFreigegeben()).toBe(true);
        });
    });
    describe('gesuchFreigeben', function () {
        it('should return undefined when the form is not valid', function () {
            controller.form.$valid = false;

            let returned: IPromise<void> = controller.gesuchEinreichen();

            expect(returned).toBeUndefined();
        });
        it('should return undefined when the form is not valid', function () {
            controller.form.$valid = true;
            controller.bestaetigungFreigabequittung = false;

            let returned: IPromise<void> = controller.gesuchEinreichen();

            expect(returned).toBeUndefined();
        });
        it('should call showDialog when form is valid', function () {
            TestDataUtil.mockDefaultGesuchModelManagerHttpCalls($httpBackend);
            controller.bestaetigungFreigabequittung = true;

            controller.form.$valid = true;
            spyOn(dialog, 'showDialog').and.returnValue($q.when({}));

            let returned: IPromise<void> = controller.gesuchEinreichen();
            $scope.$apply();

            expect(dialog.showDialog).toHaveBeenCalled();
            expect(returned).toBeDefined();
        });
    });
    describe('confirmationCallback', function () {
        it('should return a Promise when the form is valid', function () {
            TestDataUtil.mockDefaultGesuchModelManagerHttpCalls($httpBackend);
            controller.bestaetigungFreigabequittung = true;

            controller.form.$valid = true;

            spyOn(dialog, 'showDialog').and.returnValue($q.when({}));
            let downloadFile: TSDownloadFile = new TSDownloadFile();
            downloadFile.accessToken = 'token';
            downloadFile.filename = 'name';
            spyOn(downloadRS, 'getFreigabequittungAccessTokenGeneratedDokument').and.returnValue($q.when(downloadFile));
            spyOn(downloadRS, 'startDownload').and.returnValue($q.when({}));
            spyOn(gesuchModelManager, 'openGesuch').and.returnValue($q.when({}));
            let gesuch: TSGesuch = new TSGesuch();
            gesuch.id = '123';
            spyOn(gesuchModelManager, 'getGesuch').and.returnValue(gesuch);

            controller.confirmationCallback();
            $scope.$apply();

            expect(downloadRS.getFreigabequittungAccessTokenGeneratedDokument).toHaveBeenCalledWith(gesuch.id, true, TSZustelladresse.JUGENDAMT);
            expect(downloadRS.startDownload).toHaveBeenCalledWith(downloadFile.accessToken, downloadFile.filename, false, jasmine.any(Object));
        });
    });
    describe('openFreigabequittungPDF', function () {
        beforeEach(() => {
            TestDataUtil.mockDefaultGesuchModelManagerHttpCalls($httpBackend);
            spyOn(gesuchModelManager, 'openGesuch').and.returnValue($q.when({}));
            spyOn(downloadRS, 'startDownload').and.returnValue($q.when({}));
            spyOn(downloadRS, 'getFreigabequittungAccessTokenGeneratedDokument').and.returnValue($q.when({}));
            gesuch = new TSGesuch();
            gesuch.id = '123';
            spyOn(gesuchModelManager, 'getGesuch').and.returnValue(gesuch);
        });
        it('should call the service with TSZustelladresse.JUGENDAMT for Erstgesuch', function () {
            spyOn(gesuchModelManager, 'isGesuch').and.returnValue(true);
            spyOn(gesuchModelManager, 'areThereOnlySchulamtAngebote').and.returnValue(false);

            controller.openFreigabequittungPDF(false);
            $scope.$apply();

            expect(downloadRS.getFreigabequittungAccessTokenGeneratedDokument).toHaveBeenCalledWith(gesuch.id, false, TSZustelladresse.JUGENDAMT);
        });
        it('should call the service with TSZustelladresse.SCHULAMT for Erstgesuch', function () {
            spyOn(gesuchModelManager, 'isGesuch').and.returnValue(true);
            spyOn(gesuchModelManager, 'areThereOnlySchulamtAngebote').and.returnValue(true);

            controller.openFreigabequittungPDF(false);
            $scope.$apply();

            expect(downloadRS.getFreigabequittungAccessTokenGeneratedDokument).toHaveBeenCalledWith(gesuch.id, false, TSZustelladresse.SCHULAMT);
        });
        it('should call the service with TSZustelladresse.JUGENDAMT for Mutation of Erstgesuch with SA-Freigabequittung', function () {
            spyOn(gesuchModelManager, 'isGesuch').and.returnValue(false);
            spyOn(gesuchModelManager, 'areAllJAAngeboteNew').and.returnValue(true);

            controller.openFreigabequittungPDF(false);
            $scope.$apply();

            expect(downloadRS.getFreigabequittungAccessTokenGeneratedDokument).toHaveBeenCalledWith(gesuch.id, false, TSZustelladresse.JUGENDAMT);
        });
        it('should call the service with undefined for Mutation of Erstgesuch with JS-Freigabequittung', function () {
            spyOn(gesuchModelManager, 'isGesuch').and.returnValue(false);
            spyOn(gesuchModelManager, 'areAllJAAngeboteNew').and.returnValue(false);

            controller.openFreigabequittungPDF(false);
            $scope.$apply();

            expect(downloadRS.getFreigabequittungAccessTokenGeneratedDokument).toHaveBeenCalledWith(gesuch.id, false, undefined);
        });
    });
});
