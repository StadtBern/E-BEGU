package ch.dvbern.ebegu.services;

import ch.dvbern.ebegu.entities.Traegerschaft;
import ch.dvbern.ebegu.enums.ErrorCodeEnum;
import ch.dvbern.ebegu.errors.EbeguEntityNotFoundException;
import ch.dvbern.ebegu.persistence.CriteriaQueryHelper;
import ch.dvbern.lib.cdipersistence.Persistence;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Service fuer Traegerschaft
 */
@Stateless
@Local(TraegerschaftService.class)
public class TraegerschaftServiceBean extends AbstractBaseService implements TraegerschaftService {

	@Inject
	private Persistence<Traegerschaft> persistence;

	@Inject
	private CriteriaQueryHelper criteriaQueryHelper;

	@Nonnull
	@Override
	public Traegerschaft saveTraegerschaft(@Nonnull Traegerschaft traegerschaft) {
		Objects.requireNonNull(traegerschaft);
		return persistence.merge(traegerschaft);
	}

	@Nonnull
	@Override
	public Optional<Traegerschaft> findTraegerschaft(@Nonnull final String traegerschaftId) {
		Objects.requireNonNull(traegerschaftId, "id muss gesetzt sein");
		Traegerschaft a =  persistence.find(Traegerschaft.class, traegerschaftId);
		return Optional.ofNullable(a);
	}

	@Override
	@Nonnull
	public Collection<Traegerschaft> getAllTraegerschaften() {
		return new ArrayList<>(criteriaQueryHelper.getAll(Traegerschaft.class));
	}

	@Override
	public void removeTraegerschaft(@Nonnull String traegerschaftId) {
		Validate.notNull(traegerschaftId);
		Optional<Traegerschaft> traegerschaftToRemove = findTraegerschaft(traegerschaftId);
		traegerschaftToRemove.orElseThrow(() -> new EbeguEntityNotFoundException("removeTraegerschaft", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, traegerschaftId));
		persistence.remove(traegerschaftToRemove.get());
	}

}