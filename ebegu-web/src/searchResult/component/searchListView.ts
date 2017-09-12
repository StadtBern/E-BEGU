import {IComponentOptions} from 'angular';
import {ISearchResultateStateParams} from '../search.route';
import TSQuickSearchResult from '../../models/dto/TSQuickSearchResult';
import {SearchIndexRS} from '../../core/service/searchIndexRS.rest';
import EbeguUtil from '../../utils/EbeguUtil';
import TSAbstractAntragDTO from '../../models/TSAbstractAntragDTO';
import ILogService = angular.ILogService;

let template = require('./searchListView.html');
require('./searchListView.less');

export class SearchListViewComponentConfig implements IComponentOptions {
    transclude = false;
    template = template;
    controller = SearchListViewController;
    controllerAs = 'vm';
}

export class SearchListViewController {

    private antragList: Array<TSAbstractAntragDTO>;
    totalResultCount: string = '-';
    private ignoreRequest: boolean = true; //we want to ignore the first filter request because the default sort triggers always a second one
    searchString: string;


    static $inject: string[] = [ '$log', '$stateParams', 'SearchIndexRS', 'EbeguUtil'];

    constructor(private $log: ILogService,  $stateParams: ISearchResultateStateParams,
                private searchIndexRS: SearchIndexRS, private ebeguUtil: EbeguUtil) {
        this.searchString = $stateParams.searchString;
        this.initViewModel();

    }

    private initViewModel() {
        this.searchIndexRS.globalSearch(this.searchString).then((quickSearchResult: TSQuickSearchResult) => {
            this.antragList = [];
            for (let res of quickSearchResult.resultEntities) {
                this.antragList.push(res.antragDTO);
            }
            this.ebeguUtil.handleSmarttablesUpdateBug(this.antragList);
        }).catch(() => {
            this.$log.warn('error during globalSearch');
        });
    }

    public getSearchList(): Array<TSAbstractAntragDTO> {
        return this.antragList;
    }


}
