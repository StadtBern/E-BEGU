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

package ch.dvbern.ebegu.api.resource;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import ch.dvbern.ebegu.api.converter.JaxBConverter;
import ch.dvbern.ebegu.api.dtos.JaxAntragStatusHistory;
import ch.dvbern.ebegu.api.dtos.JaxId;
import ch.dvbern.ebegu.entities.AntragStatusHistory;
import ch.dvbern.ebegu.entities.Fall;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.entities.Gesuchsperiode;
import ch.dvbern.ebegu.enums.ErrorCodeEnum;
import ch.dvbern.ebegu.errors.EbeguEntityNotFoundException;
import ch.dvbern.ebegu.errors.EbeguException;
import ch.dvbern.ebegu.services.AntragStatusHistoryService;
import ch.dvbern.ebegu.services.FallService;
import ch.dvbern.ebegu.services.GesuchService;
import ch.dvbern.ebegu.services.GesuchsperiodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.Validate;

/**
 * REST Resource fuer die History von Gesuchen/Mutationen (Antraegen)
 */
@Path("antragStatusHistory")
@Stateless
@Api(description = "Resource zum Lesen der History von Gesuchen / Mutationen (Anträgen)")
public class AntragStatusHistoryResource {

	@Inject
	private JaxBConverter converter;
	@Inject
	private GesuchService gesuchService;
	@Inject
	private GesuchsperiodeService gesuchsperiodeService;
	@Inject
	private FallService fallService;
	@Inject
	private AntragStatusHistoryService antragStatusHistoryService;

	@ApiOperation(value = "Ermittelt den letzten Statusübergang des Antrags mit der übergebenen Id.", response = JaxAntragStatusHistory.class)
	@Nullable
	@GET
	@Path("/{gesuchId}")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public JaxAntragStatusHistory findLastStatusChange(
		@Nonnull @NotNull @PathParam("gesuchId") JaxId jaxGesuchId) throws EbeguException {

		Validate.notNull(jaxGesuchId.getId());
		String gesuchId = converter.toEntityId(jaxGesuchId);
		Optional<Gesuch> gesuch = gesuchService.findGesuch(gesuchId);

		if (gesuch.isPresent()) {
			final AntragStatusHistory lastStatusChange = antragStatusHistoryService.findLastStatusChange(gesuch.get());
			if (lastStatusChange != null) {
				return converter.antragStatusHistoryToJAX(lastStatusChange);
			}
		}
		return null;
	}

	@ApiOperation(value = "Ermittelt alle Statusübergänge des Antrags mit der übergebenen Id.", response = Collection.class)
	@Nullable
	@GET
	@Path("/verlauf/{gesuchsperiodeId}/{fallId}")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<JaxAntragStatusHistory> findAllAntragStatusHistoryByGesuch(
		@Nonnull @NotNull @PathParam("gesuchsperiodeId") JaxId jaxGesuchsperiodeId,
		@Nonnull @NotNull @PathParam("fallId") JaxId jaxFallId) throws EbeguException {

		Validate.notNull(jaxGesuchsperiodeId.getId());
		String gesuchsperiodeId = converter.toEntityId(jaxGesuchsperiodeId);
		Validate.notNull(jaxFallId.getId());
		String fallId = converter.toEntityId(jaxFallId);

		Optional<Gesuchsperiode> gesuchsperiode = gesuchsperiodeService.findGesuchsperiode(gesuchsperiodeId);
		if (!gesuchsperiode.isPresent()) {
			throw new EbeguEntityNotFoundException("findAllAntragStatusHistoryByGesuch", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, jaxGesuchsperiodeId.getId());
		}
		Optional<Fall> fall = fallService.findFall(fallId);
		if (!fall.isPresent()) {
			throw new EbeguEntityNotFoundException("findAllAntragStatusHistoryByGesuch", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, jaxFallId.getId());
		}

		final Collection<AntragStatusHistory> statusHistory = antragStatusHistoryService.findAllAntragStatusHistoryByGPFall(gesuchsperiode.get(), fall.get());
		return converter.antragStatusHistoryCollectionToJAX(statusHistory);
	}
}
