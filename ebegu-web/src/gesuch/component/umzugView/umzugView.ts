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

import AbstractGesuchViewController from '../abstractGesuchView';
import {IComponentOptions, IPromise} from 'angular';
import GesuchModelManager from '../../service/gesuchModelManager';
import BerechnungsManager from '../../service/berechnungsManager';
import ErrorService from '../../../core/errors/service/ErrorService';
import WizardStepManager from '../../service/wizardStepManager';
import {TSWizardStepName} from '../../../models/enums/TSWizardStepName';
import {TSWizardStepStatus} from '../../../models/enums/TSWizardStepStatus';
import {TSBetroffene} from '../../../models/enums/TSBetroffene';
import TSAdresse from '../../../models/TSAdresse';
import {TSAdressetyp} from '../../../models/enums/TSAdressetyp';
import TSUmzugAdresse from '../../../models/TSUmzugAdresse';
import {DvDialog} from '../../../core/directive/dv-dialog/dv-dialog';
import {RemoveDialogController} from '../../dialog/RemoveDialogController';
import TSGesuchstellerContainer from '../../../models/TSGesuchstellerContainer';
import TSAdresseContainer from '../../../models/TSAdresseContainer';
import EbeguUtil from '../../../utils/EbeguUtil';
import ITranslateService = angular.translate.ITranslateService;
import ITimeoutService = angular.ITimeoutService;
import IQService = angular.IQService;
import IScope = angular.IScope;

let template = require('./umzugView.html');
require('./umzugView.less');
let removeDialogTemplate = require('../../dialog/removeDialogTemplate.html');

export class UmzugViewComponentConfig implements IComponentOptions {
    transclude = false;
    bindings: any = {};
    template = template;
    controller = UmzugViewController;
    controllerAs = 'vm';
}

export class UmzugViewController extends AbstractGesuchViewController<Array<TSUmzugAdresse>> {

    dirty = false;

    static $inject = ['GesuchModelManager', 'BerechnungsManager', 'WizardStepManager', 'ErrorService', '$translate',
        'DvDialog', '$q', '$scope', '$timeout'];

    /* @ngInject */
    constructor(gesuchModelManager: GesuchModelManager, berechnungsManager: BerechnungsManager,
                wizardStepManager: WizardStepManager, private errorService: ErrorService,
                private $translate: ITranslateService, private DvDialog: DvDialog, private $q: IQService,
                $scope: IScope, $timeout: ITimeoutService) {

        super(gesuchModelManager, berechnungsManager, wizardStepManager, $scope, TSWizardStepName.UMZUG, $timeout);
        this.initViewModel();
    }

    private initViewModel(): void {
        this.model = [];
        this.wizardStepManager.updateCurrentWizardStepStatus(TSWizardStepStatus.OK);
        this.extractAdressenListFromBothGS();
    }

    public getUmzugAdressenList(): Array<TSUmzugAdresse> {
        return this.model;
    }

    public save(): IPromise<TSGesuchstellerContainer> {
        if (this.isGesuchValid()) {
            if (!this.form.$dirty && !this.dirty) {
                // If there are no changes in form we don't need anything to update on Server and we could return the
                // promise immediately
                return this.$q.when(this.gesuchModelManager.getStammdatenToWorkWith());
            }

            this.errorService.clearAll();
            this.saveAdresseInGS();
            this.gesuchModelManager.setGesuchstellerNumber(1);
            return this.gesuchModelManager.updateGesuchsteller(true).then((response) => {
                if (this.gesuchModelManager.getGesuch().gesuchsteller2) {
                    this.gesuchModelManager.setGesuchstellerNumber(2);
                    return this.gesuchModelManager.updateGesuchsteller(true);
                }
                return this.gesuchModelManager.getStammdatenToWorkWith();
            });
        }
        return undefined;
    }

    /**
     * Hier schauen wir wie viele GS es gibt und dementsprechen fuellen wir die Liste aus.
     * Bei Mutationen wird es nur geschaut ob der GS existiert (!=null), da die Familiensituation nicht relevant ist.
     * Es koennte einen GS2 geben obwohl die neue Familiensituation "ledig" sagt
     */
    public getBetroffenenList(): Array<TSBetroffene> {
        let betroffenenList: Array<TSBetroffene> = [];
        if (this.gesuchModelManager.getGesuch()) {
            if (this.gesuchModelManager.getGesuch().gesuchsteller1) {
                betroffenenList.push(TSBetroffene.GESUCHSTELLER_1);
            }
            if (this.gesuchModelManager.getGesuch().gesuchsteller2) {
                betroffenenList.push(TSBetroffene.GESUCHSTELLER_2);
            }
            if (this.gesuchModelManager.getGesuch().gesuchsteller2 && this.gesuchModelManager.getGesuch().gesuchsteller1) {
                // Dies koennte auch direkt beim Push des GS2 gemacht werden, da es keinen GS2 geben darf wenn es
                // keinen GS1 gibt. Allerdings sind wir mit diesem IF sicher dass GS1 und GS2 wirklich existieren.
                betroffenenList.push(TSBetroffene.BEIDE_GESUCHSTELLER);
            }
        }
        return betroffenenList; // empty list wenn die Daten nicht richtig sind
    }

    public getNameFromBetroffene(betroffene: TSBetroffene): string {
        if (TSBetroffene.GESUCHSTELLER_1 === betroffene && this.gesuchModelManager.getGesuch().gesuchsteller1) {
            return this.gesuchModelManager.getGesuch().gesuchsteller1.extractFullName();

        } else if (TSBetroffene.GESUCHSTELLER_2 === betroffene && this.gesuchModelManager.getGesuch().gesuchsteller2) {
            return this.gesuchModelManager.getGesuch().gesuchsteller2.extractFullName();

        } else if (TSBetroffene.BEIDE_GESUCHSTELLER === betroffene) {
            return this.$translate.instant(TSBetroffene[betroffene]);
        }

        return '';
    }

    private extractAdressenListFromBothGS() {
        this.getAdressenListFromGS1();
        this.getAdressenListFromGS2();
    }

    private getAdressenListFromGS1(): void {
        if (this.gesuchModelManager.getGesuch() && this.gesuchModelManager.getGesuch().gesuchsteller1) {
            this.gesuchModelManager.getGesuch().gesuchsteller1.getUmzugAdressen().forEach(umzugAdresse => {
                umzugAdresse.showDatumVon = true; // wird benoetigt weil es vom Server nicht kommt
                this.model.push(new TSUmzugAdresse(TSBetroffene.GESUCHSTELLER_1, umzugAdresse));
            });
        }
    }

    /**
     * Geht durch die Adressenliste des GS2 durch. Wenn eine Adresse von GS2
     */
    private getAdressenListFromGS2(): void {
        if (this.gesuchModelManager.getGesuch() && this.gesuchModelManager.getGesuch().gesuchsteller2) {
            this.gesuchModelManager.getGesuch().gesuchsteller2.getUmzugAdressen().forEach(umzugAdresse => {
                umzugAdresse.showDatumVon = true; // wird benoetigt weil es vom Server nicht kommt
                let foundPosition: number = -1;
                for (let i = 0; i < this.model.length; i++) {
                    if (this.model[i].adresse.isSameWohnAdresse(umzugAdresse)) {
                        foundPosition = i;
                    }
                }
                if (foundPosition >= 0) {
                    this.model[foundPosition].betroffene = TSBetroffene.BEIDE_GESUCHSTELLER;

                    // speichern der AdressContainer vom Gs2 damit wir sie später wieder finden
                    this.model[foundPosition].adresseGS2 = umzugAdresse;
                } else {
                    this.model.push(new TSUmzugAdresse(TSBetroffene.GESUCHSTELLER_2, umzugAdresse));
                }
            });
        }
    }

    public removeUmzugAdresse(adresse: TSUmzugAdresse): void {
        let remTitleText = this.$translate.instant('UMZUG_LOESCHEN');
        this.DvDialog.showRemoveDialog(removeDialogTemplate, this.form, RemoveDialogController, {
            title: remTitleText,
            deleteText: '',
            parentController: undefined,
            elementID: undefined
        }).then(() => {   //User confirmed removal
            this.dirty = true;
            let indexOf = this.model.lastIndexOf(adresse);
            if (indexOf >= 0) {
                this.model.splice(indexOf, 1);
            }
            this.$timeout(() => EbeguUtil.selectFirst(), 100);
        });
    }

    /**
     * Erstellt eine neue leere Adresse vom Typ WOHNADRESSE
     */
    public createUmzugAdresse(): void {
        let adresseContainer = this.createAdressContainer();
        let umzugAdresse: TSUmzugAdresse = new TSUmzugAdresse(undefined, adresseContainer);

        this.model.push(umzugAdresse);
        this.dirty = true;
        this.$postLink();
        //todo focus on specific id, so the newly added umzug will be selected not the first in the DOM
    }

    private createAdressContainer() {
        let adresseContainer: TSAdresseContainer = new TSAdresseContainer();
        let adresse: TSAdresse = new TSAdresse();
        adresse.adresseTyp = TSAdressetyp.WOHNADRESSE;
        adresseContainer.showDatumVon = true;
        adresseContainer.adresseJA = adresse;
        return adresseContainer;
    }

    /**
     * Zuerst entfernt alle Elemente der Arrays von adressen vom GS1 und GS2, ausser dem ersten Element (Wohnadresse).
     * Danach fuellt diese mit den Adressen die hier geblieben sind bzw. nicht entfernt wurden, dafuer
     * nimmt es aus der Liste von umzugAdressen alle eingegebenen Adressen und speichert sie in dem entsprechenden GS
     */
    private saveAdresseInGS(): void {
        if (this.gesuchModelManager.getGesuch().gesuchsteller1 && this.gesuchModelManager.getGesuch().gesuchsteller1.adressen
            && this.gesuchModelManager.getGesuch().gesuchsteller1.adressen.length > 0) {
            this.gesuchModelManager.getGesuch().gesuchsteller1.adressen.length = 1;
        }
        if (this.gesuchModelManager.getGesuch().gesuchsteller2 && this.gesuchModelManager.getGesuch().gesuchsteller2.adressen
            && this.gesuchModelManager.getGesuch().gesuchsteller2.adressen.length > 0) {
            this.gesuchModelManager.getGesuch().gesuchsteller2.adressen.length = 1;
        }
        this.model.forEach(umzugAdresse => {

            if (TSBetroffene.GESUCHSTELLER_1 === umzugAdresse.betroffene) {
                this.addAdresseToGS(this.gesuchModelManager.getGesuch().gesuchsteller1, umzugAdresse.adresse);

            } else if (TSBetroffene.GESUCHSTELLER_2 === umzugAdresse.betroffene) {
                this.addAdresseToGS(this.gesuchModelManager.getGesuch().gesuchsteller2, umzugAdresse.adresse);

            } else if (TSBetroffene.BEIDE_GESUCHSTELLER === umzugAdresse.betroffene) {
                this.addAdresseToGS(this.gesuchModelManager.getGesuch().gesuchsteller1, umzugAdresse.adresse);

                if (!umzugAdresse.adresseGS2) {
                    umzugAdresse.adresseGS2 = this.createAdressContainer();
                }
                umzugAdresse.adresseGS2.adresseJA.copy(umzugAdresse.adresse.adresseJA);
                this.addAdresseToGS(this.gesuchModelManager.getGesuch().gesuchsteller2, umzugAdresse.adresseGS2);
            }
        });
    }

    private addAdresseToGS(gesuchsteller: TSGesuchstellerContainer, adresse: TSAdresseContainer) {
        if (gesuchsteller) {
            if (gesuchsteller.adressen.indexOf(adresse) < 0) {
                gesuchsteller.addAdresse(adresse);
            } else {
                //update old adresse
            }
        }
    }

    public getPreviousButtonText(): string {
        if (this.getUmzugAdressenList().length === 0) {
            return 'ZURUECK_ONLY_UPPER';
        }
        return 'ZURUECK_UPPER';
    }

    public getNextButtonText(): string {
        if (this.getUmzugAdressenList().length === 0) {
            return 'WEITER_ONLY_UPPER';
        }
        return 'WEITER_UPPER';
    }
}
