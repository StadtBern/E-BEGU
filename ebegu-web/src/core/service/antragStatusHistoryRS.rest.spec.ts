import {TSAntragStatus} from '../../models/enums/TSAntragStatus';
import TSAntragStatusHistory from '../../models/TSAntragStatusHistory';
import TSGesuch from '../../models/TSGesuch';
import DateUtil from '../../utils/DateUtil';
import EbeguRestUtil from '../../utils/EbeguRestUtil';
import TestDataUtil from '../../utils/TestDataUtil';
import {EbeguWebCore} from '../core.module';
import AntragStatusHistoryRS from './antragStatusHistoryRS.rest';

describe('antragStatusHistoryRS', function () {

    let antragStatusHistoryRS: AntragStatusHistoryRS;
    let $httpBackend: angular.IHttpBackendService;
    let ebeguRestUtil: EbeguRestUtil;

    beforeEach(angular.mock.module(EbeguWebCore.name));

    beforeEach(angular.mock.inject(function ($injector: angular.auto.IInjectorService) {
        antragStatusHistoryRS = $injector.get('AntragStatusHistoryRS');
        $httpBackend = $injector.get('$httpBackend');
        ebeguRestUtil = $injector.get('EbeguRestUtil');
    }));

    describe('Public API', function () {
        it('check URI', function () {
            expect(antragStatusHistoryRS.serviceURL).toContain('antragStatusHistory');
        });
        it('check Service name', function () {
            expect(antragStatusHistoryRS.getServiceName()).toBe('AntragStatusHistoryRS');
        });
        it('should include a loadLastStatusChange() function', function () {
            expect(antragStatusHistoryRS.loadLastStatusChange).toBeDefined();
        });
    });

    describe('loadLastStatusChange', () => {
        it('should return the last status change for the given gesuch', () => {
            let gesuch: TSGesuch = new TSGesuch();
            gesuch.id = '123456';
            let antragStatusHistory: TSAntragStatusHistory = new TSAntragStatusHistory(gesuch.id, undefined, DateUtil.today(), undefined, TSAntragStatus.VERFUEGEN);
            TestDataUtil.setAbstractFieldsUndefined(antragStatusHistory);
            let restAntStatusHistory: any = ebeguRestUtil.antragStatusHistoryToRestObject({}, antragStatusHistory);
            $httpBackend.expectGET(antragStatusHistoryRS.serviceURL + '/' + encodeURIComponent(gesuch.id)).respond(restAntStatusHistory);

            let lastStatusChange: TSAntragStatusHistory;
            antragStatusHistoryRS.loadLastStatusChange(gesuch).then((response) => {
                lastStatusChange = response;
            });
            $httpBackend.flush();

            expect(lastStatusChange).toBeDefined();
            expect(lastStatusChange.timestampVon.isSame(antragStatusHistory.timestampVon)).toBe(true);
            lastStatusChange.timestampVon = antragStatusHistory.timestampVon;
            expect(lastStatusChange).toEqual(antragStatusHistory);
        });
        it('should return undefined if the gesuch is undefined', () => {
            antragStatusHistoryRS.loadLastStatusChange(undefined);
            expect(antragStatusHistoryRS.lastChange).toBeUndefined();
        });
        it('should return undefined if the gesuch id is undefined', () => {
            let gesuch: TSGesuch = new TSGesuch();
            gesuch.id = undefined;
            antragStatusHistoryRS.loadLastStatusChange(gesuch);
            expect(antragStatusHistoryRS.lastChange).toBeUndefined();
        });
    });

});
