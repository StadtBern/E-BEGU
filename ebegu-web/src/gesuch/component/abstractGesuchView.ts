import GesuchModelManager from '../service/gesuchModelManager';
import BerechnungsManager from '../service/berechnungsManager';
import {TSRole} from '../../models/enums/TSRole';
import {TSRoleUtil} from '../../utils/TSRoleUtil';
import WizardStepManager from '../service/wizardStepManager';
import {TSAntragStatus} from '../../models/enums/TSAntragStatus';
import {TSBetreuungsstatus} from '../../models/enums/TSBetreuungsstatus';
import TSExceptionReport from '../../models/TSExceptionReport';
import {TSMessageEvent} from '../../models/enums/TSErrorEvent';
import {TSWizardStepName} from '../../models/enums/TSWizardStepName';
import IPromise = angular.IPromise;
import IFormController = angular.IFormController;
import IScope = angular.IScope;
import ITimeoutService = angular.ITimeoutService;
import EbeguUtil from '../../utils/EbeguUtil';

export default class AbstractGesuchViewController<T> {

    $scope: IScope;
    gesuchModelManager: GesuchModelManager;
    berechnungsManager: BerechnungsManager;
    wizardStepManager: WizardStepManager;
    TSRole: any;
    TSRoleUtil: any;
    private _model: T;
    form: IFormController;
    $timeout: ITimeoutService;

    constructor($gesuchModelManager: GesuchModelManager, $berechnungsManager: BerechnungsManager,
                wizardStepManager: WizardStepManager, $scope: IScope, stepName: TSWizardStepName,
                $timeout: ITimeoutService) {
        this.gesuchModelManager = $gesuchModelManager;
        this.berechnungsManager = $berechnungsManager;
        this.wizardStepManager = wizardStepManager;
        this.TSRole = TSRole;
        this.TSRoleUtil = TSRoleUtil;
        this.$scope = $scope;
        this.$timeout = $timeout;
        this.wizardStepManager.setCurrentStep(stepName);
    }

    $onInit() {
        /**
         * Grund fuer diesen Code ist:
         * Wenn der Server einen Validation-Fehler zurueckgibt, wird der DirtyPlugin nicht informiert und setzt das Form
         * auf !dirty. Dann kann der Benutzer nochmal auf Speichern klicken und die Daten werden gespeichert.
         * Damit dies nicht passiert, hoeren wir in allen Views auf diesen Event und setzen das Form auf dirty
         */
        this.$scope.$on(TSMessageEvent[TSMessageEvent.ERROR_UPDATE], (event: any, errors: Array<TSExceptionReport>) => {
            this.form.$dirty = true;
            this.form.$pristine = false;
        });
    }

    public isGesuchReadonly(): boolean {
        return this.gesuchModelManager.isGesuchReadonly();
    }

    /**
     * Diese Methode prueft ob das Form valid ist. Sollte es nicht valid sein wird das erste fehlende Element gesucht
     * und fokusiert, damit der Benutzer nicht scrollen muss, um den Fehler zu finden.
     * Am Ende wird this.form.$valid zurueckgegeben
     */
    public isGesuchValid(): boolean {
        if (!this.form.$valid) {
            let element: any = angular.element('md-radio-group.ng-invalid,'
                + ' .ng-invalid>input,input.ng-invalid,select.ng-invalid,md-checkbox.ng-invalid').first();
            if (element) {
                element.focus();
            }
        }
        return this.form.$valid;
    }

    public getGesuchId(): string {
        if (this.gesuchModelManager && this.gesuchModelManager.getGesuch()) {
            return this.gesuchModelManager.getGesuch().id;
        } else {
            return '';
        }
    }

    public setGesuchStatus(status: TSAntragStatus): IPromise<TSAntragStatus> {
        if (this.gesuchModelManager) {
            return this.gesuchModelManager.saveGesuchStatus(status);
        }
        return undefined;
    }

    public isGesuchInStatus(status: TSAntragStatus): boolean {
        return status === this.gesuchModelManager.getGesuch().status;
    }

    public isBetreuungInStatus(status: TSBetreuungsstatus): boolean {
        if (this.gesuchModelManager.getBetreuungToWorkWith()) {
            return status === this.gesuchModelManager.getBetreuungToWorkWith().betreuungsstatus;
        }
        return false;
    }

    public isMutation(): boolean {
        if (this.gesuchModelManager.getGesuch()) {
            return this.gesuchModelManager.getGesuch().isMutation();
        }
        return false;
    }

    public isKorrekturModusJugendamt(): boolean {
        return this.gesuchModelManager.isKorrekturModusJugendamt();
    }

    get model(): T {
        return this._model;
    }

    set model(value: T) {
        this._model = value;
    }

    public extractFullNameGS1(): string {
        if (this.gesuchModelManager.getGesuch() && this.gesuchModelManager.getGesuch().gesuchsteller1) {
            return this.gesuchModelManager.getGesuch().gesuchsteller1.extractFullName();
        }
        return '';
    }

    public extractFullNameGS2(): string {
        if (this.gesuchModelManager.getGesuch() && this.gesuchModelManager.getGesuch().gesuchsteller2) {
            return this.gesuchModelManager.getGesuch().gesuchsteller2.extractFullName();
        }
        return '';
    }



    $postLink() {
        this.$timeout(() => {
            EbeguUtil.selectFirst();
        });
    }
}
