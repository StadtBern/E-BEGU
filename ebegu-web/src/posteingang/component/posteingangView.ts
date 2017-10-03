import {IComponentOptions} from 'angular';
import TSMitteilung from '../../models/TSMitteilung';
import MitteilungRS from '../../core/service/mitteilungRS.rest';
import EbeguUtil from '../../utils/EbeguUtil';
import IStateService = angular.ui.IStateService;
let template = require('./posteingangView.html');
require('./posteingangView.less');

export class PosteingangViewComponentConfig implements IComponentOptions {
    transclude = false;
    template = template;
    controller = PosteingangViewController;
    controllerAs = 'vm';
}

export class PosteingangViewController {

    private mitteilungen: Array<TSMitteilung>;

    itemsByPage: number = 20;
    numberOfPages: number = 1;

    static $inject: string[] = ['MitteilungRS', 'EbeguUtil', 'CONSTANTS', '$state'];

    constructor(private mitteilungRS: MitteilungRS, private ebeguUtil: EbeguUtil, private CONSTANTS: any, private $state: IStateService) {
        this.initViewModel();
    }

    public getMitteilungen() {
        return this.mitteilungen;
    }

    public addZerosToFallNummer(fallnummer: number): string {
        return this.ebeguUtil.addZerosToNumber(fallnummer, this.CONSTANTS.FALLNUMMER_LENGTH);
    }

    private initViewModel() {
        this.updatePosteingang();
    }

    private updatePosteingang() {
        this.mitteilungRS.getMitteilungenForPosteingang().then((response: any) => {
            this.mitteilungen = angular.copy(response);
            this.numberOfPages = this.mitteilungen.length / this.itemsByPage;
        });
    }

    private gotoMitteilung(mitteilung: TSMitteilung) {
        this.$state.go('mitteilungen', {
            fallId: mitteilung.fall.id
        });
    }
}
