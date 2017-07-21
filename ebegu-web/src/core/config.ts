import 'angular';
import 'angular-translate';
import 'angular-unsavedchanges';
import 'angular-hotkeys';
import HttpErrorInterceptor from './errors/service/HttpErrorInterceptor';
import HttpAuthInterceptor from '../authentication/service/HttpAuthInterceptor';
import HttpResponseInterceptor from './service/HttpResponseInterceptor';
import ITranslateProvider = angular.translate.ITranslateProvider;

import IInjectorService = angular.auto.IInjectorService;
import IThemingProvider = angular.material.IThemingProvider;
import IHttpProvider = angular.IHttpProvider;
import ILogProvider = angular.ILogProvider;
import ILocationProvider = angular.ILocationProvider;

configure.$inject = ['$translateProvider', '$injector', '$httpProvider', '$mdThemingProvider', 'hotkeysProvider', '$logProvider', '$locationProvider', '$qProvider'];
/* @ngInject */
export function configure($translateProvider: ITranslateProvider, $injector: IInjectorService, $httpProvider: IHttpProvider,
                          $mdThemingProvider: IThemingProvider, hotkeysProvider: any, $logProvider: ILogProvider,
                          $locationProvider: ILocationProvider, $qProvider: any) {
    //Translation Provider configuration
    let translProp = require('../assets/translations/translations_de.json');

    // In case you have issues with double-escaped parameters, check out this issue: https://github.com/angular-translate/angular-translate/issues/1101
    $translateProvider.useSanitizeValueStrategy('escapeParameters');

    $translateProvider
        .translations('de', translProp)
        .fallbackLanguage('de')
        .preferredLanguage('de');

    //Dirty Check configuration (nur wenn plugin vorhanden)
    if ($injector.has('unsavedWarningsConfigProvider')) {
        let unsavedWarningsConfigProvider: any = $injector.get('unsavedWarningsConfigProvider');
        unsavedWarningsConfigProvider.useTranslateService = true;
        unsavedWarningsConfigProvider.logEnabled = false;
        unsavedWarningsConfigProvider.navigateMessage = 'UNSAVED_WARNING';
        unsavedWarningsConfigProvider.reloadMessage = 'UNSAVED_WARNING_RELOAD';
    }
    //Config Angular Module Theme
    $mdThemingProvider.theme('default')
    // .primaryPalette('red')
        .accentPalette('red');

    //Config hotkey provider: https://github.com/chieffancypants/angular-hotkeys#angular-hotkeys-
    hotkeysProvider.useNgRoute = false;
    hotkeysProvider.includeCheatSheet = false;

    //Configuration of $http service
    $httpProvider.interceptors.push('HttpErrorInterceptor');
    $httpProvider.interceptors.push('HttpAuthInterceptor');
    $httpProvider.interceptors.push('HttpResponseInterceptor');
    $httpProvider.interceptors.push('HttpVersionInterceptor');

    $locationProvider.hashPrefix('');


    // disable debug log messages in production
    if (ENV === 'production') {
        $logProvider.debugEnabled(false);
    }

    // Disable "Possibly unhandled rejection:" from angular
    $qProvider.errorOnUnhandledRejections(false);
}
