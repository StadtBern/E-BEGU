import {IComponentOptions, IPromise, IQService, IScope} from 'angular';
import AbstractGesuchViewController from '../abstractGesuchView';
import GesuchModelManager from '../../service/gesuchModelManager';
import BerechnungsManager from '../../service/berechnungsManager';
import TSGesuch from '../../../models/TSGesuch';
import ErrorService from '../../../core/errors/service/ErrorService';
import {INewFallStateParams} from '../../gesuch.route';
import WizardStepManager from '../../service/wizardStepManager';
import {TSWizardStepName} from '../../../models/enums/TSWizardStepName';
import {TSAntragTyp} from '../../../models/enums/TSAntragTyp';
import {TSRoleUtil} from '../../../utils/TSRoleUtil';
import AuthServiceRS from '../../../authentication/service/AuthServiceRS.rest';
import GesuchsperiodeRS from '../../../core/service/gesuchsperiodeRS.rest';
import TSGesuchsperiode from '../../../models/TSGesuchsperiode';
import {TSGesuchsperiodeStatus} from '../../../models/enums/TSGesuchsperiodeStatus';
import ITranslateService = angular.translate.ITranslateService;
import ITimeoutService = angular.ITimeoutService;

let template = require('./fallCreationView.html');
require('./fallCreationView.less');

export class FallCreationViewComponentConfig implements IComponentOptions {
    transclude = false;
    template = template;
    controller = FallCreationViewController;
    controllerAs = 'vm';
}

export class FallCreationViewController extends AbstractGesuchViewController<any> {
    private gesuchsperiodeId: string;

    TSRoleUtil: any;

    // showError ist ein Hack damit, die Fehlermeldung fuer die Checkboxes nicht direkt beim Laden der Seite angezeigt wird
    // sondern erst nachdem man auf ein checkbox oder auf speichern geklickt hat
    showError: boolean = false;
    private nichtAbgeschlosseneGesuchsperiodenList: Array<TSGesuchsperiode>;

    static $inject = ['GesuchModelManager', 'BerechnungsManager', 'ErrorService', '$stateParams',
        'WizardStepManager', '$translate', '$q', '$scope', 'AuthServiceRS', 'GesuchsperiodeRS', '$timeout'];

    /* @ngInject */
    constructor(gesuchModelManager: GesuchModelManager, berechnungsManager: BerechnungsManager,
                private errorService: ErrorService, private $stateParams: INewFallStateParams, wizardStepManager: WizardStepManager,
                private $translate: ITranslateService, private $q: IQService, $scope: IScope, private authServiceRS: AuthServiceRS,
                private gesuchsperiodeRS: GesuchsperiodeRS, $timeout: ITimeoutService) {
        super(gesuchModelManager, berechnungsManager, wizardStepManager, $scope, TSWizardStepName.GESUCH_ERSTELLEN, $timeout);
        this.TSRoleUtil = TSRoleUtil;
    }

    $onInit() {
        this.readStateParams();
        this.initViewModel();
    }

    private readStateParams() {
        if (this.$stateParams.gesuchsperiodeId && this.$stateParams.gesuchsperiodeId !== '') {
            this.gesuchsperiodeId = this.$stateParams.gesuchsperiodeId;
        }
    }

    public setShowError(showError: boolean): void {
        this.showError = showError;
    }

    private initViewModel(): void {
        //gesuch should already have been initialized in resolve function
        if (this.gesuchsperiodeId === null || this.gesuchsperiodeId === undefined || this.gesuchsperiodeId === '') {
            if (this.gesuchModelManager.getGesuchsperiode()) {
                this.gesuchsperiodeId = this.gesuchModelManager.getGesuchsperiode().id;
            }
        }
        this.gesuchsperiodeRS.getAllNichtAbgeschlosseneNichtVerwendeteGesuchsperioden(this.$stateParams.fallId).then((response: TSGesuchsperiode[]) => {
            this.nichtAbgeschlosseneGesuchsperiodenList = angular.copy(response);
        });
    }

    save(): IPromise<TSGesuch> {
        this.showError = true;
        if (this.isGesuchValid()) {
            if (!this.form.$dirty && !this.gesuchModelManager.getGesuch().isNew()) {
                // If there are no changes in form we don't need anything to update on Server and we could return the
                // promise immediately
                return this.$q.when(this.gesuchModelManager.getGesuch());
            }
            this.errorService.clearAll();
            if (this.gesuchModelManager.getGesuch().isNew()) {
                if (this.gesuchModelManager.getGesuch().typ === TSAntragTyp.MUTATION) {
                    this.berechnungsManager.clear();
                    return this.gesuchModelManager.saveMutation();
                } else if (this.gesuchModelManager.getGesuch().typ === TSAntragTyp.ERNEUERUNGSGESUCH) {
                    this.berechnungsManager.clear();
                    return this.gesuchModelManager.saveErneuerungsgesuch();
                }
            }
            return this.gesuchModelManager.saveGesuchAndFall();
        }
        return undefined;
    }

    public getGesuchModel(): TSGesuch {
        return this.gesuchModelManager.getGesuch();
    }

    public getAllActiveGesuchsperioden() {
        return this.nichtAbgeschlosseneGesuchsperiodenList;
    }

    public setSelectedGesuchsperiode(): void {
        let gesuchsperiodeList = this.getAllActiveGesuchsperioden();
        for (let i: number = 0; i < gesuchsperiodeList.length; i++) {
            if (gesuchsperiodeList[i].id === this.gesuchsperiodeId) {
                this.getGesuchModel().gesuchsperiode = gesuchsperiodeList[i];
            }
        }
    }

    public isGesuchsperiodeActive(): boolean {
        if (this.gesuchModelManager.getGesuchsperiode()) {
            return TSGesuchsperiodeStatus.AKTIV === this.gesuchModelManager.getGesuchsperiode().status
                || TSGesuchsperiodeStatus.INAKTIV === this.gesuchModelManager.getGesuchsperiode().status;
        } else {
            return true;
        }
    }

    public getTitle(): string {
        if (this.gesuchModelManager.isGesuch()) {
            if (this.gesuchModelManager.isGesuchSaved() && this.gesuchModelManager.getGesuchsperiode()) {
                let key = (this.gesuchModelManager.getGesuch().typ === TSAntragTyp.ERNEUERUNGSGESUCH) ? 'KITAX_ERNEUERUNGSGESUCH_PERIODE' : 'KITAX_ERSTGESUCH_PERIODE';
                return this.$translate.instant(key, {
                    periode: this.gesuchModelManager.getGesuchsperiode().gesuchsperiodeString
                });
            } else {
                let key = (this.gesuchModelManager.getGesuch().typ === TSAntragTyp.ERNEUERUNGSGESUCH) ? 'KITAX_ERNEUERUNGSGESUCH' : 'KITAX_ERSTGESUCH';
                return this.$translate.instant(key);
            }
        } else {
            return this.$translate.instant('ART_DER_MUTATION');
        }
    }

    public getNextButtonText(): string {
        if (this.gesuchModelManager.getGesuch().isNew()) {
            return this.$translate.instant('ERSTELLEN');
        }
        if (this.gesuchModelManager.isGesuchReadonly() || this.authServiceRS.isOneOfRoles(this.TSRoleUtil.getGesuchstellerOnlyRoles())) {
            return this.$translate.instant('WEITER_ONLY_UPPER');
        }
        return this.$translate.instant('WEITER_UPPER');
    }

    public isSelectedGesuchsperiodeInaktiv(): boolean {
        return this.getGesuchModel() && this.getGesuchModel().gesuchsperiode
            && this.getGesuchModel().gesuchsperiode.status === TSGesuchsperiodeStatus.INAKTIV
            && this.getGesuchModel().isNew();
    }

    public canChangeGesuchsperiode(): boolean {
        return this.gesuchModelManager.isGesuch() && this.isGesuchsperiodeActive() && this.gesuchModelManager.getGesuch().isNew();
    }
}
