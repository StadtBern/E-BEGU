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

import {IComponentOptions, ILogService} from 'angular';
import MitteilungRS from '../../service/mitteilungRS.rest';
import {TSAuthEvent} from '../../../models/enums/TSAuthEvent';
import AuthServiceRS from '../../../authentication/service/AuthServiceRS.rest';
import {TSRoleUtil} from '../../../utils/TSRoleUtil';
import IRootScopeService = angular.IRootScopeService;
let template = require('./dv-posteingang.html');

export class DvPosteingangComponentConfig implements IComponentOptions {
    transclude = false;
    bindings: any = {};
    template = template;
    controller = DvPosteingangController;
    controllerAs = 'vm';
}

export class DvPosteingangController {

    amountMitteilungen: number = 0;
    reloadAmountMitteilungenInterval: number;

    static $inject: any[] = ['MitteilungRS', '$rootScope', 'AuthServiceRS', '$log'];

    constructor(private mitteilungRS: MitteilungRS, private $rootScope: IRootScopeService, private authServiceRS: AuthServiceRS,
        private $log: ILogService) {
        this.getAmountNewMitteilungen();

        this.$rootScope.$on('POSTEINGANG_MAY_CHANGED', (event: any) => {
            this.getAmountNewMitteilungen();
        });

        this.$rootScope.$on(TSAuthEvent[TSAuthEvent.LOGIN_SUCCESS], () => {
            if (this.authServiceRS.isOneOfRoles(TSRoleUtil.getGesuchstellerJugendamtRoles())) {
                this.getAmountNewMitteilungen(); // call it a first time

                if (this.authServiceRS.isOneOfRoles(TSRoleUtil.getAdministratorJugendamtRole())) { // not for GS
                    // call every 5 minutes (5*60*1000)
                    this.reloadAmountMitteilungenInterval = setInterval(() => this.getAmountNewMitteilungen(), 300000);
                }
            }
        });

        // Das Interval muss nach jedem LOGOUT entfernt werden, um zu vermeiden dass es bei Benutzern auftritt die keinen Mitteilungen haben
        this.$rootScope.$on(TSAuthEvent[TSAuthEvent.LOGOUT_SUCCESS], () => {
            clearInterval(this.reloadAmountMitteilungenInterval);
        });
    }

    private getAmountNewMitteilungen(): void {
        this.mitteilungRS.getAmountMitteilungenForCurrentBenutzer().then((response: number) => {
            if (!response || isNaN(response)) { //wenn keine gueltige antwort
                this.amountMitteilungen = 0;
            } else {
                this.amountMitteilungen = response;
            }
        }).catch((err) => {
            //Fehler bei deisem request (notokenrefresh )werden bis hier ohne Behandlung
            // (unerwarteter Fehler anzeige, redirect etc.) weitergeschlauft
            this.$log.debug('received error message while reading posteingang. Ignoring ...');
            this.amountMitteilungen = 0;
        });
    }

}
