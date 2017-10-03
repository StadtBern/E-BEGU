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

import {TSAbstractDateRangedEntity} from './TSAbstractDateRangedEntity';
import {TSDateRange} from './types/TSDateRange';
import {TSVerfuegungZeitabschnittZahlungsstatus} from './enums/TSVerfuegungZeitabschnittZahlungsstatus';

export default class TSVerfuegungZeitabschnitt extends TSAbstractDateRangedEntity {

    private _erwerbspensumGS1: number;
    private _erwerbspensumGS2: number;
    private _betreuungspensum: number;
    private _fachstellenpensum: number;
    private _anspruchspensumRest: number;
    private _anspruchberechtigtesPensum: number;
    private _bgPensum: number;
    private _betreuungsstunden: number;
    private _vollkosten: number;
    private _elternbeitrag: number;
    private _abzugFamGroesse: number;
    private _famGroesse: number;
    private _massgebendesEinkommenVorAbzugFamgr: number;
    private _bemerkungen: string;
    private _zahlungsstatus: TSVerfuegungZeitabschnittZahlungsstatus;
    private _einkommensjahr: number;
    private _kategorieMaxEinkommen: boolean;
    private _kategorieKeinPensum: boolean;
    private _kategorieZuschlagZumErwerbspensum: boolean;
    private _zuSpaetEingereicht: boolean;
    private _sameVerfuegungsdaten: boolean;
    private _sameVerguenstigung: boolean;


    constructor(erwerbspensumGS1?: number, erwerbspensumGS2?: number, betreuungspensum?: number, fachstellenpensum?: number,
                anspruchspensumRest?: number, anspruchberechtigtesPensum?: number, bgPensum?: number, betreuungsstunden?: number, vollkosten?: number,
                elternbeitrag?: number, abzugFamGroesse?: number, massgebendesEinkommen?: number, bemerkungen?: string,
                zahlungsstatus?: TSVerfuegungZeitabschnittZahlungsstatus, gueltigkeit?: TSDateRange, famGroesse?: number, einkommensjahr?: number, kategorieMaxEinkommen?: boolean,
                kategorieKeinPensum?: boolean, kategorieZuschlagZumErwerbspensum?: boolean, sameVerfuegungsdaten?: boolean,
                sameVerguenstigung?: boolean) {
        super(gueltigkeit);
        this._erwerbspensumGS1 = erwerbspensumGS1;
        this._erwerbspensumGS2 = erwerbspensumGS2;
        this._betreuungspensum = betreuungspensum;
        this._fachstellenpensum = fachstellenpensum;
        this._anspruchspensumRest = anspruchspensumRest;
        this._anspruchberechtigtesPensum = anspruchberechtigtesPensum;
        this._bgPensum = bgPensum;
        this._betreuungsstunden = betreuungsstunden;
        this._vollkosten = vollkosten;
        this._elternbeitrag = elternbeitrag;
        this._abzugFamGroesse = abzugFamGroesse;
        this._massgebendesEinkommenVorAbzugFamgr = massgebendesEinkommen;
        this._bemerkungen = bemerkungen;
        this._zahlungsstatus = zahlungsstatus;
        this._famGroesse = famGroesse;
        this._einkommensjahr = einkommensjahr;
        this._kategorieMaxEinkommen = kategorieMaxEinkommen;
        this._kategorieKeinPensum = kategorieKeinPensum;
        this._kategorieZuschlagZumErwerbspensum = kategorieZuschlagZumErwerbspensum;
        this._sameVerfuegungsdaten = sameVerfuegungsdaten;
        this._sameVerguenstigung = sameVerguenstigung;
    }

    get erwerbspensumGS1(): number {
        return this._erwerbspensumGS1;
    }

    set erwerbspensumGS1(value: number) {
        this._erwerbspensumGS1 = value;
    }

    get erwerbspensumGS2(): number {
        return this._erwerbspensumGS2;
    }

    set erwerbspensumGS2(value: number) {
        this._erwerbspensumGS2 = value;
    }

    get betreuungspensum(): number {
        return this._betreuungspensum;
    }

    set betreuungspensum(value: number) {
        this._betreuungspensum = value;
    }

    get fachstellenpensum(): number {
        return this._fachstellenpensum;
    }

    set fachstellenpensum(value: number) {
        this._fachstellenpensum = value;
    }

    get anspruchspensumRest(): number {
        return this._anspruchspensumRest;
    }

    set anspruchspensumRest(value: number) {
        this._anspruchspensumRest = value;
    }

    get anspruchberechtigtesPensum(): number {
        return this._anspruchberechtigtesPensum;
    }

    set anspruchberechtigtesPensum(value: number) {
        this._anspruchberechtigtesPensum = value;
    }

    get bgPensum(): number {
        return this._bgPensum;
    }

    set bgPensum(value: number) {
        this._bgPensum = value;
    }

    get betreuungsstunden(): number {
        return this._betreuungsstunden;
    }

    set betreuungsstunden(value: number) {
        this._betreuungsstunden = value;
    }

    get vollkosten(): number {
        return this._vollkosten;
    }

    set vollkosten(value: number) {
        this._vollkosten = value;
    }

    get elternbeitrag(): number {
        return this._elternbeitrag;
    }

    set elternbeitrag(value: number) {
        this._elternbeitrag = value;
    }

    get abzugFamGroesse(): number {
        return this._abzugFamGroesse;
    }

    set abzugFamGroesse(value: number) {
        this._abzugFamGroesse = value;
    }

    get massgebendesEinkommenVorAbzugFamgr(): number {
        return this._massgebendesEinkommenVorAbzugFamgr;
    }

    set massgebendesEinkommenVorAbzugFamgr(value: number) {
        this._massgebendesEinkommenVorAbzugFamgr = value;
    }

    get bemerkungen(): string {
        return this._bemerkungen;
    }

    set bemerkungen(value: string) {
        this._bemerkungen = value;
    }

    get zahlungsstatus(): TSVerfuegungZeitabschnittZahlungsstatus {
        return this._zahlungsstatus;
    }

    set zahlungsstatus(value: TSVerfuegungZeitabschnittZahlungsstatus) {
        this._zahlungsstatus = value;
    }

    get famGroesse(): number {
        return this._famGroesse;
    }

    set famGroesse(value: number) {
        this._famGroesse = value;
    }

    get einkommensjahr(): number {
        return this._einkommensjahr;
    }

    set einkommensjahr(value: number) {
        this._einkommensjahr = value;
    }

    get kategorieMaxEinkommen(): boolean {
        return this._kategorieMaxEinkommen;
    }

    set kategorieMaxEinkommen(value: boolean) {
        this._kategorieMaxEinkommen = value;
    }

    get kategorieKeinPensum(): boolean {
        return this._kategorieKeinPensum;
    }

    set kategorieKeinPensum(value: boolean) {
        this._kategorieKeinPensum = value;
    }

    get kategorieZuschlagZumErwerbspensum(): boolean {
        return this._kategorieZuschlagZumErwerbspensum;
    }

    set kategorieZuschlagZumErwerbspensum(value: boolean) {
        this._kategorieZuschlagZumErwerbspensum = value;
    }

    get zuSpaetEingereicht(): boolean {
        return this._zuSpaetEingereicht;
    }

    set zuSpaetEingereicht(value: boolean) {
        this._zuSpaetEingereicht = value;
    }

    get sameVerfuegungsdaten(): boolean {
        return this._sameVerfuegungsdaten;
    }

    set sameVerfuegungsdaten(value: boolean) {
        this._sameVerfuegungsdaten = value;
    }

    get sameVerguenstigung(): boolean {
        return this._sameVerguenstigung;
    }

    set sameVerguenstigung(value: boolean) {
        this._sameVerguenstigung = value;
    }
}
