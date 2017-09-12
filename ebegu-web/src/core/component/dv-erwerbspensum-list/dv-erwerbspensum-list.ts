import {IComponentOptions} from 'angular';
import TSErwerbspensum from '../../../models/TSErwerbspensum';
import TSErwerbspensumContainer from '../../../models/TSErwerbspensumContainer';
import AuthServiceRS from '../../../authentication/service/AuthServiceRS.rest';

let template = require('./dv-erwerbspensum-list.html');
require('./dv-erwerbspensum-list.less');

export class DVErwerbspensumListConfig implements IComponentOptions {
    transclude = false;

    bindings: any = {
        onRemove: '&',
        onAdd: '&',
        onEdit: '&',
        erwerbspensen: '<',
        tableId: '@',
        tableTitle: '@',
        addButtonVisible: '<',
        addButtonEnabled: '<',
        addButtonText: '@',
        inputId: '@'
    };
    template = template;
    controller = DVErwerbspensumListController;
    controllerAs = 'vm';
}

export class DVErwerbspensumListController {

    erwerbspensen: TSErwerbspensum[];
    tableId: string;
    tableTitle: string;
    inputId: string;
    addButtonText: string;
    addButtonVisible: boolean;
    addButtonEnabled: boolean;
    onRemove: (pensumToRemove: any) => void;
    onEdit: (pensumToEdit: any) => void;
    onAdd: () => void;

    static $inject: any[] = ['AuthServiceRS'];
    /* @ngInject */
    constructor(private authServiceRS: AuthServiceRS) {
    }

    $onInit() {
        if (!this.addButtonText) {
            this.addButtonText = 'add item';
        }
        if (this.addButtonVisible === undefined) {
            this.addButtonVisible = true;
        }
        if (this.addButtonEnabled === undefined) {
            this.addButtonEnabled = true;
        }
        //clear selected
        for (let i = 0; i < this.erwerbspensen.length; i++) {
            let obj: any = this.erwerbspensen[i];
            obj.isSelected = false;

        }
    }

    removeClicked(pensumToRemove: TSErwerbspensumContainer, index: any) {
        this.onRemove({pensum: pensumToRemove, index: index});
    }

    editClicked(pensumToEdit: any) {
        this.onEdit({pensum: pensumToEdit});
    }

    addClicked() {
        this.onAdd();
    }

    isRemoveAllowed(pensumToEdit: any) {
        // Loeschen erlaubt, solange das Gesuch noch nicht readonly ist. Dies ist notwendig, weil sonst in die Zukunft
        // erfasste Taetigkeiten bei nicht-zustandekommen des Jobs nicht mehr geloescht werden koennen
        // Siehe auch EBEGU-1146 und EBEGU-580
        return this.addButtonVisible;
    }
}



