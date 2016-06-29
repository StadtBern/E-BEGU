package ch.dvbern.ebegu.services;

import ch.dvbern.ebegu.dto.FinanzielleSituationResultateDTO;
import ch.dvbern.ebegu.entities.FinanzielleSituationContainer;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.enums.ErrorCodeEnum;
import ch.dvbern.ebegu.errors.EbeguEntityNotFoundException;
import ch.dvbern.ebegu.persistence.CriteriaQueryHelper;
import ch.dvbern.ebegu.util.FinanzielleSituationRechner;
import ch.dvbern.lib.cdipersistence.Persistence;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Service fuer FinanzielleSituation
 */
@Stateless
@Local(FinanzielleSituationService.class)
public class FinanzielleSituationServiceBean extends AbstractBaseService implements FinanzielleSituationService {

	@Inject
	private Persistence<FinanzielleSituationContainer> persistence;

	@Inject
	private CriteriaQueryHelper criteriaQueryHelper;

	@Inject
	private FinanzielleSituationRechner finSitRechner;

	@Nonnull
	@Override
	public FinanzielleSituationContainer saveFinanzielleSituation(@Nonnull FinanzielleSituationContainer finanzielleSituation) {
		Objects.requireNonNull(finanzielleSituation);
		return persistence.merge(finanzielleSituation);
	}

	@Nonnull
	@Override
	public Optional<FinanzielleSituationContainer> findFinanzielleSituation(@Nonnull String id) {
		Objects.requireNonNull(id, "id muss gesetzt sein");
		FinanzielleSituationContainer finanzielleSituation =  persistence.find(FinanzielleSituationContainer.class, id);
		return Optional.ofNullable(finanzielleSituation);
	}

	@Nonnull
	@Override
	public Collection<FinanzielleSituationContainer> getAllFinanzielleSituationen() {
		return new ArrayList<>(criteriaQueryHelper.getAll(FinanzielleSituationContainer.class));
	}

	@Override
	public void removeFinanzielleSituation(@Nonnull FinanzielleSituationContainer finanzielleSituation) {
		Validate.notNull(finanzielleSituation);
		finanzielleSituation.getGesuchsteller().setFinanzielleSituationContainer(null);
		persistence.merge(finanzielleSituation.getGesuchsteller());

		Optional<FinanzielleSituationContainer> propertyToRemove = findFinanzielleSituation(finanzielleSituation.getId());
		propertyToRemove.orElseThrow(() -> new EbeguEntityNotFoundException("removeFinanzielleSituation", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, finanzielleSituation));
		persistence.remove(FinanzielleSituationContainer.class, propertyToRemove.get().getId());
	}

	@Override
	@Nonnull
	public FinanzielleSituationResultateDTO calculateResultate(@Nonnull Gesuch gesuch) {
		if (gesuch.getGesuchsperiode() != null) {
			double familiengroesse = finSitRechner.calculateFamiliengroesse(gesuch, gesuch.getGesuchsperiode().getGueltigkeit().calculateEndOfPreviousYear());
			BigDecimal abzugAufgrundFamiliengroesse = finSitRechner
				.calculateAbzugAufgrundFamiliengroesse(gesuch.getGesuchsperiode().getGueltigkeit().getGueltigAb(), familiengroesse);
			return new FinanzielleSituationResultateDTO(gesuch, familiengroesse, abzugAufgrundFamiliengroesse);
		}
		return new FinanzielleSituationResultateDTO(gesuch, 0, BigDecimal.ZERO);
	}

}
