import EbeguRestUtil from '../../utils/EbeguRestUtil';
import {IHttpPromise, IHttpService} from 'angular';

export class ReindexRS {
    serviceURL: string;
    http: IHttpService;
    ebeguRestUtil: EbeguRestUtil;

    static $inject = ['$http', 'REST_API', 'EbeguRestUtil'];
    /* @ngInject */
    constructor($http: IHttpService, REST_API: string, ebeguRestUtil: EbeguRestUtil) {
        this.serviceURL = REST_API + 'admin/reindex';
        this.http = $http;
        this.ebeguRestUtil = ebeguRestUtil;
    }

    reindex(): IHttpPromise<any> {
        return this.http.get(this.serviceURL + '/' );
    }

}

