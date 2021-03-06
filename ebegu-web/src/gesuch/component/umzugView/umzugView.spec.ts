/*
 * Ki-Tax: System for the management of external childcare subsidies
 * Copyright (C) 2017 City of Bern Switzerland
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import {EbeguWebCore} from '../../../core/core.module';
import {DvDialog} from '../../../core/directive/dv-dialog/dv-dialog';
import ErrorService from '../../../core/errors/service/ErrorService';
import {TSAdressetyp} from '../../../models/enums/TSAdressetyp';
import {TSBetroffene} from '../../../models/enums/TSBetroffene';
import TSAdresseContainer from '../../../models/TSAdresseContainer';
import TSGesuch from '../../../models/TSGesuch';
import TestDataUtil from '../../../utils/TestDataUtil';
import BerechnungsManager from '../../service/berechnungsManager';
import GesuchModelManager from '../../service/gesuchModelManager';
import WizardStepManager from '../../service/wizardStepManager';
import {UmzugViewController} from './umzugView';

describe('umzugView', function () {

    let umzugController: UmzugViewController;
    let gesuchModelManager: GesuchModelManager;
    let wizardStepManager: WizardStepManager;
    let berechnungsManager: BerechnungsManager;
    let errorService: ErrorService;
    let $translate: angular.translate.ITranslateService;
    let dialog: DvDialog;
    let $q: angular.IQService;
    let $rootScope: angular.IRootScopeService;
    let $httpBackend: angular.IHttpBackendService;
    let $timeout: angular.ITimeoutService;

    beforeEach(angular.mock.module(EbeguWebCore.name));

    beforeEach(angular.mock.inject(function ($injector: angular.auto.IInjectorService) {
        gesuchModelManager = $injector.get('GesuchModelManager');
        wizardStepManager = $injector.get('WizardStepManager');
        spyOn(wizardStepManager, 'updateWizardStepStatus').and.returnValue({});
        berechnungsManager = $injector.get('BerechnungsManager');
        errorService = $injector.get('ErrorService');
        $translate = $injector.get('$translate');
        dialog = $injector.get('DvDialog');
        $q = $injector.get('$q');
        $rootScope = $injector.get('$rootScope');
        $httpBackend = $injector.get('$httpBackend');
        $timeout = $injector.get('$timeout');
    }));

    describe('getNameFromBetroffene', function () {
        beforeEach(function () {
            umzugController = new UmzugViewController(gesuchModelManager, berechnungsManager,
                wizardStepManager, errorService, $translate, dialog, $q, $rootScope, $timeout);
        });
        it('should return the names of the GS or beide Gesuchsteller', function () {
            let gesuch: TSGesuch = new TSGesuch();
            gesuch.gesuchsteller1 = TestDataUtil.createGesuchsteller('Rodolfo', 'Langostino');
            gesuch.gesuchsteller2 = TestDataUtil.createGesuchsteller('Ana', 'Karenina');
            spyOn(gesuchModelManager, 'getGesuch').and.returnValue(gesuch);

            expect(umzugController.getNameFromBetroffene(TSBetroffene.GESUCHSTELLER_1)).toEqual(gesuch.gesuchsteller1.extractFullName());
            expect(umzugController.getNameFromBetroffene(TSBetroffene.GESUCHSTELLER_2)).toEqual(gesuch.gesuchsteller2.extractFullName());
            expect(umzugController.getNameFromBetroffene(TSBetroffene.BEIDE_GESUCHSTELLER)).toEqual('beide Gesuchstellenden');
        });
        it('should return empty string for empty data', function () {
            let gesuch: TSGesuch = new TSGesuch();
            spyOn(gesuchModelManager, 'getGesuch').and.returnValue(gesuch);

            expect(umzugController.getNameFromBetroffene(TSBetroffene.GESUCHSTELLER_1)).toEqual('');
            expect(umzugController.getNameFromBetroffene(TSBetroffene.GESUCHSTELLER_2)).toEqual('');
            expect(umzugController.getNameFromBetroffene(TSBetroffene.BEIDE_GESUCHSTELLER)).toEqual('beide Gesuchstellenden');
        });
    });

    describe('getBetroffenenList', function () {
        beforeEach(function () {
            umzugController = new UmzugViewController(gesuchModelManager, berechnungsManager,
                wizardStepManager, errorService, $translate, dialog, $q, $rootScope, $timeout);
        });
        it('should return a list with only GS1', function () {
            let gesuch: TSGesuch = new TSGesuch();
            gesuch.gesuchsteller1 = TestDataUtil.createGesuchsteller('Rodolfo', 'Langostino');
            spyOn(gesuchModelManager, 'getGesuch').and.returnValue(gesuch);

            let betroffenenList: Array<TSBetroffene> = umzugController.getBetroffenenList();
            expect(betroffenenList.length).toBe(1);
            expect(betroffenenList[0]).toBe(TSBetroffene.GESUCHSTELLER_1);
        });
        it('should return a list with GS1, GS2 und BEIDE', function () {
            let gesuch: TSGesuch = new TSGesuch();
            gesuch.gesuchsteller1 = TestDataUtil.createGesuchsteller('Rodolfo', 'Langostino');
            gesuch.gesuchsteller2 = TestDataUtil.createGesuchsteller('Ana', 'Karenina');
            spyOn(gesuchModelManager, 'getGesuch').and.returnValue(gesuch);

            let betroffenenList: Array<TSBetroffene> = umzugController.getBetroffenenList();
            expect(betroffenenList.length).toBe(3);
            expect(betroffenenList[0]).toBe(TSBetroffene.GESUCHSTELLER_1);
            expect(betroffenenList[1]).toBe(TSBetroffene.GESUCHSTELLER_2);
            expect(betroffenenList[2]).toBe(TSBetroffene.BEIDE_GESUCHSTELLER);
        });
    });

    describe('getAdressenListFromGS', function () {
        it('should have an empty AdressenList for gesuch=null', function () {
            spyOn(gesuchModelManager, 'getGesuch').and.returnValue(undefined);

            umzugController = new UmzugViewController(gesuchModelManager, berechnungsManager,
                wizardStepManager, errorService, $translate, dialog, $q, $rootScope, $timeout);

            expect(umzugController.getUmzugAdressenList().length).toBe(0);
        });
        it('should have all adressen for GS1 and GS2', function () {
            let gesuch: TSGesuch = new TSGesuch();
            gesuch.gesuchsteller1 = TestDataUtil.createGesuchsteller('Rodolfo', 'Langostino');
            gesuch.gesuchsteller1.addAdresse(TestDataUtil.createAdresse('strasse1', '10'));
            let umzugAdresseGS1: TSAdresseContainer = TestDataUtil.createAdresse('umzugstrasse1', '10');
            umzugAdresseGS1.showDatumVon = true;
            gesuch.gesuchsteller1.addAdresse(umzugAdresseGS1);

            gesuch.gesuchsteller2 = TestDataUtil.createGesuchsteller('Conchita', 'Prieto');
            gesuch.gesuchsteller2.addAdresse(TestDataUtil.createAdresse('strasse2', '20'));
            let umzugAdresseGS2: TSAdresseContainer = TestDataUtil.createAdresse('umzugstrasse2', '20');
            umzugAdresseGS2.showDatumVon = true;
            gesuch.gesuchsteller2.addAdresse(umzugAdresseGS2);

            spyOn(gesuchModelManager, 'getGesuch').and.returnValue(gesuch);

            umzugController = new UmzugViewController(gesuchModelManager, berechnungsManager,
                wizardStepManager, errorService, $translate, dialog, $q, $rootScope, $timeout);

            expect(umzugController.getUmzugAdressenList().length).toBe(2);
            expect(umzugController.getUmzugAdressenList()[0].betroffene).toBe(TSBetroffene.GESUCHSTELLER_1);
            expect(umzugController.getUmzugAdressenList()[0].adresse).toEqual(umzugAdresseGS1);
            expect(umzugController.getUmzugAdressenList()[1].betroffene).toBe(TSBetroffene.GESUCHSTELLER_2);
            expect(umzugController.getUmzugAdressenList()[1].adresse).toEqual(umzugAdresseGS2);
        });
        it('should merge the adresse of GS1 and GS2 in a single one with BEIDE_GESUCHSTELLER', function () {
            let gesuch: TSGesuch = new TSGesuch();
            gesuch.gesuchsteller1 = TestDataUtil.createGesuchsteller('Rodolfo', 'Langostino');
            let adresse1: TSAdresseContainer = TestDataUtil.createAdresse('strasse1', '10');
            let adresse2: TSAdresseContainer = TestDataUtil.createAdresse('strasse2', '20');
            gesuch.gesuchsteller1.addAdresse(adresse1);
            gesuch.gesuchsteller1.addAdresse(adresse2);

            gesuch.gesuchsteller2 = TestDataUtil.createGesuchsteller('Conchita', 'Prieto');
            gesuch.gesuchsteller2.addAdresse(adresse1);
            gesuch.gesuchsteller2.addAdresse(adresse2);
            spyOn(gesuchModelManager, 'getGesuch').and.returnValue(gesuch);

            umzugController = new UmzugViewController(gesuchModelManager, berechnungsManager,
                wizardStepManager, errorService, $translate, dialog, $q, $rootScope, $timeout);

            expect(umzugController.getUmzugAdressenList().length).toBe(1);
            expect(umzugController.getUmzugAdressenList()[0].betroffene).toBe(TSBetroffene.BEIDE_GESUCHSTELLER);
            TestDataUtil.checkGueltigkeitAndSetIfSame(umzugController.getUmzugAdressenList()[0].adresse.adresseJA, adresse2.adresseJA);
            expect(umzugController.getUmzugAdressenList()[0].adresse).toEqual(adresse2);
        });
    });

    describe('createAndRemoveUmzugAdresse', function () {
        it('should create and remove adressen for GS1 and GS2', function () {
            spyOn(dialog, 'showRemoveDialog').and.returnValue($q.when({}));
            TestDataUtil.mockDefaultGesuchModelManagerHttpCalls($httpBackend);

            let gesuch: TSGesuch = new TSGesuch();
            gesuch.gesuchsteller1 = TestDataUtil.createGesuchsteller('Rodolfo', 'Langostino');
            gesuch.gesuchsteller1.addAdresse(TestDataUtil.createAdresse('strasse1', '10'));
            let umzugAdresseGS1: TSAdresseContainer = TestDataUtil.createAdresse('umzugstrasse1', '10');
            umzugAdresseGS1.showDatumVon = true;
            gesuch.gesuchsteller1.addAdresse(umzugAdresseGS1);
            gesuch.gesuchsteller2 = TestDataUtil.createGesuchsteller('Conchita', 'Prieto');
            spyOn(gesuchModelManager, 'getGesuch').and.returnValue(gesuch);

            umzugController = new UmzugViewController(gesuchModelManager, berechnungsManager,
                wizardStepManager, errorService, $translate, dialog, $q, $rootScope, $timeout);

            expect(umzugController.getUmzugAdressenList().length).toBe(1);
            expect(umzugController.getUmzugAdressenList()[0].betroffene).toBe(TSBetroffene.GESUCHSTELLER_1);
            expect(umzugController.getUmzugAdressenList()[0].adresse).toEqual(umzugAdresseGS1);

            umzugController.createUmzugAdresse();

            expect(umzugController.getUmzugAdressenList().length).toBe(2);
            expect(umzugController.getUmzugAdressenList()[1].betroffene).toBeUndefined();
            expect(umzugController.getUmzugAdressenList()[1].adresse.adresseJA.adresseTyp).toBe(TSAdressetyp.WOHNADRESSE);
            expect(umzugController.getUmzugAdressenList()[1].adresse.showDatumVon).toBe(true);

            umzugController.removeUmzugAdresse(umzugController.getUmzugAdressenList()[0]);
            $rootScope.$apply();

            expect(umzugController.getUmzugAdressenList().length).toBe(1);
            expect(umzugController.getUmzugAdressenList()[0].betroffene).toBeUndefined();
            expect(umzugController.getUmzugAdressenList()[0].adresse.adresseJA.adresseTyp).toBe(TSAdressetyp.WOHNADRESSE);
            expect(umzugController.getUmzugAdressenList()[0].adresse.showDatumVon).toBe(true);
        });
    });
});
