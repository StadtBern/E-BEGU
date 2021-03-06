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

package ch.dvbern.ebegu.api.dtos;

import java.math.BigDecimal;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO fuer Familiensituationen
 */
@XmlRootElement(name = "einkommensverschlechterung")
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxEinkommensverschlechterung extends JaxAbstractFinanzielleSituation {

	private static final long serialVersionUID = 3659631207762053261L;

	@Nullable
	private BigDecimal nettolohnJan;

	@Nullable
	private BigDecimal nettolohnFeb;

	@Nullable
	private BigDecimal nettolohnMrz;

	@Nullable
	private BigDecimal nettolohnApr;

	@Nullable
	private BigDecimal nettolohnMai;

	@Nullable
	private BigDecimal nettolohnJun;

	@Nullable
	private BigDecimal nettolohnJul;

	@Nullable
	private BigDecimal nettolohnAug;

	@Nullable
	private BigDecimal nettolohnSep;

	@Nullable
	private BigDecimal nettolohnOkt;

	@Nullable
	private BigDecimal nettolohnNov;

	@Nullable
	private BigDecimal nettolohnDez;

	@Nullable
	private BigDecimal nettolohnZus;

	@Nullable
	private BigDecimal geschaeftsgewinnBasisjahrMinus1;

	@Nullable
	public BigDecimal getNettolohnJan() {
		return nettolohnJan;
	}

	public void setNettolohnJan(@Nullable final BigDecimal nettolohnJan) {
		this.nettolohnJan = nettolohnJan;
	}

	@Nullable
	public BigDecimal getNettolohnFeb() {
		return nettolohnFeb;
	}

	public void setNettolohnFeb(@Nullable final BigDecimal nettolohnFeb) {
		this.nettolohnFeb = nettolohnFeb;
	}

	@Nullable
	public BigDecimal getNettolohnMrz() {
		return nettolohnMrz;
	}

	public void setNettolohnMrz(@Nullable final BigDecimal nettolohnMrz) {
		this.nettolohnMrz = nettolohnMrz;
	}

	@Nullable
	public BigDecimal getNettolohnApr() {
		return nettolohnApr;
	}

	public void setNettolohnApr(@Nullable final BigDecimal nettolohnApr) {
		this.nettolohnApr = nettolohnApr;
	}

	@Nullable
	public BigDecimal getNettolohnMai() {
		return nettolohnMai;
	}

	public void setNettolohnMai(@Nullable final BigDecimal nettolohnMai) {
		this.nettolohnMai = nettolohnMai;
	}

	@Nullable
	public BigDecimal getNettolohnJun() {
		return nettolohnJun;
	}

	public void setNettolohnJun(@Nullable final BigDecimal nettolohnJun) {
		this.nettolohnJun = nettolohnJun;
	}

	@Nullable
	public BigDecimal getNettolohnJul() {
		return nettolohnJul;
	}

	public void setNettolohnJul(@Nullable final BigDecimal nettolohnJul) {
		this.nettolohnJul = nettolohnJul;
	}

	@Nullable
	public BigDecimal getNettolohnAug() {
		return nettolohnAug;
	}

	public void setNettolohnAug(@Nullable final BigDecimal nettolohnAug) {
		this.nettolohnAug = nettolohnAug;
	}

	@Nullable
	public BigDecimal getNettolohnSep() {
		return nettolohnSep;
	}

	public void setNettolohnSep(@Nullable final BigDecimal nettolohnSep) {
		this.nettolohnSep = nettolohnSep;
	}

	@Nullable
	public BigDecimal getNettolohnOkt() {
		return nettolohnOkt;
	}

	public void setNettolohnOkt(@Nullable final BigDecimal nettolohnOkt) {
		this.nettolohnOkt = nettolohnOkt;
	}

	@Nullable
	public BigDecimal getNettolohnNov() {
		return nettolohnNov;
	}

	public void setNettolohnNov(@Nullable final BigDecimal nettolohnNov) {
		this.nettolohnNov = nettolohnNov;
	}

	@Nullable
	public BigDecimal getNettolohnDez() {
		return nettolohnDez;
	}

	public void setNettolohnDez(@Nullable final BigDecimal nettolohnDez) {
		this.nettolohnDez = nettolohnDez;
	}

	@Nullable
	public BigDecimal getNettolohnZus() {
		return nettolohnZus;
	}

	public void setNettolohnZus(@Nullable BigDecimal nettolohnZus) {
		this.nettolohnZus = nettolohnZus;
	}

	@Nullable
	public BigDecimal getGeschaeftsgewinnBasisjahrMinus1() {
		return geschaeftsgewinnBasisjahrMinus1;
	}

	public void setGeschaeftsgewinnBasisjahrMinus1(@Nullable BigDecimal geschaeftsgewinnBasisjahrMinus1) {
		this.geschaeftsgewinnBasisjahrMinus1 = geschaeftsgewinnBasisjahrMinus1;
	}
}
