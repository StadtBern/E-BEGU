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

import {IDirective, IDirectiveFactory} from 'angular';
import {DvDialog} from './dv-dialog/dv-dialog';
import {FreigabeController} from '../../gesuch/dialog/FreigabeController';
import AuthServiceRS from '../../authentication/service/AuthServiceRS.rest';
import {TSRoleUtil} from '../../utils/TSRoleUtil';
import ErrorService from '../errors/service/ErrorService';
import {TSAuthEvent} from '../../models/enums/TSAuthEvent';
import * as moment from 'moment';
import ITimeoutService = angular.ITimeoutService;
import IDocumentService = angular.IDocumentService;
import ILogService = angular.ILogService;
import IRootScopeService = angular.IRootScopeService;

let FREIGEBEN_DIALOG_TEMPLATE = require('../../gesuch/dialog/freigabe.html');

export class DVBarcodeListener implements IDirective {
    restrict = 'A';
    controller = DVBarcodeController;
    controllerAs = 'vm';

    static factory(): IDirectiveFactory {
        const directive = () => new DVBarcodeListener();
        directive.$inject = [];
        return directive;
    }
}

/**
 * This binds a listener for a certain keypress sequence to the document. If this keypress sequence (escaped with §)
 * is found then we open the dialog
 * The format of an expected barcode sequence is §FREIGABE|OPEN|cd85e001-403f-407f-8eb8-102c402342b6§
 */
export class DVBarcodeController {

    static $inject: string[] = ['$document', '$timeout', 'DvDialog', 'AuthServiceRS', 'ErrorService', '$log', '$rootScope'];

    private barcodeReading: boolean = false;
    private barcodeBuffer: string[] = [];
    private barcodeReadtimeout: any = null;

    /* @ngInject */
    constructor(private $document: IDocumentService, private $timeout: ITimeoutService, private dVDialog: DvDialog, private authService: AuthServiceRS,
                private errorService: ErrorService, private $log: ILogService, private $rootScope: IRootScopeService) {

    }

    $onInit() {
        let keypressEvent = (e: any) => {
            this.barcodeOnKeyPressed(e);
        };

        this.$rootScope.$on(TSAuthEvent[TSAuthEvent.LOGIN_SUCCESS], () => {
            this.$document.unbind('keypress', keypressEvent);
            if (this.authService.isOneOfRoles(TSRoleUtil.getAdministratorJugendamtSchulamtRoles())) {
                this.$document.bind('keypress', keypressEvent);
            }
        });

        this.$rootScope.$on(TSAuthEvent[TSAuthEvent.LOGOUT_SUCCESS], () => {
            this.$document.unbind('keypress', keypressEvent);
        });

    }

    public barcodeOnKeyPressed(e: any): void {

        let keyPressChar: string = e.key ? e.key : String.fromCharCode(e.which);

        if (this.barcodeReading) {
            e.preventDefault();
            if (keyPressChar !== '§') {
                this.barcodeBuffer.push(keyPressChar);
                this.$log.debug('Current buffer: ' + this.barcodeBuffer.join(''));
            }
        }

        if (keyPressChar === '§') {
            e.preventDefault();
            if (this.barcodeReading) {
                this.$log.debug('End Barcode read');

                let barcodeRead: string = this.barcodeBuffer.join('');
                this.$log.debug('Barcode read:' + barcodeRead);
                barcodeRead = barcodeRead.replace('§', '');

                let barcodeParts: string[] = barcodeRead.split('|');

                if (barcodeParts.length === 3) {
                    let barcodeDocType: string = barcodeParts[0];
                    let barcodeDocFunction: string = barcodeParts[1];
                    let barcodeDocID: string = barcodeParts[2];

                    this.$log.debug('Barcode Doc Type: ' + barcodeDocType);
                    this.$log.debug('Barcode Doc Function: ' + barcodeDocFunction);
                    this.$log.debug('Barcode Doc ID: ' + barcodeDocID);

                    this.barcodeBuffer = [];
                    this.$timeout.cancel(this.barcodeReadtimeout);

                    this.dVDialog.showDialogFullscreen(FREIGEBEN_DIALOG_TEMPLATE, FreigabeController, {
                        docID: barcodeDocID
                    });
                } else {
                    this.errorService.addMesageAsError('Barcode hat falsches Format: ' + barcodeRead);
                }
            } else {
                this.$log.debug('Begin Barcode read');

                this.barcodeReadtimeout = this.$timeout(() => {
                    this.barcodeReading = false;
                    this.$log.debug('End Barcode read');
                    this.$log.debug('Clearing buffer: ' + this.barcodeBuffer.join(''));
                    this.barcodeBuffer = [];
                }, 1000);
            }

            this.barcodeReading = !this.barcodeReading;

        }
    }
}
