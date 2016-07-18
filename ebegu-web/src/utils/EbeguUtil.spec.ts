import TSGesuchsperiode from '../models/TSGesuchsperiode';
import {TSDateRange} from '../models/types/TSDateRange';
import DateUtil from '../utils/DateUtil';
import {EbeguWebCore} from '../core/core.module';
import EbeguUtil from './EbeguUtil';
import * as moment from 'moment';
import TSFall from '../models/TSFall';

describe('EbeguUtil', function () {

    let ebeguUtil: EbeguUtil;

    beforeEach(angular.mock.module(EbeguWebCore.name));

    // Das wird nur fuer tests gebraucht in denen etwas uebersetzt wird. Leider muss man dieses erstellen
    // bevor man den Injector erstellt hat. Deshalb muss es fuer alle Tests definiert werden
    beforeEach(angular.mock.module(function($provide: any) {
        let mockTranslateFilter = function(value: any) {
            if (value === 'FIRST') {
                return 'Erster';
            }
            if (value === 'SECOND') {
                return 'Zweiter';
            }
            return value;
        };
        $provide.value('translateFilter', mockTranslateFilter);
    }));

    beforeEach(angular.mock.inject(function ($injector: any) {
        ebeguUtil = $injector.get('EbeguUtil');
    }));

    describe('getGesuchsperiodeAsString', function () {
        it('should return the current Gesuchsperiode formatted', function () {
            var momentAb = DateUtil.today().year(2016);
            var momentBis = DateUtil.today().year(2017);
            let gesuchsperiode: TSGesuchsperiode = new TSGesuchsperiode(true, new TSDateRange(momentAb, momentBis));
            let result: string = ebeguUtil.getGesuchsperiodeAsString(gesuchsperiode);
            expect(result).toEqual('2016/2017');
        });
        it('should return undefined for an undefined Gesuchsperiode', function () {
            let result: string = ebeguUtil.getGesuchsperiodeAsString(undefined);
            expect(result).not.toBeDefined();
        });
    });
    describe('translateStringList', () => {
        it('should translate the given list of words', () => {
            let list: Array<string> = ['FIRST', 'SECOND'];
            let returnedList: Array<any> = ebeguUtil.translateStringList(list);
            expect(returnedList.length).toEqual(2);
            expect(returnedList[0].key).toEqual('FIRST');
            expect(returnedList[0].value).toEqual('Erster');
            expect(returnedList[1].key).toEqual('SECOND');
            expect(returnedList[1].value).toEqual('Zweiter');
        });
    });
    describe('addZerosToNumber', () => {
        it('returns a string with 6 chars starting with 0s and ending with the given number', () => {
            expect(ebeguUtil.addZerosToNumber(0, 2)).toEqual('00');
            expect(ebeguUtil.addZerosToNumber(1, 2)).toEqual('01');
            expect(ebeguUtil.addZerosToNumber(12, 2)).toEqual('12');
        });
        it('returns undefined if the number is undefined', () => {
            expect(ebeguUtil.addZerosToNumber(undefined, 2)).toBeUndefined();
            expect(ebeguUtil.addZerosToNumber(null, 2)).toBeUndefined();
        });
        it('returns the given number as string if its length is greather than 6', () => {
            expect(ebeguUtil.addZerosToNumber(1234567, 6)).toEqual('1234567');
        });
    });
    describe('calculateBetreuungsId', () => {
        it ('it returns empty string for undefined objects', () => {
            expect(ebeguUtil.calculateBetreuungsId(undefined, undefined, 0, 0)).toBe('');
        });
        it ('it returns empty string for undefined kindContainer', () => {
            let fall: TSFall = new TSFall();
            expect(ebeguUtil.calculateBetreuungsId(undefined, fall, 0, 0)).toBe('');
        });
        it ('it returns empty string for undefined betreuung', () => {
            let gesuchsperiode: TSGesuchsperiode = new TSGesuchsperiode();
            expect(ebeguUtil.calculateBetreuungsId(gesuchsperiode, undefined, 0, 0)).toBe('');
        });
        it ('it returns the right ID: YY(gesuchsperiodeBegin).fallNummer.Kind.Betreuung', () => {
            let fall: TSFall = new TSFall(254);
            let gesuchsperiode = new TSGesuchsperiode(true, new TSDateRange(moment('01.07.2016', 'DD.MM.YYYY'), moment('31.08.2017', 'DD.MM.YYYY')));
            expect(ebeguUtil.calculateBetreuungsId(gesuchsperiode, fall, 1, 1)).toBe('16.000254.1.1');
        });
    });
});