package ch.dvbern.ebegu.api.resource;

import ch.dvbern.ebegu.api.converter.JaxBConverter;
import ch.dvbern.ebegu.api.dtos.JaxId;
import ch.dvbern.ebegu.api.dtos.JaxMandant;
import ch.dvbern.ebegu.entities.Mandant;
import ch.dvbern.ebegu.errors.EbeguException;
import ch.dvbern.ebegu.services.MandantService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Optional;

/**
 * REST Resource fuer Mandanten
 */
@Path("mandanten")
@Stateless
@Api(description = "Resource für Mandanten")
public class MandantResource {

	@Inject
	private MandantService mandantService;

	@Inject
	private JaxBConverter converter;

	@ApiOperation(value = "Gibt den Mandanten mit der angegebenen id zurueck.", response = JaxMandant.class)
	@Nullable
	@GET
	@Path("/id/{mandantId}")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public JaxMandant findMandant(@Nonnull @NotNull @PathParam("mandantId") JaxId mandantJAXPId) throws EbeguException {
		Validate.notNull(mandantJAXPId.getId());
		String mandantID = converter.toEntityId(mandantJAXPId);
		Optional<Mandant> optional = mandantService.findMandant(mandantID);

		if (!optional.isPresent()) {
			return null;
		}
		return converter.mandantToJAX(optional.get());
	}

	@ApiOperation(value = "Gibt den ersten Mandanten aus der Datenbank zurueck. Convenience-Methode, da im Moment " +
		"nur ein Mandant vorhanden ist.", response = JaxMandant.class)
	@Nullable
	@GET
	@Path("/first")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public JaxMandant getFirst() throws EbeguException {

		Mandant mandant = mandantService.getFirst();
		return converter.mandantToJAX(mandant);
	}

}
