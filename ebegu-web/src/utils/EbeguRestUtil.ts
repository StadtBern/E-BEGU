import TSApplicationProperty from "../models/TSApplicationProperty";
import TSAbstractEntity from "../models/TSAbstractEntity";
import TSAdresse from "../models/TSAdresse";
import {TSAdressetyp} from "../models/enums/TSAdressetyp";
import TSPerson from "../models/TSPerson";
import DateUtil from "./DateUtil";
import {IFilterService} from "angular";
import TSLand from "../models/TSLand";

export default class EbeguRestUtil {
    static $inject = ['$filter'];

    private filter: IFilterService;

    /* @ngInject */
    constructor($filter: IFilterService) {
        this.filter = $filter;
    }

    /**
     * Wandelt Data in einen TSApplicationProperty Array um, welches danach zurueckgeliefert wird
     * @param data
     * @returns {TSApplicationProperty[]}
     */
    public parseApplicationProperties(data: any): TSApplicationProperty[] {
        var appProperties: TSApplicationProperty[] = [];
        if (data !== null && Array.isArray(data)) {
            for (var i = 0; i < data.length; i++) {
                appProperties[i] = this.parseApplicationProperty(new TSApplicationProperty('', ''), data[i]);
            }
        } else {
            appProperties[0] = this.parseApplicationProperty(new TSApplicationProperty('', ''), data);
        }
        console.log('parsed application properites', appProperties);
        return appProperties;
    }

    /**
     * Wandelt die receivedAppProperty in einem parsedAppProperty um.
     * @param parsedAppProperty
     * @param receivedAppProperty
     * @returns {TSApplicationProperty}
     */
    public parseApplicationProperty(parsedAppProperty: TSApplicationProperty, receivedAppProperty: any): TSApplicationProperty {
        parsedAppProperty.name = receivedAppProperty.name;
        parsedAppProperty.value = receivedAppProperty.value;
        this.parseAbstractEntity(parsedAppProperty, receivedAppProperty);
        return parsedAppProperty;
    }

    public adresseToRestObject(restAdresse: any, adresse: TSAdresse): TSAdresse {
        if (adresse) {
            this.abstractEntityToRestObject(restAdresse, adresse);
            restAdresse.strasse = adresse.strasse;
            restAdresse.hausnummer = adresse.hausnummer;
            restAdresse.zusatzzeile = adresse.zusatzzeile;
            restAdresse.plz = adresse.plz;
            restAdresse.ort = adresse.ort;
            restAdresse.land = adresse.land;
            restAdresse.gemeinde = adresse.gemeinde;
            restAdresse.gueltigAb = DateUtil.momentToLocalDate(adresse.gueltigAb);
            restAdresse.gueltigBis = DateUtil.momentToLocalDate(adresse.gueltigBis);
            restAdresse.adresseTyp = TSAdressetyp[adresse.adresseTyp];
            return restAdresse;
        }
        return undefined;

    }

    public parseAdresse(adresseTS: TSAdresse, receivedAdresse: any): TSAdresse {
        if (receivedAdresse) {
            this.abstractEntityToRestObject(adresseTS, receivedAdresse);
            adresseTS.strasse = receivedAdresse.strasse;
            adresseTS.hausnummer = receivedAdresse.hausnummer;
            adresseTS.zusatzzeile = receivedAdresse.zusatzzeile;
            adresseTS.plz = receivedAdresse.plz;
            adresseTS.ort = receivedAdresse.ort;
            adresseTS.land = this.landCodeToTSLandCode(receivedAdresse.land); //this.landCodeToTSLand(receivedAdresse.land);
            adresseTS.gemeinde = receivedAdresse.gemeinde;
            adresseTS.gueltigAb = DateUtil.localDateToMoment(receivedAdresse.gueltigAb);
            adresseTS.gueltigBis = DateUtil.localDateToMoment(receivedAdresse.gueltigBis);
            adresseTS.adresseTyp = receivedAdresse.adresseTyp;
            return adresseTS;
        }
        return undefined;
    }

    /**
     * Nimmt den eingegebenen Code und erzeugt ein TSLand Objekt mit dem Code und
     * seine Uebersetzung.
     * @param landCode
     * @returns {TSLand}
     */
    public landCodeToTSLand(landCode: string): TSLand {
        if (landCode) {
            let parsedLandCode = this.landCodeToTSLandCode(landCode);
            return new TSLand(landCode, this.filter('translate')(parsedLandCode).toString());
        }
        return undefined;
    }

    /**
     * Fügt das 'Land_' dem eingegebenen Landcode hinzu.
     * @param landCode
     * @returns {string}
     */
    public landCodeToTSLandCode(landCode: string): string {
        if (landCode) {
            if (landCode.lastIndexOf('Land_', 0) !== 0) {
                return 'Land_' + landCode;
            }
        }
        return undefined;
    }


    public personToRestObject(restPerson: any, person: TSPerson): any {
        if (person) {
            this.abstractEntityToRestObject(restPerson, person);

            restPerson.vorname = person.vorname;
            restPerson.nachname = person.nachname;
            restPerson.geburtsdatum = DateUtil.momentToLocalDate(person.geburtsdatum);
            restPerson.mail = person.mail;
            restPerson.mobile = person.mobile;
            restPerson.telefon = person.telefon;
            restPerson.telefonAusland = person.telefonAusland;
            restPerson.umzug = person.umzug;
            restPerson.geschlecht = person.geschlecht;
            restPerson.wohnAdresse = this.adresseToRestObject({}, person.adresse);
            restPerson.alternativeAdresse = this.adresseToRestObject({}, person.korrespondenzAdresse);
            restPerson.umzugAdresse = this.adresseToRestObject({}, person.umzugAdresse);
            return restPerson;
        }
        return undefined;
    }

    public parsePerson(personTS: TSPerson, personFromServer: any): TSPerson {
        if (personFromServer) {

            this.parseAbstractEntity(personTS, personFromServer);
            personTS.vorname = personFromServer.vorname;
            personTS.nachname = personFromServer.nachname;
            personTS.geburtsdatum = DateUtil.localDateTimeToMoment(personFromServer.geburtsdatum);
            personTS.mail = personFromServer.mail;
            personTS.mobile = personFromServer.mobile;
            personTS.telefon = personFromServer.telefon;
            personTS.telefonAusland = personFromServer.telefonAusland;
            personTS.umzug = personFromServer.umzug;
            personTS.geschlecht = personFromServer.geschlecht;
            personTS.adresse = this.parseAdresse(new TSAdresse(), personFromServer.wohnAdresse);
            personTS.korrespondenzAdresse = this.parseAdresse(new TSAdresse(), personFromServer.alternativeAdresse);
            personTS.umzugAdresse = this.parseAdresse(new TSAdresse(), personFromServer.umzugAdresse);
            return personTS;
        }
        return undefined;
    }

    private parseAbstractEntity(parsedAppProperty: TSAbstractEntity, receivedAppProperty: any) {
        parsedAppProperty.timestampErstellt = DateUtil.localDateTimeToMoment(receivedAppProperty.timestampErstellt);
        parsedAppProperty.timestampMutiert = DateUtil.localDateTimeToMoment(receivedAppProperty.timestampMutiert);
        parsedAppProperty.id = receivedAppProperty.id;
    }

    private abstractEntityToRestObject(restObject: any, typescriptObject: TSAbstractEntity) {
        restObject.id = typescriptObject.id;
        restObject.timestampErstellt = DateUtil.momentToLocalDateTime(typescriptObject.timestampErstellt);
        restObject.timestampMutiert = DateUtil.momentToLocalDateTime(typescriptObject.timestampMutiert);
    }
}