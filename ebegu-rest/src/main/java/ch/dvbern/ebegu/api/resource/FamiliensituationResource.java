package ch.dvbern.ebegu.api.resource;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import ch.dvbern.ebegu.api.converter.JaxBConverter;
import ch.dvbern.ebegu.api.dtos.JaxFamiliensituationContainer;
import ch.dvbern.ebegu.api.dtos.JaxId;
import ch.dvbern.ebegu.api.resource.util.ResourceHelper;
import ch.dvbern.ebegu.entities.Familiensituation;
import ch.dvbern.ebegu.entities.FamiliensituationContainer;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.enums.ErrorCodeEnum;
import ch.dvbern.ebegu.errors.EbeguEntityNotFoundException;
import ch.dvbern.ebegu.errors.EbeguException;
import ch.dvbern.ebegu.services.FamiliensituationService;
import ch.dvbern.ebegu.services.GesuchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Resource fuer Familiensituation
 */
@Path("familiensituation")
@Stateless
@Api(description = "Resource für die Familiensituation")
public class FamiliensituationResource {

	@Inject
	private FamiliensituationService familiensituationService;
	@Inject
	private GesuchService gesuchService;

	@Inject
	private JaxBConverter converter;

	@Inject
	private ResourceHelper resourceHelper;

	@ApiOperation(value = "Speichert eine Familiensituation in der Datenbank", response = JaxFamiliensituationContainer.class)
	@Nullable
	@PUT
	@Path("/{gesuchId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public JaxFamiliensituationContainer saveFamiliensituation(
		@Nonnull @NotNull @PathParam("gesuchId") JaxId gesuchJAXPId,
		@Nonnull @NotNull JaxFamiliensituationContainer familiensituationContainerJAXP,
		@Context UriInfo uriInfo,
		@Context HttpServletResponse response) throws EbeguException {

		Gesuch gesuch = gesuchService.findGesuch(gesuchJAXPId.getId()).orElseThrow(() -> new EbeguEntityNotFoundException("saveFamiliensituation", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, gesuchJAXPId.getId()));

		// Sicherstellen, dass das dazugehoerige Gesuch ueberhaupt noch editiert werden darf fuer meine Rolle
		resourceHelper.assertGesuchStatusForBenutzerRole(gesuch);

		FamiliensituationContainer familiensituationContainerToMerge = new FamiliensituationContainer();
		//wenn es sich um ein update handelt
		Familiensituation oldFamiliensituation = null;
		if (familiensituationContainerJAXP.getId() != null) {
			Optional<FamiliensituationContainer> loadedFamiliensituation = this.familiensituationService.findFamiliensituation(familiensituationContainerJAXP.getId());
			if (loadedFamiliensituation.isPresent()) {
				familiensituationContainerToMerge = loadedFamiliensituation.get();
				oldFamiliensituation = new Familiensituation(familiensituationContainerToMerge.extractFamiliensituation());
			} else {
				familiensituationContainerToMerge = new FamiliensituationContainer();
			}
		}

		FamiliensituationContainer convertedFamiliensituation = converter.familiensituationContainerToEntity(familiensituationContainerJAXP, familiensituationContainerToMerge);
		FamiliensituationContainer persistedFamiliensituation = this.familiensituationService.saveFamiliensituation(gesuch, convertedFamiliensituation, oldFamiliensituation);

		return converter.familiensituationContainerToJAX(persistedFamiliensituation);
	}
}
