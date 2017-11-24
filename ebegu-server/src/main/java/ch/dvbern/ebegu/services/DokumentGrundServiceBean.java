package ch.dvbern.ebegu.services;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.dvbern.ebegu.authentication.PrincipalBean;
import ch.dvbern.ebegu.entities.DokumentGrund;
import ch.dvbern.ebegu.entities.DokumentGrund_;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.enums.DokumentGrundTyp;
import ch.dvbern.ebegu.enums.UserRole;
import ch.dvbern.ebegu.enums.WizardStepName;
import ch.dvbern.ebegu.persistence.CriteriaQueryHelper;
import ch.dvbern.lib.cdipersistence.Persistence;

import static ch.dvbern.ebegu.enums.UserRoleName.ADMIN;
import static ch.dvbern.ebegu.enums.UserRoleName.SUPER_ADMIN;

/**
 * Service fuer Kind
 */
@Stateless
@Local(DokumentGrundService.class)
@PermitAll
public class DokumentGrundServiceBean extends AbstractBaseService implements DokumentGrundService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DokumentGrundServiceBean.class.getSimpleName());

	@Inject
	private Persistence persistence;

	@Inject
	private WizardStepService wizardStepService;

	@Inject
	private CriteriaQueryHelper criteriaQueryHelper;

	@Inject
	private Authorizer authorizer;

	@Inject
	private PrincipalBean principalBean;


	@Nonnull
	@Override
	public DokumentGrund saveDokumentGrund(@Nonnull DokumentGrund dokumentGrund) {
		Objects.requireNonNull(dokumentGrund);
		if (dokumentGrund.getDokumente() != null) {
			dokumentGrund.getDokumente().forEach(dokument -> {
				if (dokument.getTimestampUpload() == null) {
					dokument.setTimestampUpload(LocalDateTime.now());
				}
			});
		}
		// Falls es der Gesuchsteller war, der das Dokument hochgeladen hat, soll das Flag auf dem Gesuch gesetzt werden,
		// damit das Jugendamt es sieht. Allerdings nur wenn das Gesuch schon freigegeben wurde
		if (principalBean.isCallerInRole(UserRole.GESUCHSTELLER) && !dokumentGrund.getGesuch().getStatus().isAnyOfInBearbeitungGS()) {
			dokumentGrund.getGesuch().setDokumenteHochgeladen(Boolean.TRUE);
		}
		final DokumentGrund mergedDokumentGrund = persistence.merge(dokumentGrund);
		wizardStepService.updateSteps(mergedDokumentGrund.getGesuch().getId(), null, null, WizardStepName.DOKUMENTE);
		return mergedDokumentGrund;
	}

	@Override
	@Nonnull
	public Optional<DokumentGrund> findDokumentGrund(@Nonnull String key) {
		Objects.requireNonNull(key, "id muss gesetzt sein");
		DokumentGrund a = persistence.find(DokumentGrund.class, key);
		return Optional.ofNullable(a);
	}

	@Override
	@Nonnull
	public Collection<DokumentGrund> findAllDokumentGrundByGesuch(@Nonnull Gesuch gesuch) {
		return this.findAllDokumentGrundByGesuch(gesuch, true);

	}

	@Nonnull
	@Override
	public Collection<DokumentGrund> findAllDokumentGrundByGesuch(@Nonnull Gesuch gesuch, boolean doAuthCheck) {
		Objects.requireNonNull(gesuch);
		if (doAuthCheck) {
			this.authorizer.checkReadAuthorization(gesuch);
		}
		return criteriaQueryHelper.getEntitiesByAttribute(DokumentGrund.class, gesuch, DokumentGrund_.gesuch);
	}

	@Override
	@Nonnull
	public Collection<DokumentGrund> findAllDokumentGrundByGesuchAndDokumentType(@Nonnull Gesuch gesuch, @Nonnull DokumentGrundTyp dokumentGrundTyp) {
		Objects.requireNonNull(gesuch);

		this.authorizer.checkReadAuthorization(gesuch);
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<DokumentGrund> query = cb.createQuery(DokumentGrund.class);

		Root<DokumentGrund> root = query.from(DokumentGrund.class);

		Predicate predicateGesuch = cb.equal(root.get(DokumentGrund_.gesuch), gesuch);
		Predicate predicateDokumentGrundTyp = cb.equal(root.get(DokumentGrund_.dokumentGrundTyp), dokumentGrundTyp);

		query.where(predicateGesuch, predicateDokumentGrundTyp);
		return persistence.getCriteriaResults(query);
	}

	@Override
	@Nullable
	public DokumentGrund updateDokumentGrund(@Nonnull DokumentGrund dokumentGrund) {
		Objects.requireNonNull(dokumentGrund);

		//Wenn DokumentGrund keine Dokumente mehr hat und nicht gebraucht wird, wird er entfernt
		if (!dokumentGrund.isNeeded() && (dokumentGrund.getDokumente() == null || dokumentGrund.getDokumente().isEmpty())) {
			persistence.remove(dokumentGrund);
			return null;
		}
		final DokumentGrund mergedDokument = persistence.merge(dokumentGrund);
		wizardStepService.updateSteps(mergedDokument.getGesuch().getId(), null, null, WizardStepName.DOKUMENTE);
		return mergedDokument;
	}

	@Override
	@RolesAllowed({SUPER_ADMIN, ADMIN,})
	public void removeAllDokumentGrundeFromGesuch(@Nonnull Gesuch gesuch) {
		LOGGER.info("Deleting Dokument-Gruende of Gesuch: {} / {}", gesuch.getFall(), gesuch.getGesuchsperiode().getGesuchsperiodeString());
		Collection<DokumentGrund> dokumentsFromGesuch = findAllDokumentGrundByGesuch(gesuch);
		for (DokumentGrund dokument : dokumentsFromGesuch) {
			LOGGER.info("Deleting DokumentGrund: {}", dokument.getId());
			persistence.remove(DokumentGrund.class, dokument.getId());
		}
	}
}
