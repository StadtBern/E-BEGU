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

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO fuer Erwerbspensum Container
 */
@XmlRootElement(name = "erwerbspensumcontainer")
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxErwerbspensumContainer extends JaxAbstractDTO {

	private static final long serialVersionUID = 4879926292956257345L;

	@Valid
	private JaxErwerbspensum erwerbspensumGS;

	@Valid
	private JaxErwerbspensum erwerbspensumJA;

	@Nullable
	public JaxErwerbspensum getErwerbspensumJA() {
		return erwerbspensumJA;
	}

	public void setErwerbspensumJA(@Nullable JaxErwerbspensum erwerbspensumJA) {
		this.erwerbspensumJA = erwerbspensumJA;
	}

	@Nullable
	public JaxErwerbspensum getErwerbspensumGS() {
		return erwerbspensumGS;
	}

	public void setErwerbspensumGS(@Nullable JaxErwerbspensum erwerbspensumGS) {
		this.erwerbspensumGS = erwerbspensumGS;
	}
}
