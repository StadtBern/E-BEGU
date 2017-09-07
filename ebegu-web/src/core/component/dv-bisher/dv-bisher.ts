import {IComponentOptions} from 'angular';
import DateUtil from '../../../utils/DateUtil';
import GesuchModelManager from '../../../gesuch/service/gesuchModelManager';
import * as moment from 'moment';
import Moment = moment.Moment;
import ITranslateService = angular.translate.ITranslateService;
import ILogService = angular.ILogService;
import {
    isAtLeastFreigegeben
} from '../../../models/enums/TSAntragStatus';
import {TSEingangsart} from '../../../models/enums/TSEingangsart';
let template = require('./dv-bisher.html');
require('./dv-bisher.less');

/**
 * Verwendung:
 * - gs: Wert, den der Gesuchsteller eingegeben hat
 * - ja: Wert, den das Jugendamt korrigiert
 *
 * Attribute fuer Eingaben-Blocks (zusammengehoerende Felder):
 * - specificBisherText: Falls einfach der GS-Wert als Bisher angezeigt werden soll. Z.B. Checkbox "Fachstelle": Wir
 *      wollen als Bisher-Text nicht "Ja", sondern "Fachstelle X, mit 50%, vom 01.01.2015 bis 31.12.2015"
 * - blockExisted: Gibt an, ob der Block ueberhaupt vom GS ausgefuellt wurde. Falls nein, muss *jede* Eingabe des JA
 *      als Korrektur angezeigt werden
 * - showIfBisherNone: Zeigt, ob "Keine Eingabe" angezeigt werden soll, wenn es keinen Bisher-Wert
 *      gibt. Normalerweise wollen wir das. Ausnahme sind Blocks, wo wir das "Keine Eingabe" *pro Block* anzeigen wollen
 *      und nicht unter jedem Feld.
 */
export class DvBisherComponentConfig implements IComponentOptions {
    transclude = false;
    bindings: any = {
        gs: '<',
        ja: '<',
        specificBisherText: '<',
        blockExisted: '<',
        showIfBisherNone: '<'
    };
    template = template;
    controller = DvBisher;
    controllerAs = 'vm';
}

export class DvBisher {

    static $inject = ['GesuchModelManager', '$translate', '$log'];

    gs: any;
    ja: any;
    showIfBisherNone: boolean;  // sollen die korrekturen des jugendamts angezeigt werden wenn im GS container kein wert ist
    specificBisherText: string;
    bisherText: Array<string>;
    blockExisted: boolean;


    /* @ngInject */
    constructor(private gesuchModelManager: GesuchModelManager, private $translate: ITranslateService, private $log: ILogService) {

    }

    $onInit() {
        if (this.showIfBisherNone === undefined) {//wenn nicht von aussen gesetzt auf true
            this.showIfBisherNone = true;
        }
    }

        public getBisher(): Array<string> {
        // noinspection IfStatementWithTooManyBranchesJS
        if (this.specificBisherText) {
            this.bisherText = this.specificBisherText ? this.specificBisherText.split('\n') : undefined;
            // War es eine Loeschung, oder ein Hinzufuegen?
            if (this.hasBisher()) {
                return this.bisherText; // neue eingabe als ein einzelner block
            } else {
                return [this.$translate.instant('LABEL_KEINE_ANGABE')];  //vorher war keine angabe da
            }
        } else if (this.gs instanceof moment) {
            return [DateUtil.momentToLocalDateFormat(this.gs, 'DD.MM.YYYY')];
        } else if (this.gs === true) {
            return [this.$translate.instant('LABEL_JA')];
        } else if (this.gs === false) {
            return [this.$translate.instant('LABEL_NEIN')];
        } else if (!this.hasBisher()) {
            return [this.$translate.instant('LABEL_KEINE_ANGABE')];
        } else {
            return [this.$translate.instant(this.gs)];
        }
    }

    public hasBisher(): boolean {
        return !this.isEmpty(this.gs);
    }

    public showBisher(): boolean {
        return ((this.showIfBisherNone || this.blockExisted === true) || this.hasBisher()) && this.isKorrekturModusJugendamtOrFreigegeben();
    }

    private isKorrekturModusJugendamtOrFreigegeben(): boolean {
        return isAtLeastFreigegeben(this.gesuchModelManager.getGesuch().status)
            && (TSEingangsart.ONLINE === this.gesuchModelManager.getGesuch().eingangsart);
    }

    public equals(gs: any, ja: any): boolean {
        if (gs instanceof moment) {
            return this.equals(DateUtil.momentToLocalDateFormat(gs, 'DD.MM.YYYY'), DateUtil.momentToLocalDateFormat(ja, 'DD.MM.YYYY'));
        }
        return gs === ja || (this.isEmpty(gs) && this.isEmpty(ja)); //either they are equal or both are a form of empty
    }

    private isEmpty(val: any): boolean {
        return val === undefined || val === null || val === '';
    }
}
