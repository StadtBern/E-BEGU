package ch.dvbern.ebegu.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.activation.MimeTypeParseException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import ch.dvbern.ebegu.authentication.PrincipalBean;
import ch.dvbern.ebegu.dto.JaxAntragDTO;
import ch.dvbern.ebegu.dto.suchfilter.smarttable.AntragTableFilterDTO;
import ch.dvbern.ebegu.dto.suchfilter.smarttable.PredicateObjectDTO;
import ch.dvbern.ebegu.entities.AbstractEntity_;
import ch.dvbern.ebegu.entities.AntragStatusHistory;
import ch.dvbern.ebegu.entities.AntragStatusHistory_;
import ch.dvbern.ebegu.entities.Benutzer;
import ch.dvbern.ebegu.entities.Benutzer_;
import ch.dvbern.ebegu.entities.Betreuung;
import ch.dvbern.ebegu.entities.Betreuung_;
import ch.dvbern.ebegu.entities.Betreuungsmitteilung;
import ch.dvbern.ebegu.entities.Fall;
import ch.dvbern.ebegu.entities.Fall_;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.entities.Gesuch_;
import ch.dvbern.ebegu.entities.Gesuchsperiode;
import ch.dvbern.ebegu.entities.Gesuchsperiode_;
import ch.dvbern.ebegu.entities.Gesuchsteller;
import ch.dvbern.ebegu.entities.GesuchstellerContainer;
import ch.dvbern.ebegu.entities.GesuchstellerContainer_;
import ch.dvbern.ebegu.entities.Gesuchsteller_;
import ch.dvbern.ebegu.entities.Institution;
import ch.dvbern.ebegu.entities.InstitutionStammdaten;
import ch.dvbern.ebegu.entities.InstitutionStammdaten_;
import ch.dvbern.ebegu.entities.Institution_;
import ch.dvbern.ebegu.entities.Kind;
import ch.dvbern.ebegu.entities.KindContainer;
import ch.dvbern.ebegu.entities.KindContainer_;
import ch.dvbern.ebegu.entities.Kind_;
import ch.dvbern.ebegu.enums.AntragStatus;
import ch.dvbern.ebegu.enums.AntragStatusDTO;
import ch.dvbern.ebegu.enums.AntragTyp;
import ch.dvbern.ebegu.enums.ApplicationPropertyKey;
import ch.dvbern.ebegu.enums.BetreuungsangebotTyp;
import ch.dvbern.ebegu.enums.Eingangsart;
import ch.dvbern.ebegu.enums.ErrorCodeEnum;
import ch.dvbern.ebegu.enums.GesuchsperiodeStatus;
import ch.dvbern.ebegu.enums.UserRole;
import ch.dvbern.ebegu.enums.UserRoleName;
import ch.dvbern.ebegu.enums.WizardStepName;
import ch.dvbern.ebegu.errors.EbeguEntityNotFoundException;
import ch.dvbern.ebegu.errors.EbeguExistingAntragException;
import ch.dvbern.ebegu.errors.EbeguRuntimeException;
import ch.dvbern.ebegu.errors.MailException;
import ch.dvbern.ebegu.errors.MergeDocException;
import ch.dvbern.ebegu.persistence.CriteriaQueryHelper;
import ch.dvbern.ebegu.services.interceptors.UpdateStatusInterceptor;
import ch.dvbern.ebegu.types.DateRange_;
import ch.dvbern.ebegu.util.AntragStatusConverterUtil;
import ch.dvbern.ebegu.util.Constants;
import ch.dvbern.ebegu.util.FreigabeCopyUtil;
import ch.dvbern.ebegu.validationgroups.AntragCompleteValidationGroup;
import ch.dvbern.lib.cdipersistence.Persistence;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Service fuer Gesuch
 */
@Stateless
@Local(GesuchService.class)
@PermitAll
@SuppressWarnings(value = {"PMD.AvoidDuplicateLiterals", "LocalVariableNamingConvention", "PMD.NcssTypeCount"})
public class GesuchServiceBean extends AbstractBaseService implements GesuchService {

	private static final Logger LOG = LoggerFactory.getLogger(GesuchServiceBean.class.getSimpleName());

	@Inject
	private Persistence<Gesuch> persistence;
	@Inject
	private CriteriaQueryHelper criteriaQueryHelper;

	@Inject
	private BenutzerService benutzerService;
	@Inject
	private WizardStepService wizardStepService;
	@Inject
	private AntragStatusHistoryService antragStatusHistoryService;
	@Inject
	private FallService fallService;
	@Inject
	private GesuchsperiodeService gesuchsperiodeService;
	@Inject
	private MahnungService mahnungService;
	@Inject
	private GeneratedDokumentService generatedDokumentService;
	@Inject
	private DokumentGrundService dokumentGrundService;
	@Inject
	private Authorizer authorizer;
	@Inject
	private BooleanAuthorizer booleanAuthorizer;
	@Inject
	private PrincipalBean principalBean;
	@Inject
	private MailService mailService;
	@Inject
	private ApplicationPropertyService applicationPropertyService;
	@Inject
	private ZahlungService zahlungService;
	@Inject
	private SuperAdminService superAdminService;
	@Inject
	private FileSaverService fileSaverService;
	@Inject
	private MitteilungService mitteilungService;
	@Inject
	private BetreuungService betreuungService;


	@Nonnull
	@Override
	@RolesAllowed(value = {UserRoleName.SUPER_ADMIN, UserRoleName.ADMIN, UserRoleName.SACHBEARBEITER_JA, UserRoleName.GESUCHSTELLER})
	public Gesuch createGesuch(@Nonnull Gesuch gesuch) {
		Objects.requireNonNull(gesuch);
		authorizer.checkCreateAuthorizationGesuch();
		final Gesuch persistedGesuch = persistence.persist(gesuch);
		// Die WizsrdSteps werden direkt erstellt wenn das Gesuch erstellt wird. So vergewissern wir uns dass es kein Gesuch ohne WizardSteps gibt
		wizardStepService.createWizardStepList(persistedGesuch);
		antragStatusHistoryService.saveStatusChange(persistedGesuch, null);
		return persistedGesuch;
	}

	@Nonnull
	@Override
	@PermitAll
	public Gesuch updateGesuch(@Nonnull Gesuch gesuch, boolean saveInStatusHistory, @Nullable Benutzer saveAsUser) {
		return updateGesuch(gesuch, saveInStatusHistory, saveAsUser, true);
	}

	@Nonnull
	@Override
	@PermitAll
	public Gesuch updateGesuch(@Nonnull Gesuch gesuch, boolean saveInStatusHistory, @Nullable Benutzer saveAsUser, boolean doAuthCheck) {
		if (doAuthCheck) {
			authorizer.checkWriteAuthorization(gesuch);
		}
		Objects.requireNonNull(gesuch);
		final Gesuch merged = persistence.merge(gesuch);
		if (saveInStatusHistory) {
			antragStatusHistoryService.saveStatusChange(merged, saveAsUser);
		}
		return merged;
	}

	@Nonnull
	@Override
	@PermitAll
	@Interceptors(UpdateStatusInterceptor.class)
	public Optional<Gesuch> findGesuch(@Nonnull String key) {
		Objects.requireNonNull(key, "id muss gesetzt sein");
		Gesuch gesuch = persistence.find(Gesuch.class, key);
		authorizer.checkReadAuthorization(gesuch);
		return Optional.ofNullable(gesuch);
	}

	@Override
	@Nonnull
	@RolesAllowed(value = {UserRoleName.ADMIN, UserRoleName.SUPER_ADMIN, UserRoleName.SACHBEARBEITER_JA, UserRoleName.SCHULAMT})
	public Optional<Gesuch> findGesuchForFreigabe(@Nonnull String gesuchId) {
		Objects.requireNonNull(gesuchId, "gesuchId muss gesetzt sein");
		Gesuch gesuch = persistence.find(Gesuch.class, gesuchId);
		if (gesuch != null) {
			authorizer.checkReadAuthorizationForFreigabe(gesuch);
			return Optional.of(gesuch);
		}
		return Optional.empty();
	}

	@PermitAll
	@Override
	public List<Gesuch> findReadableGesuche(@Nullable Collection<String> gesuchIds) {
		if (gesuchIds == null || gesuchIds.isEmpty()) {
			return Collections.emptyList();
		}
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);

		Predicate predicateId = root.get(Gesuch_.id).in(gesuchIds);
		query.where(predicateId);
		query.orderBy(cb.asc(root.get(Gesuch_.fall).get(Fall_.id)));
		List<Gesuch> criteriaResults = persistence.getCriteriaResults(query);
		return criteriaResults.stream()
			.filter(gesuch -> this.booleanAuthorizer.hasReadAuthorization(gesuch))
			.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	@RolesAllowed(value = {UserRoleName.ADMIN, UserRoleName.SUPER_ADMIN})
	public Collection<Gesuch> getAllGesuche() {
		return new ArrayList<>(criteriaQueryHelper.getAll(Gesuch.class));
	}

	@Nonnull
	@Override
	@RolesAllowed(value = {UserRoleName.ADMIN, UserRoleName.SUPER_ADMIN, UserRoleName.SACHBEARBEITER_JA}) // TODO (team) evt. umbenennen: getGesuchePendenzenJugendamt
	public Collection<Gesuch> getAllActiveGesuche() {
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);

		Predicate predicateStatus = root.get(Gesuch_.status).in(AntragStatus.FOR_SACHBEARBEITER_JUGENDAMT_PENDENZEN);
		// Gesuchsperiode darf nicht geschlossen sein
		Predicate predicateGesuchsperiode = root.get(Gesuch_.gesuchsperiode).get(Gesuchsperiode_.status).in(GesuchsperiodeStatus.AKTIV, GesuchsperiodeStatus.INAKTIV);

		query.where(predicateStatus, predicateGesuchsperiode);
		query.orderBy(cb.asc(root.get(Gesuch_.fall).get(Fall_.fallNummer)));
		return persistence.getCriteriaResults(query);
	}

	@Nonnull
	@Override
	@RolesAllowed(value = {UserRoleName.ADMIN, UserRoleName.SUPER_ADMIN, UserRoleName.SACHBEARBEITER_JA})  // TODO (team) evt. umbenennen: getGesucheForBenutzerPendenzenJugendamt
	public Collection<Gesuch> getAllActiveGesucheOfVerantwortlichePerson(@Nonnull String benutzername) {
		Validate.notNull(benutzername);
		Benutzer benutzer = benutzerService.findBenutzer(benutzername).orElseThrow(() -> new EbeguEntityNotFoundException("getAllActiveGesucheOfVerantwortlichePerson", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, benutzername));

		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);

		Predicate predicateStatus = root.get(Gesuch_.status).in(AntragStatus.FOR_SACHBEARBEITER_JUGENDAMT_PENDENZEN);
		Predicate predicateVerantwortlicher = cb.equal(root.get(Gesuch_.fall).get(Fall_.verantwortlicher), benutzer);
		// Gesuchsperiode darf nicht geschlossen sein
		Predicate predicateGesuchsperiode = root.get(Gesuch_.gesuchsperiode).get(Gesuchsperiode_.status).in(GesuchsperiodeStatus.AKTIV, GesuchsperiodeStatus.INAKTIV);

		query.where(predicateStatus, predicateVerantwortlicher, predicateGesuchsperiode);
		query.orderBy(cb.asc(root.get(Gesuch_.fall).get(Fall_.fallNummer)));
		return persistence.getCriteriaResults(query);
	}

	@Override
	@RolesAllowed(value = {UserRoleName.SUPER_ADMIN, UserRoleName.ADMIN})
	public void removeGesuch(@Nonnull String gesuchId) {
		Validate.notNull(gesuchId);
		Optional<Gesuch> gesuchOptional = findGesuch(gesuchId);
		Gesuch gesToRemove = gesuchOptional.orElseThrow(() -> new EbeguEntityNotFoundException("removeGesuch", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, gesuchId));
		authorizer.checkWriteAuthorization(gesuchOptional.get());
		//Remove all depending objects
		wizardStepService.removeSteps(gesToRemove);  //wizard steps removen
		mahnungService.removeAllMahnungenFromGesuch(gesToRemove);
		generatedDokumentService.removeAllGeneratedDokumenteFromGesuch(gesToRemove);
		dokumentGrundService.removeAllDokumentGrundeFromGesuch(gesToRemove);
		fileSaverService.removeAllFromSubfolder(gesToRemove.getId());
		antragStatusHistoryService.removeAllAntragStatusHistoryFromGesuch(gesToRemove);
		zahlungService.deleteZahlungspositionenOfGesuch(gesToRemove);
		mitteilungService.removeAllBetreuungMitteilungenForGesuch(gesToRemove);


		//Finally remove the Gesuch when all other objects are really removed
		persistence.remove(gesToRemove);
	}

	@Nonnull
	@Override
	@RolesAllowed(value = {UserRoleName.ADMIN, UserRoleName.SUPER_ADMIN})
	public List<Gesuch> findGesuchByGSName(String nachname, String vorname) {
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);

		ParameterExpression<String> nameParam = cb.parameter(String.class, "nachname");
		Predicate namePredicate = cb.equal(root.get(Gesuch_.gesuchsteller1).get(GesuchstellerContainer_.gesuchstellerJA).get(Gesuchsteller_.nachname), nameParam);

		ParameterExpression<String> vornameParam = cb.parameter(String.class, "vorname");
		Predicate vornamePredicate = cb.equal(root.get(Gesuch_.gesuchsteller1).get(GesuchstellerContainer_.gesuchstellerJA).get(Gesuchsteller_.vorname), vornameParam);

		query.where(namePredicate, vornamePredicate);
		TypedQuery<Gesuch> q = persistence.getEntityManager().createQuery(query);
		q.setParameter(nameParam, nachname);
		q.setParameter(vornameParam, vorname);

		return q.getResultList();
	}

	@Override
	@Nonnull
	@RolesAllowed(value = {UserRoleName.GESUCHSTELLER, UserRoleName.SUPER_ADMIN}) //TODO (team) evt. umbenennen: getGesucheDashboardGesuchsteller
	public List<Gesuch> getAntraegeByCurrentBenutzer() {
		Optional<Fall> fallOptional = fallService.findFallByCurrentBenutzerAsBesitzer();
		if (fallOptional.isPresent()) {
			final CriteriaBuilder cb = persistence.getCriteriaBuilder();
			final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

			Root<Gesuch> root = query.from(Gesuch.class);
			Predicate predicate = cb.equal(root.get(Gesuch_.fall), fallOptional.get());
			query.orderBy(cb.desc(root.get(Gesuch_.laufnummer)));
			query.where(predicate);

			List<Gesuch> gesuche = persistence.getCriteriaResults(query);
			authorizer.checkReadAuthorizationGesuche(gesuche);
			return gesuche;
		}
		return Collections.emptyList();
	}

	@Override
	@PermitAll
	public Pair<Long, List<Gesuch>> searchAntraege(@Nonnull AntragTableFilterDTO antragTableFilterDto) {
		Pair<Long, List<Gesuch>> result;
		Long countResult = searchAntraege(antragTableFilterDto, Mode.COUNT).getLeft();
		if (countResult.equals(Long.valueOf(0))) {    // no result found
			result = new ImmutablePair<>(0L, Collections.emptyList());
		} else {
			Pair<Long, List<Gesuch>> searchResult = searchAntraege(antragTableFilterDto, Mode.SEARCH);
			result = new ImmutablePair<>(countResult, searchResult.getRight());
		}
		return result;
	}

	@SuppressWarnings("PMD.NcssMethodCount")
	private Pair<Long, List<Gesuch>> searchAntraege(@Nonnull AntragTableFilterDTO antragTableFilterDto, @Nonnull Mode mode) {
		Benutzer user = benutzerService.getCurrentBenutzer().orElseThrow(() -> new EbeguRuntimeException("searchAntraege", "No User is logged in"));
		UserRole role = user.getRole();
		Set<AntragStatus> allowedAntragStatus = AntragStatus.allowedforRole(role);
		if (allowedAntragStatus.isEmpty()) {
			return new ImmutablePair<>(0L, Collections.emptyList());
		}

		CriteriaBuilder cb = persistence.getCriteriaBuilder();
		CriteriaQuery query;
		switch (mode) {
			case SEARCH:
				query = cb.createQuery(String.class);
				break;
			case COUNT:
				query = cb.createQuery(Long.class);
				break;
			default:
				throw new IllegalStateException("Undefined Mode for searchAntraege Query: " + mode);
		}
		// Construct from-clause
		Root<Gesuch> root = query.from(Gesuch.class);
		// Join all the relevant relations (except gesuchsteller join, which is only done when needed)
		Join<Gesuch, Fall> fall = root.join(Gesuch_.fall, JoinType.INNER);
		Join<Fall, Benutzer> verantwortlicher = fall.join(Fall_.verantwortlicher, JoinType.LEFT);
		Join<Gesuch, Gesuchsperiode> gesuchsperiode = root.join(Gesuch_.gesuchsperiode, JoinType.INNER);

		SetJoin<Gesuch, KindContainer> kindContainers = root.join(Gesuch_.kindContainers, JoinType.LEFT);
		SetJoin<KindContainer, Betreuung> betreuungen = kindContainers.join(KindContainer_.betreuungen, JoinType.LEFT);
		Join<KindContainer, Kind> kinder = kindContainers.join(KindContainer_.kindJA, JoinType.LEFT);
		Join<Betreuung, InstitutionStammdaten> institutionstammdaten = betreuungen.join(Betreuung_.institutionStammdaten, JoinType.LEFT);
		Join<InstitutionStammdaten, Institution> institution = institutionstammdaten.join(InstitutionStammdaten_.institution, JoinType.LEFT);

		//prepare predicates
		List<Expression<Boolean>> predicates = new ArrayList<>();

		// General role based predicates
		Predicate inClauseStatus = root.get(Gesuch_.status).in(allowedAntragStatus);
		predicates.add(inClauseStatus);

		// Special role based predicates
		switch (role) {
			case SUPER_ADMIN:
			case ADMIN:
			case REVISOR:
			case JURIST:
				break;
			case STEUERAMT:
				break;
			case SACHBEARBEITER_JA:
				// Jugendamt-Mitarbeiter duerfen auch Faelle sehen, die noch gar keine Kinder/Betreuungen haben.
				// Wenn aber solche erfasst sind, dann duerfen sie nur diejenigen sehen, die nicht nur Schulamt haben
				// zudem muss auch der status ensprechend sein
				Predicate predicateKeineKinder = kindContainers.isNull();
				Predicate predicateKeineBetreuungen = betreuungen.isNull();
				Predicate predicateKeineInstitutionsstammdaten = institutionstammdaten.isNull();
				Predicate predicateKeineInstitution = institution.isNull();
				Predicate predicateAngebotstyp = cb.notEqual(institutionstammdaten.get(InstitutionStammdaten_.betreuungsangebotTyp), BetreuungsangebotTyp.TAGESSCHULE);
				Predicate predicateRichtigerAngebotstypOderNichtAusgefuellt = cb.or(predicateKeineKinder, predicateKeineBetreuungen, predicateKeineInstitutionsstammdaten, predicateKeineInstitution, predicateAngebotstyp);
				predicates.add(predicateRichtigerAngebotstypOderNichtAusgefuellt);
				break;
			case SACHBEARBEITER_TRAEGERSCHAFT:
				predicates.add(cb.equal(institution.get(Institution_.traegerschaft), user.getTraegerschaft()));
				break;
			case SACHBEARBEITER_INSTITUTION:
				// es geht hier nicht um die institution des zugewiesenen benutzers sondern um die institution des eingeloggten benutzers
				predicates.add(cb.equal(institution, user.getInstitution()));
				break;
			case SCHULAMT:
				predicates.add(cb.equal(institutionstammdaten.get(InstitutionStammdaten_.betreuungsangebotTyp), BetreuungsangebotTyp.TAGESSCHULE));
				break;
			default:
				LOG.warn("antragSearch can not be performed by users in role " + role);
				predicates.add(cb.isFalse(cb.literal(Boolean.TRUE))); // impossible predicate
				break;
		}

		// Predicates derived from PredicateDTO
		PredicateObjectDTO predicateObjectDto = antragTableFilterDto.getSearch().getPredicateObject();
		if (predicateObjectDto != null) {
			if (predicateObjectDto.getFallNummer() != null) {
				// Die Fallnummer muss als String mit LIKE verglichen werden: Bei Eingabe von "14" soll der Fall "114" kommen
				Expression<String> fallNummerAsString = fall.get(Fall_.fallNummer).as(String.class);
				String fallNummerWithWildcards = "%" + predicateObjectDto.getFallNummer() + "%";
				predicates.add(cb.like(fallNummerAsString, fallNummerWithWildcards));
			}
			if (predicateObjectDto.getFamilienName() != null) {
				Join<Gesuch, GesuchstellerContainer> gesuchsteller1 = root.join(Gesuch_.gesuchsteller1, JoinType.LEFT);
				Join<Gesuch, GesuchstellerContainer> gesuchsteller2 = root.join(Gesuch_.gesuchsteller2, JoinType.LEFT);
				Join<GesuchstellerContainer, Gesuchsteller> gesuchsteller1JA = gesuchsteller1.join(GesuchstellerContainer_.gesuchstellerJA, JoinType.LEFT);
				Join<GesuchstellerContainer, Gesuchsteller> gesuchsteller2JA = gesuchsteller2.join(GesuchstellerContainer_.gesuchstellerJA, JoinType.LEFT);
				predicates.add(
					cb.or(
						cb.like(gesuchsteller1JA.get(Gesuchsteller_.nachname), predicateObjectDto.getFamilienNameForLike()),
						cb.like(gesuchsteller2JA.get(Gesuchsteller_.nachname), predicateObjectDto.getFamilienNameForLike())
					));
			}
			if (predicateObjectDto.getAntragTyp() != null) {
				List<AntragTyp> values = AntragTyp.getValuesForFilter(predicateObjectDto.getAntragTyp());
				predicates.add(root.get(Gesuch_.typ).in(values));
			}
			if (predicateObjectDto.getGesuchsperiodeString() != null) {
				String[] years = ensureYearFormat(predicateObjectDto.getGesuchsperiodeString());
				predicates.add(
					cb.and(
						cb.equal(cb.function("year", Integer.class, gesuchsperiode.get(Gesuchsperiode_.gueltigkeit).get(DateRange_.gueltigAb)), years[0]),
						cb.equal(cb.function("year", Integer.class, gesuchsperiode.get(Gesuchsperiode_.gueltigkeit).get(DateRange_.gueltigBis)), years[1]))
				);
			}
			if (predicateObjectDto.getEingangsdatum() != null) {
				try {
					LocalDate searchDate = LocalDate.parse(predicateObjectDto.getEingangsdatum(), Constants.DATE_FORMATTER);
					predicates.add(cb.equal(root.get(Gesuch_.eingangsdatum), searchDate));
				} catch (DateTimeParseException e) {
					// Kein gueltiges Datum. Es kann kein Gesuch geben, welches passt. Wir geben leer zurueck
					return new ImmutablePair<>(0L, Collections.emptyList());
				}
			}
			if (predicateObjectDto.getEingangsdatumSTV() != null) {
				try {
					LocalDate searchDate = LocalDate.parse(predicateObjectDto.getEingangsdatumSTV(), Constants.DATE_FORMATTER);
					predicates.add(cb.equal(root.get(Gesuch_.eingangsdatumSTV), searchDate));
				} catch (DateTimeParseException e) {
					// Kein gueltiges Datum. Es kann kein Gesuch geben, welches passt. Wir geben leer zurueck
					return new ImmutablePair<>(0L, Collections.emptyList());
				}
			}
			if (predicateObjectDto.getAenderungsdatum() != null) {
				try {
					// Wir wollen ohne Zeit vergleichen
					Expression<LocalDate> timestampAsLocalDate = root.get(Gesuch_.timestampMutiert).as(LocalDate.class);
					LocalDate searchDate = LocalDate.parse(predicateObjectDto.getAenderungsdatum(), Constants.DATE_FORMATTER);
					predicates.add(cb.equal(timestampAsLocalDate, searchDate));
				} catch (DateTimeParseException e) {
					// Kein gueltiges Datum. Es kann kein Gesuch geben, welches passt. Wir geben leer zurueck
					return new ImmutablePair<>(0L, Collections.emptyList());
				}
			}
			if (predicateObjectDto.getStatus() != null) {
				// Achtung, hier muss von Client zu Server Status konvertiert werden!
				Collection<AntragStatus> antragStatus = AntragStatusConverterUtil.convertStatusToEntityForRole(AntragStatusDTO.valueOf(predicateObjectDto.getStatus()), role);
				predicates.add(root.get(Gesuch_.status).in(antragStatus));
			}
			if (predicateObjectDto.getAngebote() != null) {
				predicates.add(cb.equal(institutionstammdaten.get(InstitutionStammdaten_.betreuungsangebotTyp), BetreuungsangebotTyp.valueOf(predicateObjectDto.getAngebote())));
			}
			if (predicateObjectDto.getInstitutionen() != null) {
				predicates.add(cb.equal(institution.get(Institution_.name), predicateObjectDto.getInstitutionen()));
			}
			if (predicateObjectDto.getKinder() != null) {
				predicates.add(cb.like(kinder.get(Kind_.vorname), predicateObjectDto.getKindNameForLike()));
			}
			if (predicateObjectDto.getVerantwortlicher() != null) {
				String[] strings = predicateObjectDto.getVerantwortlicher().split(" ");
				predicates.add(
					cb.and(
						cb.equal(verantwortlicher.get(Benutzer_.vorname), strings[0]),
						cb.equal(verantwortlicher.get(Benutzer_.nachname), strings[1])
					));
			}
		}
		// Construct the select- and where-clause
		switch (mode) {
			case SEARCH:
				query.select(root.get(Gesuch_.id))
					.where(CriteriaQueryHelper.concatenateExpressions(cb, predicates));
				constructOrderByClause(antragTableFilterDto, cb, query, root, kinder, gesuchsperiode, institutionstammdaten, institution);
				break;
			case COUNT:
				query.select(cb.countDistinct(root.get(Gesuch_.id)))
					.where(CriteriaQueryHelper.concatenateExpressions(cb, predicates));
				break;
		}

		// Prepare and execute the query and build the result
		Pair<Long, List<Gesuch>> result = null;
		switch (mode) {
			case SEARCH:
				List<String> gesuchIds = persistence.getCriteriaResults(query); //select all ids in order, may contain duplicates
				List<Gesuch> pagedResult;
				if (antragTableFilterDto.getPagination() != null) {
					int firstIndex = antragTableFilterDto.getPagination().getStart();
					Integer maxresults = antragTableFilterDto.getPagination().getNumber();
					List<String> orderedIdsToLoad = this.determineDistinctGesuchIdsToLoad(gesuchIds, firstIndex, maxresults);
					pagedResult = findGesuche(orderedIdsToLoad);
				} else {
					pagedResult = findGesuche(gesuchIds);
				}
				result = new ImmutablePair<>(null, pagedResult);
				break;
			case COUNT:
				Long count = (Long) persistence.getCriteriaSingleResult(query);
				result = new ImmutablePair<>(count, null);
				break;
		}
		return result;
	}

	@SuppressWarnings({"PMD.NcssMethodCount"})
	private void constructOrderByClause(@Nonnull AntragTableFilterDTO antragTableFilterDto, CriteriaBuilder cb, CriteriaQuery query,
										Root<Gesuch> root, Join<KindContainer, Kind> kinder,
										Join<Gesuch, Gesuchsperiode> gesuchsperiode,
										Join<Betreuung, InstitutionStammdaten> institutionstammdaten,
										Join<InstitutionStammdaten, Institution> institution) {
		Expression<?> expression;
		if (antragTableFilterDto.getSort() != null && antragTableFilterDto.getSort().getPredicate() != null) {
			switch (antragTableFilterDto.getSort().getPredicate()) {
				case "fallNummer":
					expression = root.get(Gesuch_.fall).get(Fall_.fallNummer);
					break;
				case "familienName":
					expression = root.get(Gesuch_.gesuchsteller1).get(GesuchstellerContainer_.gesuchstellerJA).get(Gesuchsteller_.nachname);
					break;
				case "antragTyp":
					expression = root.get(Gesuch_.typ);
					break;
				case "gesuchsperiode":
					expression = gesuchsperiode.get(Gesuchsperiode_.gueltigkeit).get(DateRange_.gueltigAb);
					break;
				case "aenderungsdatum":
					expression = root.get(Gesuch_.timestampMutiert);
					break;
				case "eingangsdatum":
					expression = root.get(Gesuch_.eingangsdatum);
					break;
				case "eingangsdatumSTV":
					expression = root.get(Gesuch_.eingangsdatumSTV);
					break;
				case "status":
					expression = root.get(Gesuch_.status);
					break;
				case "angebote":
					// Die Angebote sind eigentlich eine Liste innerhalb der Liste (also des Tabelleneintrages).
					// Kinder ohne Angebot sollen egal wie sortiert ist am Schluss kommen!
					if (antragTableFilterDto.getSort().getReverse()) {
						expression = cb.selectCase().when(institutionstammdaten.isNull(), "ZZZZ")
							.otherwise(institutionstammdaten.get(InstitutionStammdaten_.betreuungsangebotTyp));
					} else {
						expression = cb.selectCase().when(institutionstammdaten.isNull(), "0000")
							.otherwise(institutionstammdaten.get(InstitutionStammdaten_.betreuungsangebotTyp));
					}
					break;
				case "institutionen":
					// Die Institutionen sind eigentlich eine Liste innerhalb der Liste (also des Tabelleneintrages).
					// Kinder ohne Angebot sollen egal wie sortiert ist am Schluss kommen!
					if (antragTableFilterDto.getSort().getReverse()) {
						expression = cb.selectCase().when(institution.isNull(), "ZZZZ")
							.otherwise(institution.get(Institution_.name));
					} else {
						expression = cb.selectCase().when(institution.isNull(), "0000")
							.otherwise(institution.get(Institution_.name));
					}
					break;
				case "verantwortlicher":
					expression = root.get(Gesuch_.fall).get(Fall_.verantwortlicher).get(Benutzer_.nachname);
					break;
				case "kinder":
					expression = kinder.get(Kind_.vorname);
					break;
				default:
					LOG.warn("Using default sort by FallNummer because there is no specific clause for predicate " + antragTableFilterDto.getSort().getPredicate());
					expression = root.get(Gesuch_.fall).get(Fall_.fallNummer);
					break;
			}
			query.orderBy(antragTableFilterDto.getSort().getReverse() ? cb.asc(expression) : cb.desc(expression));
		} else {
			// Default sort when nothing is choosen
			expression = root.get(Gesuch_.timestampMutiert);
			query.orderBy(cb.desc(expression));
		}

	}

	private enum Mode {
		COUNT,
		SEARCH
	}

	/**
	 * Diese Methode sucht alle Antraege die zu dem gegebenen Fall gehoeren.
	 * Die Antraege werden aber je nach Benutzerrolle gefiltert.
	 * - SACHBEARBEITER_TRAEGERSCHAFT oder SACHBEARBEITER_INSTITUTION - werden nur diejenige Antraege zurueckgegeben,
	 * die mindestens ein Angebot fuer die InstituionEn des Benutzers haben
	 * - SCHULAMT - werden nur diejenige Antraege zurueckgegeben, die mindestens ein Angebot von Typ Schulamt haben
	 * - SACHBEARBEITER_JA oder ADMIN - werden nur diejenige Antraege zurueckgegeben, die ein Angebot von einem anderen
	 * Typ als Schulamt haben oder ueberhaupt kein Angebot
	 */
	@Nonnull
	@Override
	@PermitAll
	public List<JaxAntragDTO> getAllAntragDTOForFall(String fallId) {
		authorizer.checkReadAuthorizationFall(fallId);

		final Optional<Benutzer> optBenutzer = benutzerService.getCurrentBenutzer();
		if (optBenutzer.isPresent()) {
			final Benutzer benutzer = optBenutzer.get();

			final CriteriaBuilder cb = persistence.getCriteriaBuilder();
			final CriteriaQuery<JaxAntragDTO> query = cb.createQuery(JaxAntragDTO.class);
			Root<Gesuch> root = query.from(Gesuch.class);


			Join<InstitutionStammdaten, Institution> institutionJoin = null;
			Join<Betreuung, InstitutionStammdaten> institutionstammdatenJoin = null;

			if (benutzer.getRole().equals(UserRole.SACHBEARBEITER_TRAEGERSCHAFT)
				|| benutzer.getRole().equals(UserRole.SACHBEARBEITER_INSTITUTION)
				|| benutzer.getRole().equals(UserRole.SCHULAMT)
				|| benutzer.getRole().equals(UserRole.ADMIN)
				|| benutzer.getRole().equals(UserRole.SACHBEARBEITER_JA)) {
				// Join all the relevant relations only when the User belongs to Admin, JA, Schulamt, Institution or Traegerschaft
				SetJoin<Gesuch, KindContainer> kindContainers = root.join(Gesuch_.kindContainers, JoinType.LEFT);
				SetJoin<KindContainer, Betreuung> betreuungen = kindContainers.join(KindContainer_.betreuungen, JoinType.LEFT);
				institutionstammdatenJoin = betreuungen.join(Betreuung_.institutionStammdaten, JoinType.LEFT);
				institutionJoin = institutionstammdatenJoin.join(InstitutionStammdaten_.institution, JoinType.LEFT);
			}
			Join<Gesuch, Fall> fallJoin = root.join(Gesuch_.fall);
			Join<Fall, Benutzer> besitzerJoin = fallJoin.join(Fall_.besitzer, JoinType.LEFT);

			query.multiselect(
				root.get(Gesuch_.id),
				root.get(Gesuch_.gesuchsperiode).get(Gesuchsperiode_.gueltigkeit).get(DateRange_.gueltigAb),
				root.get(Gesuch_.gesuchsperiode).get(Gesuchsperiode_.gueltigkeit).get(DateRange_.gueltigBis),
				root.get(Gesuch_.eingangsdatum),
				root.get(Gesuch_.eingangsdatumSTV),
				root.get(Gesuch_.typ),
				root.get(Gesuch_.status),
				root.get(Gesuch_.laufnummer),
				root.get(Gesuch_.eingangsart),
				besitzerJoin.get(Benutzer_.username) //wir machen hier extra vorher einen left join
			).distinct(true);

			ParameterExpression<String> fallIdParam = cb.parameter(String.class, "fallId");

			List<Expression<Boolean>> predicatesToUse = new ArrayList<>();
			Expression<Boolean> fallPredicate = cb.equal(root.get(Gesuch_.fall).get(AbstractEntity_.id), fallIdParam);
			predicatesToUse.add(fallPredicate);

			// Alle AUSSER Gesuchsteller, Institution und Trägerschaft muessen im Status eingeschraenkt werden,
			// d.h. sie duerfen IN_BEARBEITUNG_GS und FREIGABEQUITTUNG NICHT sehen
			if (!(benutzer.getRole().equals(UserRole.GESUCHSTELLER)
				|| benutzer.getRole().equals(UserRole.SACHBEARBEITER_TRAEGERSCHAFT)
				|| benutzer.getRole().equals(UserRole.SACHBEARBEITER_INSTITUTION))) {
				// Nur GS darf ein Gesuch sehen, das sich im Status BEARBEITUNG_GS oder FREIGABEQUITTUNG befindet
				predicatesToUse.add(root.get(Gesuch_.status).in(AntragStatus.IN_BEARBEITUNG_GS, AntragStatus.FREIGABEQUITTUNG).not());
			}

			if (institutionstammdatenJoin != null) {
				if (benutzer.getRole().equals(UserRole.ADMIN) || benutzer.getRole().equals(UserRole.SACHBEARBEITER_JA)) {
					final Predicate noSchulamt = cb.notEqual(institutionstammdatenJoin.get(InstitutionStammdaten_.betreuungsangebotTyp), BetreuungsangebotTyp.TAGESSCHULE);
					final Predicate noAngebote = cb.isNull(institutionstammdatenJoin.get(InstitutionStammdaten_.betreuungsangebotTyp));
					predicatesToUse.add(cb.or(noSchulamt, noAngebote));
				}
				if (benutzer.getRole().equals(UserRole.SCHULAMT)) {
					predicatesToUse.add(cb.equal(institutionstammdatenJoin.get(InstitutionStammdaten_.betreuungsangebotTyp), BetreuungsangebotTyp.TAGESSCHULE));
				}
			}
			if (institutionJoin != null) {
				// only if the institutionJoin was set
				if (benutzer.getRole().equals(UserRole.SACHBEARBEITER_TRAEGERSCHAFT)) {
					predicatesToUse.add(cb.equal(institutionJoin.get(Institution_.traegerschaft), benutzer.getTraegerschaft()));
				}
				if (benutzer.getRole().equals(UserRole.SACHBEARBEITER_INSTITUTION)) {
					// es geht hier nicht um die institutionJoin des zugewiesenen benutzers sondern um die institutionJoin des eingeloggten benutzers
					predicatesToUse.add(cb.equal(institutionJoin, benutzer.getInstitution()));
				}
			}

			query.where(CriteriaQueryHelper.concatenateExpressions(cb, predicatesToUse));

			query.orderBy(cb.asc(root.get(Gesuch_.laufnummer)));
			TypedQuery<JaxAntragDTO> q = persistence.getEntityManager().createQuery(query);
			q.setParameter(fallIdParam, fallId);

			return q.getResultList();

		}

		return new ArrayList<>();
	}

	@Override
	@RolesAllowed(value = {UserRoleName.ADMIN, UserRoleName.SUPER_ADMIN})
	@Nonnull
	public List<String> getAllGesuchIDsForFall(String fallId) {
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<String> query = cb.createQuery(String.class);
		Root<Gesuch> root = query.from(Gesuch.class);

		query.select(root.get(Gesuch_.id));

		ParameterExpression<String> fallIdParam = cb.parameter(String.class, "fallId");

		Expression<Boolean> fallPredicate = cb.equal(root.get(Gesuch_.fall).get(AbstractEntity_.id), fallIdParam);
		query.where(fallPredicate);
		TypedQuery<String> q = persistence.getEntityManager().createQuery(query);
		q.setParameter(fallIdParam, fallId);

		return q.getResultList();

	}

	@Override
	@Nonnull
	public List<Gesuch> getAllGesucheForFallAndPeriod(@Nonnull Fall fall, @Nonnull Gesuchsperiode gesuchsperiode) {
		authorizer.checkReadAuthorizationFall(fall);

		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);
		Predicate fallPredicate = cb.equal(root.get(Gesuch_.fall), fall);
		Predicate gesuchsperiodePredicate = cb.equal(root.get(Gesuch_.gesuchsperiode), gesuchsperiode);

		query.where(fallPredicate, gesuchsperiodePredicate);
		return persistence.getCriteriaResults(query);
	}

	@Override
	public Gesuch antragFreigabequittungErstellen(@Nonnull Gesuch gesuch, AntragStatus statusToChangeTo) {
		authorizer.checkWriteAuthorization(gesuch);

		// Zum jetzigen Zeitpunkt muss das Gesuch zwingend in einem kompletten / gueltigen Zustand sein
		validateGesuchComplete(gesuch);

		gesuch.setFreigabeDatum(LocalDate.now());

		if (AntragStatus.FREIGEGEBEN.equals(statusToChangeTo)) {
			gesuch.setEingangsdatum(LocalDate.now()); // Nur wenn das Gesuch direkt freigegeben wird, muessen wir das Eingangsdatum auch setzen
		}

		gesuch.setStatus(statusToChangeTo);

		// Step Freigabe gruen
		wizardStepService.setWizardStepOkay(gesuch.getId(), WizardStepName.FREIGABE);

		return updateGesuch(gesuch, true, null);
	}

	@Nonnull
	@Override
	@RolesAllowed(value = {UserRoleName.ADMIN, UserRoleName.SUPER_ADMIN, UserRoleName.SACHBEARBEITER_JA, UserRoleName.SCHULAMT, UserRoleName.GESUCHSTELLER})
	public Gesuch antragFreigeben(@Nonnull String gesuchId, @Nullable String username) {
		Optional<Gesuch> gesuchOptional = Optional.ofNullable(persistence.find(Gesuch.class, gesuchId)); //direkt ueber persistence da wir eigentlich noch nicht leseberechtigt sind)
		if (gesuchOptional.isPresent()) {
			Gesuch gesuch = gesuchOptional.get();

			// Zum jetzigen Zeitpunkt muss das Gesuch zwingend in einem kompletten / gueltigen Zustand sein
			validateGesuchComplete(gesuch);

			if (!gesuch.getStatus().equals(AntragStatus.FREIGABEQUITTUNG) && !gesuch.getStatus().equals(AntragStatus.IN_BEARBEITUNG_GS)) {
				throw new EbeguRuntimeException("antragFreigeben",
					"Gesuch war im falschen Status: " + gesuch.getStatus() + " wir erwarten aber nur Freigabequittung oder In Bearbeitung GS",
					"Das Gesuch wurde bereits freigegeben");
			}

			this.authorizer.checkWriteAuthorization(gesuch);

			// Die Daten des GS in die entsprechenden Containers kopieren
			FreigabeCopyUtil.copyForFreigabe(gesuch);
			// Je nach Status
			if (!gesuch.getStatus().equals(AntragStatus.FREIGABEQUITTUNG)) {
				// Es handelt sich um eine Mutation ohne Freigabequittung: Wir setzen das Tagesdatum als FreigabeDatum an dem es der Gesuchsteller einreicht
				gesuch.setFreigabeDatum(LocalDate.now());
			}

			// Den Gesuchsstatus setzen
			gesuch.setStatus(calculateFreigegebenStatus(gesuch));

			// Step Freigabe gruen
			wizardStepService.setWizardStepOkay(gesuch.getId(), WizardStepName.FREIGABE);

			// Step Verfuegen gruen, falls NUR_SCHULAMT
			if (AntragStatus.NUR_SCHULAMT.equals(gesuch.getStatus())) {
				wizardStepService.setWizardStepOkay(gesuch.getId(), WizardStepName.VERFUEGEN);
			}

			if (username != null) {
				Optional<Benutzer> currentUser = benutzerService.findBenutzer(username);
				if (currentUser.isPresent() && !currentUser.get().getRole().equals(UserRole.SCHULAMT)) {
					gesuch.getFall().setVerantwortlicher(currentUser.get());
				}
			}

			// Falls es ein OnlineGesuch war: Das Eingangsdatum setzen
			if (Eingangsart.ONLINE.equals(gesuch.getEingangsart())) {
				gesuch.setEingangsdatum(LocalDate.now());
			}

			final Gesuch merged = persistence.merge(gesuch);
			antragStatusHistoryService.saveStatusChange(merged, null);
			return merged;
		} else {
			throw new EbeguEntityNotFoundException("antragFreigeben", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, gesuchId);
		}
	}

	@Override
	@Nonnull
	public Gesuch setBeschwerdeHaengigForPeriode(@Nonnull Gesuch gesuch) {
		final List<Gesuch> allGesucheForFall = getAllGesucheForFallAndPeriod(gesuch.getFall(), gesuch.getGesuchsperiode());
		allGesucheForFall.iterator().forEachRemaining(gesuchLoop -> {
			if (gesuch.equals(gesuchLoop)) {
				gesuchLoop.setStatus(AntragStatus.BESCHWERDE_HAENGIG);
				updateGesuch(gesuchLoop, true, null);
			}
			gesuchLoop.setGesperrtWegenBeschwerde(true); // Flag nicht über Service setzen, da u.U. Gesuch noch inBearbeitungGS
			persistence.merge(gesuchLoop);
		});
		return gesuch;
	}

	@Override
	@Nonnull
	public Gesuch removeBeschwerdeHaengigForPeriode(@Nonnull Gesuch gesuch) {
		final List<Gesuch> allGesucheForFall = getAllGesucheForFallAndPeriod(gesuch.getFall(), gesuch.getGesuchsperiode());
		allGesucheForFall.iterator().forEachRemaining(gesuchLoop -> {
			if (gesuch.equals(gesuchLoop) && AntragStatus.BESCHWERDE_HAENGIG.equals(gesuchLoop.getStatus())) {
				final AntragStatusHistory lastStatusChange = antragStatusHistoryService.findLastStatusChangeBeforeBeschwerde(gesuchLoop);
				gesuchLoop.setStatus(lastStatusChange.getStatus());
				updateGesuch(gesuchLoop, true, null);
			}
			gesuchLoop.setGesperrtWegenBeschwerde(false); // Flag nicht über Service setzen, da u.U. Gesuch noch inBearbeitungGS
			persistence.merge(gesuchLoop);
		});
		return gesuch;
	}

	/**
	 * wenn ein Gesuch nur Schulamt Betreuuungen hat so geht es beim barcode Scannen in den Zustand NUR_SCHULAMTM; sonst Freigegeben
	 */
	private AntragStatus calculateFreigegebenStatus(@Nonnull Gesuch gesuch) {
		if (gesuch.hasOnlyBetreuungenOfSchulamt()) {
			gesuch.setTimestampVerfuegt(LocalDateTime.now());
			gesuch.setGueltig(true);
			return AntragStatus.NUR_SCHULAMT;
		}
		return AntragStatus.FREIGEGEBEN;
	}

	@Override
	@Nonnull
	@RolesAllowed(value = {UserRoleName.ADMIN, UserRoleName.SUPER_ADMIN, UserRoleName.SACHBEARBEITER_JA, UserRoleName.GESUCHSTELLER})
	public Optional<Gesuch> antragMutieren(@Nonnull String antragId, @Nullable LocalDate eingangsdatum) {
		// Mutiert wird immer das Gesuch mit dem letzten Verfügungsdatum
		Optional<Gesuch> gesuchOptional = findGesuch(antragId);
		if (gesuchOptional.isPresent()) {
			Gesuch gesuch = gesuchOptional.get();
			authorizer.checkWriteAuthorization(gesuch);
			if (!isThereAnyOpenMutation(gesuch.getFall(), gesuch.getGesuchsperiode())) {
				authorizer.checkReadAuthorization(gesuch);
				Optional<Gesuch> gesuchForMutationOpt = getNeustesVerfuegtesGesuchFuerGesuch(gesuch.getGesuchsperiode(), gesuch.getFall(), true);
				Gesuch gesuchForMutation = gesuchForMutationOpt.orElseThrow(() -> new EbeguEntityNotFoundException("antragMutieren", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, "Kein Verfuegtes Gesuch fuer ID " + antragId));
				return getGesuchMutation(eingangsdatum, gesuchForMutation);
			} else {
				throw new EbeguExistingAntragException("antragMutieren", ErrorCodeEnum.ERROR_EXISTING_ONLINE_MUTATION,
					gesuch.getFall().getId(), gesuch.getGesuchsperiode().getId());
			}
		} else {
			throw new EbeguEntityNotFoundException("antragMutieren", "Es existiert kein Antrag mit ID, kann keine Mutation erstellen " + antragId, antragId);
		}
	}

	@Override
	@Nonnull
	@RolesAllowed(value = {UserRoleName.SUPER_ADMIN})
	public Optional<Gesuch> antragMutieren(@Nonnull Long fallNummer, @Nonnull String gesuchsperiodeId,
										   @Nonnull LocalDate eingangsdatum) {
		// Mutiert wird immer das Gesuch mit dem letzten Verfügungsdatum
		final Optional<Fall> fall = fallService.findFallByNumber(fallNummer);
		final Optional<Gesuchsperiode> gesuchsperiode = gesuchsperiodeService.findGesuchsperiode(gesuchsperiodeId);

		if (fall.isPresent() && gesuchsperiode.isPresent()) {
			if (!isThereAnyOpenMutation(fall.get(), gesuchsperiode.get())) {
				Optional<Gesuch> gesuchForMutationOpt = getNeustesVerfuegtesGesuchFuerGesuch(gesuchsperiode.get(), fall.get(), true);
				Gesuch gesuchForMutation = gesuchForMutationOpt.orElseThrow(() -> new EbeguEntityNotFoundException("antragMutieren", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, "Kein Verfuegtes Gesuch fuer Fallnummer " + fallNummer));
				return getGesuchMutation(eingangsdatum, gesuchForMutation);
			} else {
				throw new EbeguExistingAntragException("antragMutieren", ErrorCodeEnum.ERROR_EXISTING_ONLINE_MUTATION, fall.get().getId(),
					gesuchsperiodeId);
			}
		} else {
			throw new EbeguEntityNotFoundException("antragMutieren", "fall oder gesuchsperiode konnte nicht geladen werden  fallNr:" + fallNummer + "gsPerID" + gesuchsperiodeId);
		}
	}

	private boolean isThereAnyOpenMutation(@Nonnull Fall fall, @Nonnull Gesuchsperiode gesuchsperiode) {
		List<Gesuch> criteriaResults = findExistingOpenMutationen(fall, gesuchsperiode);
		return !criteriaResults.isEmpty();
	}

	/**
	 * Diese Methode gibt eine Liste zurueck. Diese Liste sollte aber maximal eine Mutation enthalten, da es unmoeglich ist,
	 * mehrere offene Mutationen fuer dieselbe Gesuchsperiode zu haben. Rechte werden nicht beruecksichtigt d.h. alle
	 * Gesuche werden geguckt und daher die richtige letzte Mutation wird zurueckgegeben.
	 */
	private List<Gesuch> findExistingOpenMutationen(@Nonnull Fall fall, @Nonnull Gesuchsperiode gesuchsperiode) {
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);

		Predicate predicateMutation = root.get(Gesuch_.typ).in(AntragTyp.MUTATION);
		Predicate predicateStatus = root.get(Gesuch_.status).in(AntragStatus.getAllVerfuegtStates()).not();
		Predicate predicateGesuchsperiode = cb.equal(root.get(Gesuch_.gesuchsperiode), gesuchsperiode);
		Predicate predicateFall = cb.equal(root.get(Gesuch_.fall), fall);

		query.where(predicateMutation, predicateStatus, predicateGesuchsperiode, predicateFall);
		query.select(root);
		return persistence.getCriteriaResults(query);
	}

	private List<Gesuch> findExistingFolgegesuch(@Nonnull Fall fall, @Nonnull Gesuchsperiode gesuchsperiode) {
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);

		Predicate predicateMutation = root.get(Gesuch_.typ).in(AntragTyp.ERNEUERUNGSGESUCH);
		Predicate predicateStatus = root.get(Gesuch_.status).in(AntragStatus.getInBearbeitungGSStates());
		Predicate predicateGesuchsperiode = cb.equal(root.get(Gesuch_.gesuchsperiode), gesuchsperiode);
		Predicate predicateFall = cb.equal(root.get(Gesuch_.fall), fall);

		query.where(predicateMutation, predicateStatus, predicateGesuchsperiode, predicateFall);
		query.select(root);
		return persistence.getCriteriaResults(query);
	}

	private Optional<Gesuch> getGesuchMutation(@Nullable LocalDate eingangsdatum, @Nonnull Gesuch gesuchForMutation) {
		Eingangsart eingangsart = calculateEingangsart();
		Gesuch mutation = gesuchForMutation.copyForMutation(new Gesuch(), eingangsart);
		if (eingangsdatum != null) {
			mutation.setEingangsdatum(eingangsdatum);
		}
		return Optional.of(mutation);
	}

	@Nonnull
	private Eingangsart calculateEingangsart() {
		Eingangsart eingangsart;
		if (this.principalBean.isCallerInRole(UserRole.GESUCHSTELLER)) {
			eingangsart = Eingangsart.ONLINE;
		} else {
			eingangsart = Eingangsart.PAPIER;
		}
		return eingangsart;
	}

	@Override
	@Nonnull
	@RolesAllowed(value = {UserRoleName.ADMIN, UserRoleName.SUPER_ADMIN, UserRoleName.SACHBEARBEITER_JA, UserRoleName.GESUCHSTELLER})
	public Optional<Gesuch> antragErneuern(@Nonnull String antragId, @Nonnull String gesuchsperiodeId, @Nullable LocalDate eingangsdatum) {
		Gesuchsperiode gesuchsperiode = gesuchsperiodeService.findGesuchsperiode(gesuchsperiodeId).orElseThrow(() -> new EbeguEntityNotFoundException("findGesuchsperiode", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, gesuchsperiodeId));
		Gesuch gesuch = findGesuch(antragId).orElseThrow(() -> new EbeguEntityNotFoundException("antragErneuern", "Es existiert kein Antrag mit ID, kann kein Erneuerungsgesuch erstellen " + antragId, antragId));
		List<Gesuch> allGesucheForFallAndPeriod = getAllGesucheForFallAndPeriod(gesuch.getFall(), gesuchsperiode);
		if (allGesucheForFallAndPeriod.isEmpty()) {
			authorizer.checkWriteAuthorization(gesuch);
			Optional<Gesuch> gesuchForErneuerungOpt = getGesuchFuerErneuerungsantrag(gesuch.getFall());
			Gesuch gesuchForErneuerung = gesuchForErneuerungOpt.orElseThrow(() -> new EbeguEntityNotFoundException("antragErneuern", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, "Kein Verfuegtes Gesuch fuer ID " + antragId));
			return getErneuerungsgesuch(eingangsdatum, gesuchForErneuerung, gesuchsperiode);
		} else {
			// must have the gesuchsperiodeID as first item in the arguments list
			throw new EbeguExistingAntragException("antragErneuern", ErrorCodeEnum.ERROR_EXISTING_ERNEUERUNGSGESUCH,
				gesuch.getFall().getId(), gesuchsperiodeId);
		}
	}

	private Optional<Gesuch> getErneuerungsgesuch(@Nullable LocalDate eingangsdatum, @Nonnull Gesuch gesuchForErneuerung, @Nonnull Gesuchsperiode gesuchsperiode) {
		Eingangsart eingangsart = calculateEingangsart();
		Gesuch erneuerungsgesuch = gesuchForErneuerung.copyForErneuerung(new Gesuch(), gesuchsperiode, eingangsart);
		if (eingangsdatum != null) {
			erneuerungsgesuch.setEingangsdatum(eingangsdatum);
		}
		return Optional.of(erneuerungsgesuch);
	}

	@Nonnull
	private Optional<Gesuch> getNeustesVerfuegtesGesuchFuerGesuch(Gesuchsperiode gesuchsperiode, Fall fall, boolean doAuthCheck) {
		authorizer.checkReadAuthorizationFall(fall);
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);

		Predicate predicateStatus = root.get(Gesuch_.status).in(AntragStatus.getAllVerfuegtStates());
		Predicate predicateGesuchsperiode = cb.equal(root.get(Gesuch_.gesuchsperiode), gesuchsperiode);
		Predicate predicateFall = cb.equal(root.get(Gesuch_.fall), fall);
		Predicate predicateGueltig = cb.equal(root.get(Gesuch_.gueltig), Boolean.TRUE);

		query.where(predicateStatus, predicateGesuchsperiode, predicateGueltig, predicateFall);
		query.select(root);
		query.orderBy(cb.desc(root.get(Gesuch_.timestampVerfuegt))); // Das mit dem neuesten Verfuegungsdatum
		List<Gesuch> criteriaResults = persistence.getCriteriaResults(query, 1);
		if (criteriaResults.isEmpty()) {
			return Optional.empty();
		}
		Gesuch gesuch = criteriaResults.get(0);
		if (doAuthCheck) {
			authorizer.checkReadAuthorization(gesuch);
		}
		return Optional.of(gesuch);
	}

	@Override
	@Nonnull
	@PermitAll
	public Optional<Gesuch> getNeustesGesuchFuerGesuch(@Nonnull Gesuch gesuch) {
		authorizer.checkReadAuthorization(gesuch);
		return getNeustesGesuchFuerGesuch(gesuch.getGesuchsperiode(), gesuch.getFall(), true);
	}

	@Override
	@PermitAll
	public boolean isNeustesGesuch(@Nonnull Gesuch gesuch) {
		final Optional<Gesuch> neustesGesuchFuerGesuch = getNeustesGesuchFuerGesuch(gesuch.getGesuchsperiode(), gesuch.getFall(), false);
		return neustesGesuchFuerGesuch.isPresent() && Objects.equals(neustesGesuchFuerGesuch.get().getId(), gesuch.getId());
	}

	/**
	 * Da es eine private Methode ist, ist es sicher, als Parameter zu fragen, ob man nach ReadAuthorization pruefen muss.
	 * Das Interface sollte aber diese Moeglichkeit nur versteckt durch bestimmte Methoden anbieten.
	 */
	@Nonnull
	private Optional<Gesuch> getNeustesGesuchFuerGesuch(@Nonnull Gesuchsperiode gesuchsperiode, @Nonnull Fall fall, boolean checkReadAuthorization) {
		authorizer.checkReadAuthorizationFall(fall);
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);
		Predicate predicateGesuchsperiode = cb.equal(root.get(Gesuch_.gesuchsperiode), gesuchsperiode);
		Predicate predicateFall = cb.equal(root.get(Gesuch_.fall), fall);

		query.where(predicateGesuchsperiode, predicateFall);
		query.select(root);
		query.orderBy(cb.desc(root.get(Gesuch_.timestampErstellt)));
		List<Gesuch> criteriaResults = persistence.getCriteriaResults(query, 1);
		if (criteriaResults.isEmpty()) {
			return Optional.empty();
		}
		Gesuch gesuch = criteriaResults.get(0);
		if (checkReadAuthorization) {
			authorizer.checkReadAuthorization(gesuch);
		}
		return Optional.of(gesuch);
	}

	@Nonnull
	private Optional<Gesuch> getGesuchFuerErneuerungsantrag(@Nonnull Fall fall) {
		authorizer.checkReadAuthorizationFall(fall);
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);
		Predicate predicateFall = cb.equal(root.get(Gesuch_.fall), fall);

		query.where(predicateFall);
		query.select(root);
		query.orderBy(cb.desc(root.get(Gesuch_.timestampErstellt)));
		List<Gesuch> criteriaResults = persistence.getCriteriaResults(query, 1);
		if (criteriaResults.isEmpty()) {
			return Optional.empty();
		}
		Gesuch gesuch = criteriaResults.get(0);
		return Optional.of(gesuch);
	}

	private List<String> determineDistinctGesuchIdsToLoad(List<String> allGesuchIds, int startindex, int maxresults) {
		List<String> uniqueGesuchIds = new ArrayList<>(new LinkedHashSet<>(allGesuchIds)); //keep order but remove duplicate ids
		int lastindex = Math.min(startindex + maxresults, (uniqueGesuchIds.size()));
		return uniqueGesuchIds.subList(startindex, lastindex);
	}

	private List<Gesuch> findGesuche(@Nonnull List<String> gesuchIds) {
		if (!gesuchIds.isEmpty()) {

			final CriteriaBuilder cb = persistence.getCriteriaBuilder();
			final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);
			Root<Gesuch> root = query.from(Gesuch.class);
			Predicate predicate = root.get(Gesuch_.id).in(gesuchIds);
			Fetch<Gesuch, KindContainer> kindContainers = root.fetch(Gesuch_.kindContainers, JoinType.LEFT);
			kindContainers.fetch(KindContainer_.betreuungen, JoinType.LEFT);
			query.where(predicate);
			//reduce to unique gesuche
			List<Gesuch> listWithDuplicates = persistence.getCriteriaResults(query);
			LinkedHashSet<Gesuch> set = new LinkedHashSet<>();
			//richtige reihenfolge beibehalten
			for (String gesuchId : gesuchIds) {
				listWithDuplicates.stream()
					.filter(gesuch -> gesuch.getId().equals(gesuchId))
					.findFirst()
					.ifPresent(set::add);
			}

			return new ArrayList<>(set);

		} else {
			return Collections.emptyList();
		}
	}

	private String[] ensureYearFormat(String gesuchsperiodeString) {
		String[] years = gesuchsperiodeString.split("/");
		if (years.length != 2) {
			throw new EbeguRuntimeException("searchAntraege", "Der Gesuchsperioden string war nicht im erwarteten Format x/y sondern " + gesuchsperiodeString);
		}
		String[] result = new String[2];
		result[0] = changeTwoDigitYearToFourDigit(years[0]);
		result[1] = changeTwoDigitYearToFourDigit(years[1]);
		return result;
	}

	private String changeTwoDigitYearToFourDigit(String year) {
		//im folgenden wandeln wir z.B 16  in 2016 um. Funktioniert bis ins jahr 2099, da die Periode 2099/2100 mit dieser Methode nicht geht
		String currentYearAsString = String.valueOf(LocalDate.now().getYear());
		if (year.length() == currentYearAsString.length()) {
			return year;
		} else if (year.length() < currentYearAsString.length()) { // jahr ist im kurzformat
			return currentYearAsString.substring(0, currentYearAsString.length() - year.length()) + year;
		} else {
			throw new EbeguRuntimeException("searchAntraege", "Der Gesuchsperioden string war nicht im erwarteten Format yy oder yyyy sondern " + year);
		}
	}

	@Override
	@Nonnull
	@Deprecated //TODO (hefr) Nur noch für die Migaration V0040 gebraucht! Umbenennen? Name stimmt sowieso nicht (freigegeben)
	public Optional<String> getNeustesFreigegebenesGesuchIdFuerGesuch(Gesuchsperiode gesuchsperiode, Fall fall) {
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<String> query = cb.createQuery(String.class);

		Root<Gesuch> root = query.from(Gesuch.class);
		Join<Gesuch, AntragStatusHistory> join = root.join(Gesuch_.antragStatusHistories, JoinType.INNER);

		Predicate predicateStatus = root.get(Gesuch_.status).in(AntragStatus.getAllVerfuegtStates());
		Predicate predicateGesuchsperiode = cb.equal(root.get(Gesuch_.gesuchsperiode), gesuchsperiode);
		Predicate predicateAntragStatus = join.get(AntragStatusHistory_.status).in(AntragStatus.FIRST_STATUS_OF_VERFUEGT);
		Predicate predicateFall = cb.equal(root.get(Gesuch_.fall), fall);

		query.where(predicateStatus, predicateGesuchsperiode, predicateAntragStatus, predicateFall);
		query.select(root.get(Gesuch_.id));
		query.orderBy(cb.desc(join.get(AntragStatusHistory_.timestampVon))); // Das mit dem neuesten Verfuegungsdatum
		List<String> criteriaResults = persistence.getCriteriaResults(query, 1);
		if (criteriaResults.isEmpty()) {
			return Optional.<String>empty();
		}
		String gesuchId = criteriaResults.get(0);

		return Optional.of(gesuchId);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@RolesAllowed(value = {UserRoleName.SUPER_ADMIN})
	public int warnGesuchNichtFreigegeben() {

		Integer anzahlTageBisWarnungFreigabe = applicationPropertyService.findApplicationPropertyAsInteger(ApplicationPropertyKey.ANZAHL_TAGE_BIS_WARNUNG_FREIGABE);
		Integer anzahlTageBisLoeschungNachWarnungFreigabe = applicationPropertyService.findApplicationPropertyAsInteger(ApplicationPropertyKey.ANZAHL_TAGE_BIS_LOESCHUNG_NACH_WARNUNG_FREIGABE);
		if (anzahlTageBisWarnungFreigabe == null || anzahlTageBisLoeschungNachWarnungFreigabe == null) {
			throw new EbeguRuntimeException("warnGesuchNichtFreigegeben",
				ApplicationPropertyKey.ANZAHL_TAGE_BIS_WARNUNG_FREIGABE.name() + " or " +
					ApplicationPropertyKey.ANZAHL_TAGE_BIS_LOESCHUNG_NACH_WARNUNG_FREIGABE.name() + " not defined");
		}

		// Stichtag ist EndeTag -> Plus 1 Tag und dann less statt lessOrEqual
		LocalDateTime stichtag = LocalDate.now().minusDays(anzahlTageBisWarnungFreigabe).atStartOfDay().plusDays(1);

		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);
		// Status in Bearbeitung GS
		Predicate predicateStatus = cb.equal(root.get(Gesuch_.status), AntragStatus.IN_BEARBEITUNG_GS);
		// Irgendwann am Stichtag erstellt:
		Predicate predicateDatum = cb.lessThan(root.get(Gesuch_.timestampErstellt), stichtag);
		// Noch nicht gewarnt
		Predicate predicateNochNichtGewarnt = cb.isNull(root.get(Gesuch_.datumGewarntNichtFreigegeben));

		query.where(predicateStatus, predicateDatum, predicateNochNichtGewarnt);
		query.select(root);
		query.orderBy(cb.desc(root.get(Gesuch_.timestampErstellt)));
		List<Gesuch> gesucheNichtAbgeschlossenSeit = persistence.getCriteriaResults(query);

		int anzahl = gesucheNichtAbgeschlossenSeit.size();
		for (Gesuch gesuch : gesucheNichtAbgeschlossenSeit) {
			try {
				mailService.sendWarnungGesuchNichtFreigegeben(gesuch, anzahlTageBisLoeschungNachWarnungFreigabe);
				gesuch.setDatumGewarntNichtFreigegeben(LocalDate.now());
				updateGesuch(gesuch, false, null);
			} catch (MailException e) {
				LOG.error("Mail WarnungGesuchNichtFreigegeben konnte nicht verschickt werden fuer Gesuch " + gesuch.getId(), e);
				anzahl--;
			}
		}
		return anzahl;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@RolesAllowed(value = {UserRoleName.SUPER_ADMIN})
	public int warnFreigabequittungFehlt() {

		Integer anzahlTageBisWarnungQuittung = applicationPropertyService.findApplicationPropertyAsInteger(ApplicationPropertyKey.ANZAHL_TAGE_BIS_WARNUNG_QUITTUNG);
		Integer anzahlTageBisLoeschungNachWarnungFreigabe = applicationPropertyService.findApplicationPropertyAsInteger(ApplicationPropertyKey.ANZAHL_TAGE_BIS_LOESCHUNG_NACH_WARNUNG_QUITTUNG);
		if (anzahlTageBisWarnungQuittung == null) {
			throw new EbeguRuntimeException("warnFreigabequittungFehlt", ApplicationPropertyKey.ANZAHL_TAGE_BIS_WARNUNG_QUITTUNG.name() + " not defined");
		}
		if (anzahlTageBisLoeschungNachWarnungFreigabe == null) {
			throw new EbeguRuntimeException("warnFreigabequittungFehlt", ApplicationPropertyKey.ANZAHL_TAGE_BIS_LOESCHUNG_NACH_WARNUNG_QUITTUNG.name() + " not defined");
		}

		LocalDate stichtag = LocalDate.now().minusDays(anzahlTageBisWarnungQuittung);

		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);
		Predicate predicateStatus = cb.equal(root.get(Gesuch_.status), AntragStatus.FREIGABEQUITTUNG);
		Predicate predicateDatum = cb.lessThanOrEqualTo(root.get(Gesuch_.freigabeDatum), stichtag);
		// Noch nicht gewarnt
		Predicate predicateNochNichtGewarnt = cb.isNull(root.get(Gesuch_.datumGewarntFehlendeQuittung));

		query.where(predicateStatus, predicateDatum, predicateNochNichtGewarnt);
		query.select(root);
		query.orderBy(cb.desc(root.get(Gesuch_.timestampErstellt)));
		List<Gesuch> gesucheNichtAbgeschlossenSeit = persistence.getCriteriaResults(query);

		int anzahl = gesucheNichtAbgeschlossenSeit.size();
		for (Gesuch gesuch : gesucheNichtAbgeschlossenSeit) {
			try {
				mailService.sendWarnungFreigabequittungFehlt(gesuch, anzahlTageBisLoeschungNachWarnungFreigabe);
				gesuch.setDatumGewarntFehlendeQuittung(LocalDate.now());
				updateGesuch(gesuch, false, null);
			} catch (MailException e) {
				LOG.error("Mail WarnungFreigabequittungFehlt konnte nicht verschickt werden fuer Gesuch " + gesuch.getId(), e);
				anzahl--;
			}
		}
		return anzahl;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@RolesAllowed(value = {UserRoleName.SUPER_ADMIN})
	public int deleteGesucheOhneFreigabeOderQuittung() {

		Integer anzahlTageBisLoeschungNachWarnungFreigabe = applicationPropertyService.findApplicationPropertyAsInteger(ApplicationPropertyKey.ANZAHL_TAGE_BIS_LOESCHUNG_NACH_WARNUNG_FREIGABE);
		Integer anzahlTageBisLoeschungNachWarnungQuittung = applicationPropertyService.findApplicationPropertyAsInteger(ApplicationPropertyKey.ANZAHL_TAGE_BIS_LOESCHUNG_NACH_WARNUNG_QUITTUNG);
		if (anzahlTageBisLoeschungNachWarnungFreigabe == null || anzahlTageBisLoeschungNachWarnungQuittung == null) {
			throw new EbeguRuntimeException("warnGesuchNichtFreigegeben",
				ApplicationPropertyKey.ANZAHL_TAGE_BIS_LOESCHUNG_NACH_WARNUNG_FREIGABE.name() + " or " +
					ApplicationPropertyKey.ANZAHL_TAGE_BIS_LOESCHUNG_NACH_WARNUNG_QUITTUNG.name() + " not defined");
		}

		// Stichtag ist EndeTag -> Plus 1 Tag und dann less statt lessOrEqual
		LocalDate stichtagFehlendeFreigabe = LocalDate.now()
			.minusDays(anzahlTageBisLoeschungNachWarnungFreigabe).plusDays(1);
		LocalDate stichtagFehlendeQuittung = LocalDate.now()
			.minusDays(anzahlTageBisLoeschungNachWarnungQuittung);

		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);

		// Entweder IN_BEARBEITUNG_GS und vor stichtagFehlendeFreigabe erstellt
		Predicate predicateStatusNichtFreigegeben = cb.equal(root.get(Gesuch_.status), AntragStatus.IN_BEARBEITUNG_GS);
		Predicate predicateGewarntNichtFreigegeben = cb.isNotNull(root.get(Gesuch_.datumGewarntNichtFreigegeben));
		Predicate predicateDatumNichtFreigegeben = cb.lessThan(root.get(Gesuch_.datumGewarntNichtFreigegeben), stichtagFehlendeFreigabe);
		Predicate predicateNichtFreigegeben = cb.and(predicateStatusNichtFreigegeben, predicateDatumNichtFreigegeben, predicateGewarntNichtFreigegeben);

		// Oder FREIGABEQUITTUNG und vor stichtagFehlendeQuittung freigegeben
		Predicate predicateStatusFehlendeQuittung = cb.equal(root.get(Gesuch_.status), AntragStatus.FREIGABEQUITTUNG);
		Predicate predicateGewarntFehlendeQuittung = cb.isNotNull(root.get(Gesuch_.datumGewarntFehlendeQuittung));
		Predicate predicateDatumFehlendeQuittung = cb.lessThanOrEqualTo(root.get(Gesuch_.datumGewarntFehlendeQuittung), stichtagFehlendeQuittung);

		Predicate predicateFehlendeQuittung = cb.and(predicateStatusFehlendeQuittung, predicateDatumFehlendeQuittung, predicateGewarntFehlendeQuittung);

		Predicate predicateFehlendeFreigabeOrQuittung = cb.or(predicateNichtFreigegeben, predicateFehlendeQuittung);

		query.where(predicateFehlendeFreigabeOrQuittung);
		query.select(root);
		query.orderBy(cb.desc(root.get(Gesuch_.timestampErstellt)));

		List<Gesuch> criteriaResults = persistence.getCriteriaResults(query);
		int anzahl = criteriaResults.size();
		for (Gesuch gesuch : criteriaResults) {
			try {
				mailService.sendInfoGesuchGeloescht(gesuch);
				removeGesuch(gesuch.getId());
			} catch (MailException e) {
				LOG.error("Mail InfoGesuchGeloescht konnte nicht verschickt werden fuer Gesuch " + gesuch.getId(), e);
				anzahl--;
			}
		}
		return anzahl;
	}

	@Override
	public boolean canGesuchsperiodeBeClosed(@Nonnull Gesuchsperiode gesuchsperiode) {
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);

		// Status verfuegt
		Predicate predicateStatus = root.get(Gesuch_.status).in(AntragStatus.ERLEDIGTE_PENDENZ).not();
		// Gesuchsperiode
		final Predicate predicateGesuchsperiode = cb.equal(root.get(Gesuch_.gesuchsperiode), gesuchsperiode);

		query.where(predicateStatus, predicateGesuchsperiode);
		query.select(root);
		List<Gesuch> criteriaResults = persistence.getCriteriaResults(query);
		return criteriaResults.isEmpty();
	}

	@Override
	@RolesAllowed(value = {UserRoleName.ADMIN, UserRoleName.SUPER_ADMIN})
	public void removeOnlineMutation(@Nonnull Fall fall, @Nonnull Gesuchsperiode gesuchsperiode) {
		logDeletingOfGesuchstellerAntrag(fall, gesuchsperiode);
		List<Gesuch> criteriaResults = findExistingOpenMutationen(fall, gesuchsperiode);
		if (criteriaResults.size() > 1) {
			// It should be impossible that there are more than one open Mutation
			throw new EbeguRuntimeException("removeOnlineMutation", ErrorCodeEnum.ERROR_TOO_MANY_RESULTS);
		}
		if (criteriaResults.size() <= 0) {
			throw new EbeguEntityNotFoundException("removeOnlineMutation", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND);
		}
		final Gesuch onlineMutation = criteriaResults.get(0);
		moveBetreuungmitteilungenToPreviousAntrag(onlineMutation);
		superAdminService.removeGesuch(onlineMutation.getId());
	}

	/**
	 * Takes all Betreuungsmitteilungen of the given Gesuch and links them to the previous corresponding Betreuung (vorgaengerId),
	 * so that the mitteilungen don't get lost.
	 * @param onlineMutation
	 */
	private void moveBetreuungmitteilungenToPreviousAntrag(@Nonnull Gesuch onlineMutation) {
		if (onlineMutation.hasVorgaenger()) {
			for (Betreuung betreuung : onlineMutation.extractAllBetreuungen()) {
				if (betreuung.hasVorgaenger()) {
					final Optional<Betreuung> optVorgaengerBetreuung = betreuungService.findBetreuung(betreuung.getVorgaengerId());
					final Betreuung vorgaengerBetreuung = optVorgaengerBetreuung
						.orElseThrow(() -> new EbeguEntityNotFoundException("moveBetreuungmitteilungenToPreviousAntrag", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, betreuung.getVorgaengerId()));

					final Collection<Betreuungsmitteilung> mitteilungen = mitteilungService.findAllBetreuungsmitteilungenForBetreuung(betreuung);
					mitteilungen.forEach(mitteilung -> mitteilung.setBetreuung(vorgaengerBetreuung)); // should be saved automatically
				}
			}
		} else {
			throw new EbeguEntityNotFoundException("moveBetreuungmitteilungenToPreviousAntrag", ErrorCodeEnum.ERROR_VORGAENGER_MISSING, onlineMutation.getId());
		}
	}

	@Override
	@RolesAllowed(value = {UserRoleName.ADMIN, UserRoleName.SUPER_ADMIN})
	public void removeOnlineFolgegesuch(@Nonnull Fall fall, @Nonnull Gesuchsperiode gesuchsperiode) {
		logDeletingOfGesuchstellerAntrag(fall, gesuchsperiode);
		List<Gesuch> criteriaResults = findExistingFolgegesuch(fall, gesuchsperiode);
		if (criteriaResults.size() > 1) {
			// It should be impossible that there are more than one open Folgegesuch for one period
			throw new EbeguRuntimeException("removeOnlineFolgegesuch", ErrorCodeEnum.ERROR_TOO_MANY_RESULTS);
		}
		superAdminService.removeGesuch(criteriaResults.get(0).getId());
	}

	@Override
	public Gesuch closeWithoutAngebot(@Nonnull Gesuch gesuch) {
		if (gesuch.getStatus() != AntragStatus.GEPRUEFT) {
			throw new EbeguRuntimeException("closeWithoutAngebot", ErrorCodeEnum.ERROR_ONLY_IN_GEPRUEFT_ALLOWED);
		}
		if (!gesuch.extractAllBetreuungen().isEmpty()) {
			throw new EbeguRuntimeException("closeWithoutAngebot", ErrorCodeEnum.ERROR_ONLY_IF_NO_BETERUUNG);
		}
		gesuch.setStatus(AntragStatus.KEIN_ANGEBOT);
		postGesuchVerfuegen(gesuch);
		wizardStepService.setWizardStepOkay(gesuch.getId(), WizardStepName.VERFUEGEN);
		Gesuch persistedGesuch = updateGesuch(gesuch, true, null);
		// Das Dokument der Finanziellen Situation erstellen
		try {
			generatedDokumentService.getFinSitDokumentAccessTokenGeneratedDokument(persistedGesuch, true);
		} catch (MimeTypeParseException | MergeDocException e) {
			throw new EbeguRuntimeException("closeWithoutAngebot", "FinSit-Dokument konnte nicht erstellt werden"
				+ persistedGesuch.getId(), e);
		}
		return persistedGesuch;
	}

	@Override
	public Gesuch verfuegenStarten(@Nonnull Gesuch gesuch) {
		if (gesuch.getStatus() != AntragStatus.GEPRUEFT) {
			throw new EbeguRuntimeException("verfuegenStarten", ErrorCodeEnum.ERROR_ONLY_IN_GEPRUEFT_ALLOWED);
		}
		// Zum jetzigen Zeitpunkt muss das Gesuch zwingend in einem kompletten / gueltigen Zustand sein
		validateGesuchComplete(gesuch);

		if (gesuch.hasOnlyBetreuungenOfSchulamt()) {
			gesuch.setStatus(AntragStatus.NUR_SCHULAMT);
			postGesuchVerfuegen(gesuch);
		} else {
			gesuch.setStatus(AntragStatus.VERFUEGEN);
		}

		// In Fall von NUR_SCHULAMT-Angeboten werden diese nicht verfügt, d.h. ab "Verfügen starten"
		// sind diese Betreuungen die letzt gültigen
		final List<Betreuung> betreuungen = gesuch.extractAllBetreuungen();
		for (Betreuung betreuung : betreuungen) {
			if (betreuung.getInstitutionStammdaten().getBetreuungsangebotTyp().isSchulamt()) {
				betreuung.setGueltig(true);
				if (betreuung.getVorgaengerId() != null) {
					Optional<Betreuung> vorgaengerBetreuungOptional = betreuungService.findBetreuung(betreuung.getVorgaengerId(), false);
					vorgaengerBetreuungOptional.ifPresent(vorgaenger -> vorgaenger.setGueltig(false));
				}
			}
		}

		Gesuch persistedGesuch = superAdminService.updateGesuch(gesuch, true, principalBean.getBenutzer());
		// Das Dokument der Finanziellen Situation erstellen
		try {
			generatedDokumentService.getFinSitDokumentAccessTokenGeneratedDokument(persistedGesuch, true);
		} catch (MimeTypeParseException | MergeDocException e) {
			throw new EbeguRuntimeException("verfuegenStarten", "FinSit-Dokument konnte nicht erstellt werden"
				+ persistedGesuch.getId(), e);
		}
		return persistedGesuch;
	}

	private void validateGesuchComplete(@Nonnull Gesuch gesuch) {
		// Gesamt-Validierung durchführen
		Validator validator = Validation.byDefaultProvider().configure().buildValidatorFactory().getValidator();
		Set<ConstraintViolation<Gesuch>> constraintViolations = validator.validate(gesuch, AntragCompleteValidationGroup.class);
		if (!constraintViolations.isEmpty()) {
			throw new EbeguRuntimeException("verfuegenStarten", ErrorCodeEnum.ERROR_ANTRAG_NOT_COMPLETE);
		}
	}

	@Override
	public void postGesuchVerfuegen(@Nonnull Gesuch gesuch) {
		Optional<Gesuch> neustesVerfuegtesGesuchFuerGesuch = getNeustesVerfuegtesGesuchFuerGesuch(gesuch.getGesuchsperiode(), gesuch.getFall(), false);
		if (AntragStatus.FIRST_STATUS_OF_VERFUEGT.contains(gesuch.getStatus()) && gesuch.getTimestampVerfuegt() == null) {
			// Status ist neuerdings verfuegt, aber das Datum noch nicht gesetzt -> dies war der Statuswechsel
			gesuch.setTimestampVerfuegt(LocalDateTime.now());
			gesuch.setGueltig(true);
			if (neustesVerfuegtesGesuchFuerGesuch.isPresent() && !neustesVerfuegtesGesuchFuerGesuch.get().getId().equals(gesuch.getId())) {
				neustesVerfuegtesGesuchFuerGesuch.get().setGueltig(false);
				updateGesuch(neustesVerfuegtesGesuchFuerGesuch.get(), false, null, false);
			}
		}
	}

	@Override
	@Asynchronous
	public void sendMailsToAllGesuchstellerOfLastGesuchsperiode(@Nonnull Gesuchsperiode lastGesuchsperiode, @Nonnull Gesuchsperiode nextGesuchsperiode) {
		List<Gesuch> antraegeOfLastYear = new ArrayList<>();
		Collection<Fall> allFaelle = fallService.getAllFalle(true);
		for (Fall fall : allFaelle) {
			Optional<Gesuch> idsFuerGesuch = getNeuestesGesuchForFallAndPeriod(fall, lastGesuchsperiode);
			idsFuerGesuch.ifPresent(antraegeOfLastYear::add);
		}
		mailService.sendInfoFreischaltungGesuchsperiode(nextGesuchsperiode, antraegeOfLastYear);
	}

	@Nonnull
	private Optional<Gesuch> getNeuestesGesuchForFallAndPeriod(@Nonnull Fall fall, @Nonnull Gesuchsperiode gesuchsperiode) {
		authorizer.checkReadAuthorizationFall(fall);

		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Gesuch> query = cb.createQuery(Gesuch.class);

		Root<Gesuch> root = query.from(Gesuch.class);
		Predicate fallPredicate = cb.equal(root.get(Gesuch_.fall), fall);
		Predicate gesuchsperiodePredicate = cb.equal(root.get(Gesuch_.gesuchsperiode), gesuchsperiode);

		query.where(fallPredicate, gesuchsperiodePredicate);
		query.orderBy(cb.desc(root.get(Gesuch_.timestampErstellt)));
		List<Gesuch> criteriaResults = persistence.getCriteriaResults(query, 1);
		if (criteriaResults.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(criteriaResults.get(0));
	}

	private void logDeletingOfGesuchstellerAntrag(@Nonnull Fall fall, @Nonnull Gesuchsperiode gesuchsperiode) {
		LOG.info("****************************************************");
		LOG.info("Online Gesuch wird gelöscht:");
		LOG.info("Benutzer: " + principalBean.getBenutzer().getUsername());
		LOG.info("Fall: " + fall.getFallNummer());
		LOG.info("Gesuchsperiode: " + gesuchsperiode.getGesuchsperiodeString());
		LOG.info("****************************************************");
	}
}


