package ch.dvbern.ebegu.api.converter;

import ch.dvbern.ebegu.api.dtos.*;
import ch.dvbern.ebegu.entities.*;
import ch.dvbern.ebegu.enums.ErrorCodeEnum;
import ch.dvbern.ebegu.errors.EbeguEntityNotFoundException;
import ch.dvbern.ebegu.services.*;
import ch.dvbern.ebegu.types.DateRange;
import ch.dvbern.ebegu.util.Constants;
import ch.dvbern.lib.beanvalidation.embeddables.IBAN;
import ch.dvbern.lib.date.DateConvertUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;


@Dependent
@SuppressWarnings({"PMD.NcssTypeCount", "unused"})
public class JaxBConverter {

	@Inject
	private GesuchstellerService gesuchstellerService;

	@Inject
	private AdresseService adresseService;
	@Inject
	private PensumFachstelleService pensumFachstelleService;
	@Inject
	private FachstelleService fachstelleService;
	@Inject
	private GesuchService gesuchService;
	@Inject
	private FinanzielleSituationService finanzielleSituationService;
	@Inject
	private FallService fallService;
	@Inject
	private MandantService mandantService;
	@Inject
	private TraegerschaftService traegerschaftService;
	@Inject
	private InstitutionService institutionService;

	private static final Logger LOG = LoggerFactory.getLogger(JaxBConverter.class);


	@Nonnull
	public String toResourceId(@Nonnull final AbstractEntity entity) {
		return String.valueOf(Objects.requireNonNull(entity.getId()));
	}

	@Nonnull
	public String toEntityId(@Nonnull final JaxId resourceId) {
		// TODO wahrscheinlich besser manuell auf NULL pruefen und gegebenenfalls eine IllegalArgumentException werfen
		return Objects.requireNonNull(resourceId.getId());
	}

	@Nonnull
	public JaxId toJaxId(@Nonnull final AbstractEntity entity) {
		return new JaxId(entity.getId());
	}

	@Nonnull
	public JaxId toJaxId(@Nonnull final JaxAbstractDTO entity) {
		return new JaxId(entity.getId());
	}

	@Nonnull
	private <T extends JaxAbstractDTO> T convertAbstractFieldsToJAX(@Nonnull final AbstractEntity abstEntity, T jaxDTOToConvertTo) {
		jaxDTOToConvertTo.setTimestampErstellt(abstEntity.getTimestampErstellt());
		jaxDTOToConvertTo.setTimestampMutiert(abstEntity.getTimestampMutiert());
		jaxDTOToConvertTo.setId(checkNotNull(abstEntity.getId()));
		return jaxDTOToConvertTo;
	}

	@Nonnull
	private <T extends AbstractEntity> T convertAbstractFieldsToEntity(JaxAbstractDTO jaxToConvert, @Nonnull final T abstEntityToConvertTo) {
		if (jaxToConvert.getId() != null) {
			abstEntityToConvertTo.setId(jaxToConvert.getId());
			//ACHTUNG hier timestamp erstellt und mutiert NICHT  konvertieren da diese immer auf dem server gesetzt werden muessen
		}

		return abstEntityToConvertTo;
	}

	/**
	 * Converts all person related fields from Jax to Entity
	 * @param personEntityJAXP das objekt als Jax
	 * @param personEntity das object als Entity
	 */
	private void convertAbstractPersonFieldsToEntity(JaxAbstractPersonDTO personEntityJAXP, AbstractPersonEntity personEntity) {
		personEntity.setNachname(personEntityJAXP.getNachname());
		personEntity.setVorname(personEntityJAXP.getVorname());
		personEntity.setGeburtsdatum(personEntityJAXP.getGeburtsdatum());
		personEntity.setGeschlecht(personEntityJAXP.getGeschlecht());
	}

	/**
	 * Converts all person related fields from Entity to Jax
	 * @param personEntity das object als Entity
	 * @param personEntityJAXP das objekt als Jax
	 */
	private void convertAbstractPersonFieldsToJax(AbstractPersonEntity personEntity, JaxAbstractPersonDTO personEntityJAXP) {
		personEntityJAXP.setNachname(personEntity.getNachname());
		personEntityJAXP.setVorname(personEntity.getVorname());
		personEntityJAXP.setGeburtsdatum(personEntity.getGeburtsdatum());
		personEntityJAXP.setGeschlecht(personEntity.getGeschlecht());
	}

	@Nonnull
	public JaxApplicationProperties applicationPropertyToJAX(@Nonnull final ApplicationProperty applicationProperty) {
		JaxApplicationProperties jaxProperty = new JaxApplicationProperties();
		convertAbstractFieldsToJAX(applicationProperty, jaxProperty);
		jaxProperty.setName(applicationProperty.getName());
		jaxProperty.setValue(applicationProperty.getValue());
		return jaxProperty;
	}

	@Nonnull
	public ApplicationProperty applicationPropertieToEntity(JaxApplicationProperties jaxAP, @Nonnull final ApplicationProperty applicationProperty) {
		Validate.notNull(applicationProperty);
		Validate.notNull(jaxAP);
		convertAbstractFieldsToEntity(jaxAP, applicationProperty);
		applicationProperty.setName(jaxAP.getName());
		applicationProperty.setValue(jaxAP.getValue());

		return applicationProperty;
	}

	@Nonnull
	public Adresse adresseToEntity(@Nonnull JaxAdresse jaxAdresse, @Nonnull final Adresse adresse) {
		Validate.notNull(adresse);
		Validate.notNull(jaxAdresse);
		convertAbstractFieldsToEntity(jaxAdresse, adresse);
		adresse.setStrasse(jaxAdresse.getStrasse());
		adresse.setHausnummer(jaxAdresse.getHausnummer());
		adresse.setZusatzzeile(jaxAdresse.getZusatzzeile());
		adresse.setPlz(jaxAdresse.getPlz());
		adresse.setOrt(jaxAdresse.getOrt());
		adresse.setGemeinde(jaxAdresse.getGemeinde());
		adresse.setLand(jaxAdresse.getLand());
		adresse.setGueltigkeit(convertDateRange(jaxAdresse));
		//adresse gilt per default von start of time an
		adresse.getGueltigkeit().setGueltigAb(jaxAdresse.getGueltigAb() == null ? Constants.START_OF_TIME : jaxAdresse.getGueltigAb());
		adresse.setAdresseTyp(jaxAdresse.getAdresseTyp());

		return adresse;
	}

	/**
	 * Checks fields gueltigAb and gueltigBis from given object and returns the corresponding DateRange object
	 * If gueltigAb is null then current date is set instead
	 * If gueltigBis is null then end_of_time is set instead
	 *
	 * @param jaxAbstractDateRangedDTO JaxObject extending abstract class JaxAbstractDateRangedDTO
	 * @return DateRange object created with the given data
	 */
	private DateRange convertDateRange(JaxAbstractDateRangedDTO jaxAbstractDateRangedDTO) {
		LocalDate dateAb = jaxAbstractDateRangedDTO.getGueltigAb() == null ? LocalDate.now() : jaxAbstractDateRangedDTO.getGueltigAb();
		LocalDate dateBis = jaxAbstractDateRangedDTO.getGueltigBis() == null ? Constants.END_OF_TIME : jaxAbstractDateRangedDTO.getGueltigBis();
		return new DateRange(dateAb, dateBis);
	}

	@Nonnull
	public JaxAdresse adresseToJAX(@Nonnull final Adresse adresse) {
		JaxAdresse jaxAdresse = new JaxAdresse();
		convertAbstractFieldsToJAX(adresse, jaxAdresse);
		jaxAdresse.setStrasse(adresse.getStrasse());
		jaxAdresse.setHausnummer(adresse.getHausnummer());
		jaxAdresse.setZusatzzeile(adresse.getZusatzzeile());
		jaxAdresse.setPlz(adresse.getPlz());
		jaxAdresse.setOrt(adresse.getOrt());
		jaxAdresse.setGemeinde(adresse.getGemeinde());
		jaxAdresse.setLand(adresse.getLand());
		jaxAdresse.setGueltigAb(adresse.getGueltigkeit().getGueltigAb());
		jaxAdresse.setGueltigBis(adresse.getGueltigkeit().getGueltigBis());
		jaxAdresse.setAdresseTyp(adresse.getAdresseTyp());
		return jaxAdresse;
	}

	@Nonnull
	public JaxEnversRevision enversRevisionToJAX(@Nonnull final DefaultRevisionEntity revisionEntity,
												 @Nonnull final AbstractEntity abstractEntity, RevisionType accessType) {

		JaxEnversRevision jaxEnversRevision = new JaxEnversRevision();
		if (abstractEntity instanceof ApplicationProperty) {
			jaxEnversRevision.setEntity(applicationPropertyToJAX((ApplicationProperty) abstractEntity));
		}
		jaxEnversRevision.setRev(revisionEntity.getId());
		jaxEnversRevision.setRevTimeStamp(DateConvertUtils.asLocalDateTime(revisionEntity.getRevisionDate()));
		jaxEnversRevision.setAccessType(accessType);
		return jaxEnversRevision;
	}

	public Gesuchsteller gesuchstellerToEntity(@Nonnull JaxGesuchsteller gesuchstellerJAXP, @Nonnull Gesuchsteller gesuchsteller) {
		Validate.notNull(gesuchsteller);
		Validate.notNull(gesuchstellerJAXP);
		Validate.notNull(gesuchstellerJAXP.getWohnAdresse(),"Wohnadresse muss gesetzt sein");
		convertAbstractFieldsToEntity(gesuchstellerJAXP, gesuchsteller);
		convertAbstractPersonFieldsToEntity(gesuchstellerJAXP, gesuchsteller);
		gesuchsteller.setMail(gesuchstellerJAXP.getMail());
		gesuchsteller.setTelefon(gesuchstellerJAXP.getTelefon());
		gesuchsteller.setMobile(gesuchstellerJAXP.getMobile());
		gesuchsteller.setTelefonAusland(gesuchstellerJAXP.getTelefonAusland());
		gesuchsteller.setZpvNumber(gesuchstellerJAXP.getZpvNumber());

		//Relationen
		//Wir fuehren derzeit immer maximal  eine alternative Korrespondenzadressse -> diese updaten wenn vorhanden
		if (gesuchstellerJAXP.getAlternativeAdresse() != null) {
			Adresse currentAltAdr = adresseService.getKorrespondenzAdr(gesuchsteller.getId()).orElse(new Adresse());
			Adresse altAddrToMerge = adresseToEntity(gesuchstellerJAXP.getAlternativeAdresse(), currentAltAdr);
			gesuchsteller.addAdresse(altAddrToMerge);
		}
		// Umzug und Wohnadresse
		Adresse umzugAddr = null;
		if (gesuchstellerJAXP.getUmzugAdresse() != null) {
			umzugAddr = toStoreableAddresse(gesuchstellerJAXP.getUmzugAdresse());
			gesuchsteller.addAdresse(umzugAddr);
		}
		//Wohnadresse (abh von Umzug noch datum setzten)
		Adresse wohnAddrToMerge = toStoreableAddresse(gesuchstellerJAXP.getWohnAdresse());
		if (umzugAddr != null) {
			wohnAddrToMerge.getGueltigkeit().endOnDayBefore(umzugAddr.getGueltigkeit());
		}
		// Finanzielle Situation
		gesuchsteller.addAdresse(wohnAddrToMerge);
		if (gesuchstellerJAXP.getFinanzielleSituationContainer() != null) {
			gesuchsteller.setFinanzielleSituationContainer(finanzielleSituationContainerToEntity(gesuchstellerJAXP.getFinanzielleSituationContainer(), new FinanzielleSituationContainer()));
		}
		return gesuchsteller;
	}

	@Nonnull
	private Adresse toStoreableAddresse(@Nonnull JaxAdresse adresseToPrepareForSaving) {
		Adresse adrToMergeWith = new Adresse();
		if (adresseToPrepareForSaving.getId() != null) {

			Optional<Adresse> altAdr = adresseService.findAdresse(adresseToPrepareForSaving.getId());
			//wenn schon vorhanden updaten
			if (altAdr.isPresent()) {
				adrToMergeWith = altAdr.get();
			}
		}
		return adresseToEntity(adresseToPrepareForSaving, adrToMergeWith);
	}

	public JaxGesuchsteller gesuchstellerToJAX(@Nonnull Gesuchsteller persistedGesuchsteller) {
		Validate.isTrue(!persistedGesuchsteller.isNew(), "Gesuchsteller kann nicht nach REST transformiert werden weil sie noch " +
			"nicht persistiert wurde; Grund dafuer ist, dass wir die aktuelle Wohnadresse aus der Datenbank lesen wollen");
		JaxGesuchsteller jaxGesuchsteller = new JaxGesuchsteller();
		convertAbstractFieldsToJAX(persistedGesuchsteller, jaxGesuchsteller);
		convertAbstractPersonFieldsToJax(persistedGesuchsteller, jaxGesuchsteller);
		jaxGesuchsteller.setMail(persistedGesuchsteller.getMail());
		jaxGesuchsteller.setTelefon(persistedGesuchsteller.getTelefon());
		jaxGesuchsteller.setMobile(persistedGesuchsteller.getMobile());
		jaxGesuchsteller.setTelefonAusland(persistedGesuchsteller.getTelefonAusland());
		jaxGesuchsteller.setZpvNumber(persistedGesuchsteller.getZpvNumber());
		//relationen laden
		Optional<Adresse> altAdr = adresseService.getKorrespondenzAdr(persistedGesuchsteller.getId());
		altAdr.ifPresent(adresse -> jaxGesuchsteller.setAlternativeAdresse(adresseToJAX(adresse)));
		Adresse currentWohnadr = adresseService.getCurrentWohnadresse(persistedGesuchsteller.getId());
		jaxGesuchsteller.setWohnAdresse(adresseToJAX(currentWohnadr));

		//wenn heute gueltige Adresse von der Adresse divergiert die bis End of Time gilt dann wurde ein Umzug angegeben
		Optional<Adresse> maybeUmzugadresse = adresseService.getNewestWohnadresse(persistedGesuchsteller.getId());
		maybeUmzugadresse.filter(umzugAdresse -> !currentWohnadr.equals(umzugAdresse))
			.ifPresent(umzugAdr -> jaxGesuchsteller.setUmzugAdresse(adresseToJAX(umzugAdr)));
		// Finanzielle Situation
		if (persistedGesuchsteller.getFinanzielleSituationContainer() != null) {
			JaxFinanzielleSituationContainer jaxFinanzielleSituationContainer = finanzielleSituationContainerToJAX(persistedGesuchsteller.getFinanzielleSituationContainer());
			jaxGesuchsteller.setFinanzielleSituationContainer(jaxFinanzielleSituationContainer);
		}

		return jaxGesuchsteller;
	}

	public Familiensituation familiensituationToEntity(@Nonnull JaxFamilienSituation familiensituationJAXP, @Nonnull Familiensituation familiensituation) {
		Validate.notNull(familiensituation);
		Validate.notNull(familiensituationJAXP);
		convertAbstractFieldsToEntity(familiensituationJAXP, familiensituation);
		familiensituation.setFamilienstatus(familiensituationJAXP.getFamilienstatus());
		familiensituation.setGesuchstellerKardinalitaet(familiensituationJAXP.getGesuchstellerKardinalitaet());
		familiensituation.setBemerkungen(familiensituationJAXP.getBemerkungen());
		familiensituation.setGesuch(this.gesuchToEntity(familiensituationJAXP.getGesuch(), new Gesuch())); //todo imanol sollte Gesuch nicht aus der DB geholt werden?
		return familiensituation;
	}

	public JaxFamilienSituation familiensituationToJAX(@Nonnull Familiensituation persistedFamiliensituation) {
		JaxFamilienSituation jaxFamiliensituation = new JaxFamilienSituation();
		convertAbstractFieldsToJAX(persistedFamiliensituation, jaxFamiliensituation);
		jaxFamiliensituation.setFamilienstatus(persistedFamiliensituation.getFamilienstatus());
		jaxFamiliensituation.setGesuchstellerKardinalitaet(persistedFamiliensituation.getGesuchstellerKardinalitaet());
		jaxFamiliensituation.setBemerkungen(persistedFamiliensituation.getBemerkungen());
		jaxFamiliensituation.setGesuch(this.gesuchToJAX(persistedFamiliensituation.getGesuch()));
		return jaxFamiliensituation;
	}

	public Fall fallToEntity(@Nonnull JaxFall fallJAXP, @Nonnull Fall fall) {
		Validate.notNull(fall);
		Validate.notNull(fallJAXP);
		convertAbstractFieldsToEntity(fallJAXP, fall);
		return fall;
	}

	public JaxFall fallToJAX(@Nonnull Fall persistedFall) {
		JaxFall jaxFall = new JaxFall();
		convertAbstractFieldsToJAX(persistedFall, jaxFall);
		return jaxFall;
	}

	public Gesuch gesuchToEntity(@Nonnull JaxGesuch gesuchJAXP, @Nonnull Gesuch gesuch) {
		Validate.notNull(gesuch);
		Validate.notNull(gesuchJAXP);
		convertAbstractFieldsToEntity(gesuchJAXP, gesuch);

		Optional<Fall> fallFromDB = fallService.findFall(gesuchJAXP.getFall().getId());
		if (fallFromDB.isPresent()) {
			gesuch.setFall(this.fallToEntity(gesuchJAXP.getFall(), fallFromDB.get()));
		} else {
			throw new EbeguEntityNotFoundException("gesuchToEntity", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, gesuchJAXP.getFall());
		}
		if (gesuchJAXP.getGesuchsteller1() != null && gesuchJAXP.getGesuchsteller1().getId() != null) {
			Optional<Gesuchsteller> gesuchsteller1 = gesuchstellerService.findGesuchsteller(gesuchJAXP.getGesuchsteller1().getId());
			if (gesuchsteller1.isPresent()) {
				gesuch.setGesuchsteller1(gesuchstellerToEntity(gesuchJAXP.getGesuchsteller1(), gesuchsteller1.get()));
			} else {
				throw new EbeguEntityNotFoundException("gesuchToEntity", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, gesuchJAXP.getGesuchsteller1());
			}
		}
		if (gesuchJAXP.getGesuchsteller2() != null && gesuchJAXP.getGesuchsteller2().getId() != null) {
			Optional<Gesuchsteller> gesuchsteller2 = gesuchstellerService.findGesuchsteller(gesuchJAXP.getGesuchsteller2().getId());
			if (gesuchsteller2.isPresent()){
				gesuch.setGesuchsteller2(gesuchstellerToEntity(gesuchJAXP.getGesuchsteller2(), gesuchsteller2.get()));
			} else {
				throw new EbeguEntityNotFoundException("gesuchToEntity", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, gesuchJAXP.getGesuchsteller2().getId());
			}
		}
		return gesuch;
	}

	public JaxGesuch gesuchToJAX(@Nonnull Gesuch persistedGesuch) {
		JaxGesuch jaxGesuch = new JaxGesuch();
		convertAbstractFieldsToJAX(persistedGesuch, jaxGesuch);
		jaxGesuch.setFall(this.fallToJAX(persistedGesuch.getFall()));
		if(persistedGesuch.getGesuchsteller1() != null) {
			jaxGesuch.setGesuchsteller1(this.gesuchstellerToJAX(persistedGesuch.getGesuchsteller1()));
		}
		if(persistedGesuch.getGesuchsteller2() != null) {
			jaxGesuch.setGesuchsteller2(this.gesuchstellerToJAX(persistedGesuch.getGesuchsteller2()));
		}
		for (KindContainer kind : persistedGesuch.getKindContainers()) {
			jaxGesuch.getKinder().add(kindContainerToJAX(kind));
		}

		return jaxGesuch;
	}

	public JaxMandant mandantToJAX(@Nonnull Mandant persistedMandant) {
		JaxMandant jaxMandant = new JaxMandant();
		convertAbstractFieldsToJAX(persistedMandant, jaxMandant);
		jaxMandant.setName(persistedMandant.getName());
		return jaxMandant;
	}

	public JaxTraegerschaft traegerschaftToJAX(Traegerschaft persistedTraegerschaft) {
		JaxTraegerschaft jaxTraegerschaft = new JaxTraegerschaft();
		convertAbstractFieldsToJAX(persistedTraegerschaft, jaxTraegerschaft);
		jaxTraegerschaft.setName(persistedTraegerschaft.getName());
		return jaxTraegerschaft;
	}

	public Mandant mandantToEntity(JaxMandant mandantJAXP, Mandant mandant) {
		Validate.notNull(mandant);
		Validate.notNull(mandantJAXP);
		convertAbstractFieldsToEntity(mandantJAXP, mandant);
		mandant.setName(mandantJAXP.getName());
		return mandant;
	}

	public Traegerschaft traegerschaftToEntity(@Nonnull JaxTraegerschaft traegerschaftJAXP, @Nonnull Traegerschaft traegerschaft) {
		Validate.notNull(traegerschaft);
		Validate.notNull(traegerschaftJAXP);
		convertAbstractFieldsToEntity(traegerschaftJAXP, traegerschaft);
		traegerschaft.setName(traegerschaftJAXP.getName());
		return traegerschaft;
	}

	public Fachstelle fachstelleToEntity(JaxFachstelle fachstelleJAXP, Fachstelle fachstelle) {
		Validate.notNull(fachstelleJAXP);
		Validate.notNull(fachstelle);
		convertAbstractFieldsToEntity(fachstelleJAXP, fachstelle);
		fachstelle.setName(fachstelleJAXP.getName());
		fachstelle.setBeschreibung(fachstelleJAXP.getBeschreibung());
		fachstelle.setBehinderungsbestaetigung(fachstelleJAXP.isBehinderungsbestaetigung());
		return fachstelle;
	}

	public JaxFachstelle fachstelleToJAX(@Nonnull Fachstelle persistedFachstelle) {
		JaxFachstelle jaxFachstelle = new JaxFachstelle();
		convertAbstractFieldsToJAX(persistedFachstelle, jaxFachstelle);
		jaxFachstelle.setName(persistedFachstelle.getName());
		jaxFachstelle.setBeschreibung(persistedFachstelle.getBeschreibung());
		jaxFachstelle.setBehinderungsbestaetigung(persistedFachstelle.isBehinderungsbestaetigung());
		return jaxFachstelle;
	}


	public JaxInstitution institutionToJAX(Institution persistedInstitution) {
		JaxInstitution jaxInstitution = new JaxInstitution();
		convertAbstractFieldsToJAX(persistedInstitution, jaxInstitution);
		jaxInstitution.setName(persistedInstitution.getName());
		jaxInstitution.setMandant(mandantToJAX(persistedInstitution.getMandant()));
		jaxInstitution.setTraegerschaft(traegerschaftToJAX(persistedInstitution.getTraegerschaft()));
		return jaxInstitution;
	}

	public Institution institutionToEntity(JaxInstitution institutionJAXP, Institution institution) {
		Validate.notNull(institutionJAXP);
		Validate.notNull(institution);
		convertAbstractFieldsToEntity(institutionJAXP, institution);
		institution.setName(institutionJAXP.getName());

		if (institutionJAXP.getMandant().getId() != null) {
			Optional<Mandant> mandantFromDB = mandantService.findMandant(institutionJAXP.getMandant().getId());
			if (mandantFromDB.isPresent()) {
				institution.setMandant(mandantToEntity(institutionJAXP.getMandant(), mandantFromDB.get()));
			} else {
				throw new EbeguEntityNotFoundException("institutionToEntity -> mandant", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, institutionJAXP.getMandant().getId());
			}

		} else {
			//todo homa ebegu 82 review wie reagieren wir hier
			throw new EbeguEntityNotFoundException("institutionToEntity -> mandant", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND);
//			institution.setMandant(mandantToEntity(institutionJAXP.getMandant(), new Mandant()));
		}

		if (institutionJAXP.getTraegerschaft().getId() != null) {
			Optional<Traegerschaft> traegerschaftFromDB = traegerschaftService.findTraegerschaft(institutionJAXP.getTraegerschaft().getId());
			if (traegerschaftFromDB.isPresent()) {
				institution.setTraegerschaft(traegerschaftToEntity(institutionJAXP.getTraegerschaft(), traegerschaftFromDB.get()));
			} else {
				throw new EbeguEntityNotFoundException("institutionToEntity -> traegerschaft", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, institutionJAXP.getTraegerschaft().getId());
			}
		} else {
			//todo homa ebegu 82 review wie reagieren wir hier
			throw new EbeguEntityNotFoundException("institutionToEntity -> traegerschaft", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND);
//			institution.setTraegerschaft(traegerschaftToEntity(institutionJAXP.getTraegerschaft(), new Traegerschaft()));
		}
		return institution;
	}

	public JaxInstitutionStammdaten institutionStammdatenToJAX(InstitutionStammdaten persistedInstStammdaten) {
		JaxInstitutionStammdaten jaxInstStammdaten = new JaxInstitutionStammdaten();
		convertAbstractFieldsToJAX(persistedInstStammdaten, jaxInstStammdaten);
		jaxInstStammdaten.setOeffnungstage(persistedInstStammdaten.getOeffnungstage());
		jaxInstStammdaten.setOeffnungsstunden(persistedInstStammdaten.getOeffnungsstunden());
		jaxInstStammdaten.setIban(persistedInstStammdaten.getIban().getIban());
		jaxInstStammdaten.setBetreuungsangebotTyp(persistedInstStammdaten.getBetreuungsangebotTyp());
		jaxInstStammdaten.setGueltigAb(persistedInstStammdaten.getGueltigkeit().getGueltigAb());
		jaxInstStammdaten.setGueltigBis(persistedInstStammdaten.getGueltigkeit().getGueltigBis());
		jaxInstStammdaten.setInstitution(institutionToJAX(persistedInstStammdaten.getInstitution()));
		return jaxInstStammdaten;
	}

	public InstitutionStammdaten institutionStammdatenToEntity(JaxInstitutionStammdaten institutionStammdatenJAXP, InstitutionStammdaten institutionStammdaten) {
		Validate.notNull(institutionStammdatenJAXP);
		Validate.notNull(institutionStammdaten);

		convertAbstractFieldsToEntity(institutionStammdatenJAXP, institutionStammdaten);
		institutionStammdaten.setOeffnungstage(institutionStammdatenJAXP.getOeffnungstage());
		institutionStammdaten.setOeffnungsstunden(institutionStammdatenJAXP.getOeffnungsstunden());
		institutionStammdaten.setIban(new IBAN(institutionStammdatenJAXP.getIban()));
		institutionStammdaten.setBetreuungsangebotTyp(institutionStammdatenJAXP.getBetreuungsangebotTyp());
		institutionStammdaten.setGueltigkeit(convertDateRange(institutionStammdatenJAXP));

		Optional<Institution> institutionFromDB = institutionService.findInstitution(institutionStammdatenJAXP.getInstitution().getId());
		if (institutionFromDB.isPresent()) {
			institutionStammdaten.setInstitution(institutionToEntity(institutionStammdatenJAXP.getInstitution(), institutionFromDB.get()));
		} else {
			throw new EbeguEntityNotFoundException("institutionStammdatenToEntity", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, institutionStammdatenJAXP.getInstitution().getId());
		}

		return institutionStammdaten;

	}

	public JaxKind kindToJAX(Kind persistedKind) {
		JaxKind jaxKind = new JaxKind();
		convertAbstractFieldsToJAX(persistedKind, jaxKind);
		convertAbstractPersonFieldsToJax(persistedKind, jaxKind);
		jaxKind.setWohnhaftImGleichenHaushalt(persistedKind.getWohnhaftImGleichenHaushalt());
		jaxKind.setUnterstuetzungspflicht(persistedKind.getUnterstuetzungspflicht());
		jaxKind.setFamilienErgaenzendeBetreuung(persistedKind.getFamilienErgaenzendeBetreuung());
		jaxKind.setMutterspracheDeutsch(persistedKind.getMutterspracheDeutsch());
		jaxKind.setPensumFachstelle(pensumFachstelleToJax(persistedKind.getPensumFachstelle()));
		jaxKind.setBemerkungen(persistedKind.getBemerkungen());
		return jaxKind;
	}

	public JaxPensumFachstelle pensumFachstelleToJax(PensumFachstelle persistedPensumFachstelle) {
		if (persistedPensumFachstelle == null) {
			return null;
		}
		JaxPensumFachstelle jaxPensumFachstelle = new JaxPensumFachstelle();
		convertAbstractFieldsToJAX(persistedPensumFachstelle, jaxPensumFachstelle);
		jaxPensumFachstelle.setPensum(persistedPensumFachstelle.getPensum());
		jaxPensumFachstelle.setGueltigAb(persistedPensumFachstelle.getGueltigkeit().getGueltigAb());
		jaxPensumFachstelle.setGueltigBis(persistedPensumFachstelle.getGueltigkeit().getGueltigBis());
		jaxPensumFachstelle.setFachstelle(fachstelleToJAX(persistedPensumFachstelle.getFachstelle()));
		return jaxPensumFachstelle;
	}

	public PensumFachstelle pensumFachstelletoEntity(JaxPensumFachstelle pensumFachstelleJAXP, PensumFachstelle pensumFachstelle) {
		pensumFachstelle.setGueltigkeit(convertDateRange(pensumFachstelleJAXP));
		pensumFachstelle.setPensum(pensumFachstelleJAXP.getPensum());

		Optional<Fachstelle> fachstelleFromDB = fachstelleService.findFachstelle(pensumFachstelleJAXP.getFachstelle().getId());
		if (fachstelleFromDB.isPresent()) {
			pensumFachstelle.setFachstelle(fachstelleToEntity(pensumFachstelleJAXP.getFachstelle(), fachstelleFromDB.get()));
		} else {
			throw new EbeguEntityNotFoundException("pensumFachstelletoEntity", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, pensumFachstelleJAXP.getFachstelle().getId());
		}

		return pensumFachstelle;
	}

	public JaxKindContainer kindContainerToJAX(KindContainer persistedKind) {
		JaxKindContainer jaxKindContainer = new JaxKindContainer();
		convertAbstractFieldsToJAX(persistedKind, jaxKindContainer);
		if (persistedKind.getKindGS() != null) {
			jaxKindContainer.setKindGS(kindToJAX(persistedKind.getKindGS()));
		}
		if (persistedKind.getKindJA() != null) {
			jaxKindContainer.setKindJA(kindToJAX(persistedKind.getKindJA()));
		}
		return jaxKindContainer;
	}

	public Kind kindToEntity(JaxKind kindJAXP, Kind kind) {
		Validate.notNull(kindJAXP);
		Validate.notNull(kind);
		convertAbstractFieldsToEntity(kindJAXP, kind);
		convertAbstractPersonFieldsToEntity(kindJAXP, kind);
		kind.setWohnhaftImGleichenHaushalt(kindJAXP.getWohnhaftImGleichenHaushalt());
		kind.setUnterstuetzungspflicht(kindJAXP.getUnterstuetzungspflicht());
		kind.setFamilienErgaenzendeBetreuung(kindJAXP.getFamilienErgaenzendeBetreuung());
		kind.setMutterspracheDeutsch(kindJAXP.getMutterspracheDeutsch());

		PensumFachstelle updatedPensumFachstelle = null;
		if (kindJAXP.getPensumFachstelle() != null && kindJAXP.getPensumFachstelle().getId() == null) {
			updatedPensumFachstelle = pensumFachstelletoEntity(kindJAXP.getPensumFachstelle(), new PensumFachstelle());
		}
		else if (kindJAXP.getPensumFachstelle() != null) {
			PensumFachstelle pensumFachstelleFromDB = pensumFachstelleService.findPensumFachstelle(kindJAXP.getPensumFachstelle().getId())
				.orElseThrow(() -> new EbeguEntityNotFoundException("kindToEntity", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, kindJAXP.getPensumFachstelle().getId()));
			updatedPensumFachstelle = pensumFachstelletoEntity(kindJAXP.getPensumFachstelle(), pensumFachstelleFromDB);
		}

		kind.setPensumFachstelle(updatedPensumFachstelle);

		kind.setBemerkungen(kindJAXP.getBemerkungen());
		return kind;
	}

	public KindContainer kindContainerToEntity(JaxKindContainer kindContainerJAXP, KindContainer kindContainer) {
		Validate.notNull(kindContainer);
		Validate.notNull(kindContainerJAXP);
		convertAbstractFieldsToEntity(kindContainerJAXP, kindContainer);
		if (kindContainerJAXP.getKindGS() != null) {
			Kind kindGS = new Kind();
			if (kindContainer.getKindGS() != null) {
				kindGS = kindContainer.getKindGS();
			}
			kindContainer.setKindGS(kindToEntity(kindContainerJAXP.getKindGS(), kindGS));
		}
		if (kindContainerJAXP.getKindJA() != null) {
			Kind kindJA = new Kind();
			if (kindContainer.getKindJA() != null) {
				kindJA = kindContainer.getKindJA();
			}
			kindContainer.setKindJA(kindToEntity(kindContainerJAXP.getKindJA(), kindJA));
		}
		return kindContainer;
	}

	/**
	 * Sucht die Fachstelle in der DB und fuegt sie mit der als Parameter gegebenen Fachstelle zusammen.
	 * Sollte sie in der DB nicht existieren, gibt die Methode eine neue Fachstelle mit den gegebenen Daten zurueck
	 * @param fachstelleToFind die Fachstelle als JAX
	 * @return die Fachstelle als Entity
     */
	@Nonnull
	private Fachstelle findFachstelleToEntity(JaxFachstelle fachstelleToFind) {
		Validate.notNull(fachstelleToFind);
		Fachstelle fachstelleToMergeWith = new Fachstelle();
		if (fachstelleToFind.getId() != null ) {
			Optional<Fachstelle> altFachstelle = fachstelleService.findFachstelle(fachstelleToFind.getId());
			if (altFachstelle.isPresent()) {
				fachstelleToMergeWith = altFachstelle.get();
			}
		}
		return fachstelleToEntity(fachstelleToFind, fachstelleToMergeWith);
	}

	/**
	 * Sucht das Gesuch in der DB und fuegt es mit dem als Parameter gegebenen Gesuch zusammen.
	 * Sollte es in der DB nicht existieren, gibt die Methode ein neues Gesuch mit den gegebenen Daten zurueck
	 * @param gesuchToFind das Gesuch als JAX
	 * @return das Gesuch als Entity
     */
	@Nonnull
	private Gesuch findGesuchToEntity(JaxGesuch gesuchToFind) {
		Validate.notNull(gesuchToFind);
		Gesuch gesuchToMergeWith = new Gesuch();
		if (gesuchToFind.getId() != null ) {
			Optional<Gesuch> altGesuch = gesuchService.findGesuch(gesuchToFind.getId());
			if (altGesuch.isPresent()) {
				gesuchToMergeWith = altGesuch.get();
			}
		}
		return gesuchToEntity(gesuchToFind, gesuchToMergeWith);
	}

	public FinanzielleSituationContainer finanzielleSituationContainerToEntity(@Nonnull JaxFinanzielleSituationContainer containerJAX,
																			   @Nonnull FinanzielleSituationContainer container) {
		Validate.notNull(container);
		Validate.notNull(containerJAX);
		convertAbstractFieldsToEntity(containerJAX, container);
		container.setJahr(containerJAX.getJahr());
		if (containerJAX.getFinanzielleSituationGS() != null) {
			container.setFinanzielleSituationGS(finanzielleSituationToEntity(containerJAX.getFinanzielleSituationGS(), new FinanzielleSituation()));
		}
		if (containerJAX.getFinanzielleSituationJA() != null) {
			container.setFinanzielleSituationJA(finanzielleSituationToEntity(containerJAX.getFinanzielleSituationJA(), new FinanzielleSituation()));
		}
		if (containerJAX.getFinanzielleSituationSV() != null) {
			container.setFinanzielleSituationSV(finanzielleSituationToEntity(containerJAX.getFinanzielleSituationSV(), new FinanzielleSituation()));
		}
		return container;
	}

	public JaxFinanzielleSituationContainer finanzielleSituationContainerToJAX(FinanzielleSituationContainer persistedFinanzielleSituation) {
		JaxFinanzielleSituationContainer jaxPerson = new JaxFinanzielleSituationContainer();
		convertAbstractFieldsToJAX(persistedFinanzielleSituation, jaxPerson);
		jaxPerson.setJahr(persistedFinanzielleSituation.getJahr());
		jaxPerson.setFinanzielleSituationGS(finanzielleSituationToJAX(persistedFinanzielleSituation.getFinanzielleSituationGS()));
		jaxPerson.setFinanzielleSituationJA(finanzielleSituationToJAX(persistedFinanzielleSituation.getFinanzielleSituationJA()));
		jaxPerson.setFinanzielleSituationSV(finanzielleSituationToJAX(persistedFinanzielleSituation.getFinanzielleSituationSV()));
		return jaxPerson;
	}

	public FinanzielleSituation finanzielleSituationToEntity(@Nonnull JaxFinanzielleSituation finanzielleSituationJAXP, @Nonnull FinanzielleSituation finanzielleSituation) {
		Validate.notNull(finanzielleSituation);
		Validate.notNull(finanzielleSituationJAXP);
		convertAbstractFieldsToEntity(finanzielleSituationJAXP, finanzielleSituation);
		finanzielleSituation.setSteuerveranlagungErhalten(finanzielleSituationJAXP.getSteuerveranlagungErhalten());
		finanzielleSituation.setSteuererklaerungAusgefuellt(finanzielleSituationJAXP.getSteuererklaerungAusgefuellt());
		finanzielleSituation.setNettolohn(finanzielleSituationJAXP.getNettolohn());
		finanzielleSituation.setFamilienzulage(finanzielleSituationJAXP.getFamilienzulage());
		finanzielleSituation.setErsatzeinkommen(finanzielleSituationJAXP.getErsatzeinkommen());
		finanzielleSituation.setErhalteneAlimente(finanzielleSituationJAXP.getErhalteneAlimente());
		finanzielleSituation.setBruttovermoegen(finanzielleSituationJAXP.getBruttovermoegen());
		finanzielleSituation.setSchulden(finanzielleSituationJAXP.getSchulden());
		finanzielleSituation.setSelbstaendig(finanzielleSituationJAXP.getSelbstaendig());
		finanzielleSituation.setGeschaeftsgewinnBasisjahrMinus2(finanzielleSituationJAXP.getGeschaeftsgewinnBasisjahrMinus2());
		finanzielleSituation.setGeschaeftsgewinnBasisjahrMinus1(finanzielleSituationJAXP.getGeschaeftsgewinnBasisjahrMinus1());
		finanzielleSituation.setGeschaeftsgewinnBasisjahr(finanzielleSituationJAXP.getGeschaeftsgewinnBasisjahr());
		finanzielleSituation.setGeleisteteAlimente(finanzielleSituationJAXP.getGeleisteteAlimente());
		return finanzielleSituation;
	}

	private JaxFinanzielleSituation finanzielleSituationToJAX(@Nullable FinanzielleSituation persistedFinanzielleSituation) {
		if (persistedFinanzielleSituation != null) {
			JaxFinanzielleSituation jaxPerson = new JaxFinanzielleSituation();
			convertAbstractFieldsToJAX(persistedFinanzielleSituation, jaxPerson);
			jaxPerson.setSteuerveranlagungErhalten(persistedFinanzielleSituation.getSteuerveranlagungErhalten());
			jaxPerson.setSteuererklaerungAusgefuellt(persistedFinanzielleSituation.getSteuererklaerungAusgefuellt());
			jaxPerson.setNettolohn(persistedFinanzielleSituation.getNettolohn());
			jaxPerson.setFamilienzulage(persistedFinanzielleSituation.getFamilienzulage());
			jaxPerson.setErsatzeinkommen(persistedFinanzielleSituation.getErsatzeinkommen());
			jaxPerson.setErhalteneAlimente(persistedFinanzielleSituation.getErhalteneAlimente());
			jaxPerson.setBruttovermoegen(persistedFinanzielleSituation.getBruttovermoegen());
			jaxPerson.setSchulden(persistedFinanzielleSituation.getSchulden());
			jaxPerson.setSelbstaendig(persistedFinanzielleSituation.getSelbstaendig());
			jaxPerson.setGeschaeftsgewinnBasisjahrMinus2(persistedFinanzielleSituation.getGeschaeftsgewinnBasisjahrMinus2());
			jaxPerson.setGeschaeftsgewinnBasisjahrMinus1(persistedFinanzielleSituation.getGeschaeftsgewinnBasisjahrMinus1());
			jaxPerson.setGeschaeftsgewinnBasisjahr(persistedFinanzielleSituation.getGeschaeftsgewinnBasisjahr());
			jaxPerson.setGeleisteteAlimente(persistedFinanzielleSituation.getGeleisteteAlimente());
			return jaxPerson;
		}
		return null;
	}

}
