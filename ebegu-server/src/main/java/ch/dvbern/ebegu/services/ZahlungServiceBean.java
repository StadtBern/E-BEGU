package ch.dvbern.ebegu.services;

import ch.dvbern.ebegu.config.EbeguConfiguration;
import ch.dvbern.ebegu.entities.*;
import ch.dvbern.ebegu.enums.*;
import ch.dvbern.ebegu.errors.EbeguEntityNotFoundException;
import ch.dvbern.ebegu.errors.EbeguRuntimeException;
import ch.dvbern.ebegu.persistence.CriteriaQueryHelper;
import ch.dvbern.ebegu.types.DateRange;
import ch.dvbern.ebegu.types.DateRange_;
import ch.dvbern.ebegu.util.Constants;
import ch.dvbern.ebegu.util.MathUtil;
import ch.dvbern.lib.cdipersistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeTypeParseException;
import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static ch.dvbern.ebegu.enums.UserRoleName.*;


/**
 * Service fuer Zahlungen. Die Zahlungen werden folgendermassen generiert:
 * Wenn ein neuer Zahlungsauftrag erstellt wird, muss nur ein Faelligkeitsdatum mitgegeben werden. Dieses wird *nur* fuer
 * das Abfuellen des XML Files (ISO-20022) verwendet. Fuer die Ermittlung der einzuschliessenden Zahlungsdetail wird
 * immer der aktuelle Timestamp verwendet. Dies, damit wir fuer den naechsten Zahlungsauftrag immer wissen, welche
 * Zahlungen bereits beruecksichtigt wurden.
 * Wir muessen mit 2 Zeitraeumen arbeiten:
 * |      Jan      |      Feb       |
 * |                  |
 * letzter            aktueller
 * Zahlungslauf		Zahlungslauf
 * Für die Ermittlung der "normalen" Zahlungen wird immer (mind.) ein ganzer Monat berücksichtigt, und zwar der aktuelle
 * Monat des Zahlungslaufes plus fruehere Monate, falls in diesen kein Zahlungslauf stattfand.
 * Für die Ermittlung der Korrektur-Zahlungen muessen alle Verfuegungen berücksichtigt werden, welche seit dem letzten
 * Zahlungslauf bis heute dazugekommen sind.
 */
@Stateless
@Local(ZahlungService.class)
@RolesAllowed(value = {SUPER_ADMIN, ADMIN, SACHBEARBEITER_JA, SACHBEARBEITER_INSTITUTION, SACHBEARBEITER_TRAEGERSCHAFT, JURIST, REVISOR})
@SuppressWarnings(value = {"PMD.AvoidDuplicateLiterals", "SpringAutowiredFieldsWarningInspection", "InstanceMethodNamingConvention"})
public class ZahlungServiceBean extends AbstractBaseService implements ZahlungService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZahlungServiceBean.class.getSimpleName());

	@Inject
	private Persistence persistence;

	@Inject
	private CriteriaQueryHelper criteriaQueryHelper;

	@Inject
	private VerfuegungService verfuegungService;

	@Inject
	private FileSaverService fileSaverService;

	@Inject
	private EbeguConfiguration ebeguConfiguration;

	@Inject
	private GeneratedDokumentService generatedDokumentService;

	@Override
	@Nonnull
	@RolesAllowed(value = {SUPER_ADMIN, ADMIN, SACHBEARBEITER_JA})
	public Zahlungsauftrag zahlungsauftragErstellen(@Nonnull LocalDate datumFaelligkeit, @Nonnull String beschreibung) {
		return zahlungsauftragErstellen(datumFaelligkeit, beschreibung, LocalDateTime.now());
	}

	@Override
	@Nonnull
	@RolesAllowed(value = {SUPER_ADMIN, ADMIN, SACHBEARBEITER_JA})
	public Zahlungsauftrag zahlungsauftragErstellen(@Nonnull LocalDate datumFaelligkeit, @Nonnull String beschreibung, @Nonnull LocalDateTime datumGeneriert) {
		// Es darf immer nur ein Zahlungsauftrag im Status ENTWURF sein
		Optional<Zahlungsauftrag> lastZahlungsauftrag = findLastZahlungsauftrag();
		if (lastZahlungsauftrag.isPresent() && lastZahlungsauftrag.get().getStatus().isEntwurf()) {
			throw new EbeguRuntimeException("zahlungsauftragErstellen", ErrorCodeEnum.ERROR_ZAHLUNG_ERSTELLEN);
		}

		LOGGER.info("Erstelle Zahlungsauftrag mit Faelligkeit: " + Constants.DATE_FORMATTER.format(datumFaelligkeit));
		Zahlungsauftrag zahlungsauftrag = new Zahlungsauftrag();
		zahlungsauftrag.setStatus(ZahlungauftragStatus.ENTWURF);
		zahlungsauftrag.setBeschrieb(beschreibung);
		zahlungsauftrag.setDatumFaellig(datumFaelligkeit);
		zahlungsauftrag.setDatumGeneriert(datumGeneriert);

		// Alle aktuellen (d.h. der letzte Antrag jedes Falles) Verfuegungen suchen, welche ein Kita-Angebot haben
		// Wir brauchen folgende Daten:
		// - Zeitraum, welcher fuer die (normale) Auszahlung gilt: Immer ganzer Monat, mindestens der Monat des DatumFaellig,
		// 		jedoch seit Ende Monat des letzten Auftrags -> 1 oder mehrere ganze Monate
		// - Zeitraum, welcher fuer die Berechnung der rueckwirkenden Korrekturen gilt: Zeitpunkt der letzten Zahlungserstellung bis aktueller Zeitpunkt
		// 		(Achtung: Es ist *nicht* das Faelligkeitsdatum relevant, sondern das Erstellungsdatum des letzten Auftrags!)
		// Den letzten Zahlungsauftrag lesen
		LocalDateTime lastZahlungErstellt = Constants.START_OF_DATETIME; // Default, falls dies der erste Auftrag ist
		// Falls es eine Wiederholung des Auftrags ist, muessen nur noch die Korrekturen beruecksichtigt werden, welche
		// seit dem letzten Auftrag erstellt wurden
		boolean isRepetition = false;
		// Falls fuer denselben Zeitraum (oder den letzten Teil davon) schon ein Auftrag vorhanden ist, kann das DatumVon nicht
		// einfach an den letzten Auftrag anschliessen
		LocalDate zeitabschnittVon;
		LocalDate zeitabschnittBis = zahlungsauftrag.getDatumGeneriert().toLocalDate().with(TemporalAdjusters.lastDayOfMonth());

		if (lastZahlungsauftrag.isPresent()) {
			lastZahlungErstellt = lastZahlungsauftrag.get().getDatumGeneriert();
			if (zahlungsauftrag.getDatumGeneriert().toLocalDate().isAfter(lastZahlungsauftrag.get().getGueltigkeit().getGueltigBis())) {
				// Wir beginnen am Anfang des Folgemonats des letzten Auftrags
				zeitabschnittVon = lastZahlungErstellt.plusMonths(1).with(TemporalAdjusters.firstDayOfMonth()).toLocalDate();
			} else {
				// Repetition, dh.der Monat ist schon ausgeloest. Wir nehmen den Anfang des Monats
				zeitabschnittVon = lastZahlungErstellt.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate();
				isRepetition = true;
			}
		} else {
			zeitabschnittVon = lastZahlungErstellt.toLocalDate();
		}

		zahlungsauftrag.setGueltigkeit(new DateRange(zeitabschnittVon, zeitabschnittBis));

		Map<String, Zahlung> zahlungProInstitution = new HashMap<>();

		// "Normale" Zahlungen
		if (!isRepetition) {
			LOGGER.info("Ermittle normale Zahlungen im Zeitraum " + zahlungsauftrag.getGueltigkeit().toRangeString());
			Collection<VerfuegungZeitabschnitt> gueltigeVerfuegungZeitabschnitte = getGueltigeVerfuegungZeitabschnitte(zeitabschnittVon, zeitabschnittBis);
			for (VerfuegungZeitabschnitt zeitabschnitt : gueltigeVerfuegungZeitabschnitte) {
				if (zeitabschnitt.getZahlungsstatus().isNeu()) {
					createZahlungsposition(zeitabschnitt, zahlungsauftrag, zahlungProInstitution);
				}
			}
		}
		// Korrekturen und Nachzahlungen
		// Stichtag: Falls es eine Wiederholung des Auftrags ist, wurde der aktuelle Monat bereits ausbezahlt.
		LocalDate stichtagKorrekturen = isRepetition ? zeitabschnittBis : zeitabschnittBis.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
		// Die Korrekturzahlungen werden seit dem letzten Zahlungsauftrag beruecksichtigt. Falls wir im TEST-Mode sind
		// und ein fiktives "DatumGeneriert" gewaehlt haben, nehmen wir als Datum des letzten Auftrags das timestampErstellt
		// und nicht das (eventuell ebenfalls fiktive) datumGeneriert.
		boolean isTestMode = ebeguConfiguration.getIsZahlungenTestMode();
		if (isTestMode) {
			lastZahlungErstellt = Constants.START_OF_DATETIME;
		}
		Collection<VerfuegungZeitabschnitt> verfuegungsZeitabschnitte = getVerfuegungsZeitabschnitteNachVerfuegungDatum(lastZahlungErstellt, zahlungsauftrag.getDatumGeneriert(), stichtagKorrekturen);
		for (VerfuegungZeitabschnitt zeitabschnitt : verfuegungsZeitabschnitte) {
			if (zeitabschnitt.getZahlungsstatus().isIgnorierend() || zeitabschnitt.getZahlungsstatus().isNeu()) {
				createZahlungspositionenKorrekturUndNachzahlung(zeitabschnitt, zahlungsauftrag, zahlungProInstitution);
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Zahlungsauftrag generiert: ").append(zahlungsauftrag.getGueltigkeit().toRangeString());
		if (isRepetition) {
			sb.append(" [Repetition]");
		}
		LOGGER.info(sb.toString());
		calculateZahlungsauftrag(zahlungsauftrag);
		return persistence.merge(zahlungsauftrag);
	}

	/**
	 * Zahlungsauftrag wird einmalig berechnet. Danach koennen nur noch die Stammdaten der Institutionen
	 * geaendert werden.
	 */
	private void calculateZahlungsauftrag(Zahlungsauftrag zahlungsauftrag) {
		BigDecimal totalAuftrag = BigDecimal.ZERO;
		for (Zahlung zahlung : zahlungsauftrag.getZahlungen()) {
			BigDecimal totalZahlung = BigDecimal.ZERO;
			for (Zahlungsposition zahlungsposition : zahlung.getZahlungspositionen()) {
				if (!zahlungsposition.isIgnoriert()) {
					totalZahlung = MathUtil.DEFAULT.add(totalZahlung, zahlungsposition.getBetrag());
				}
			}
			zahlung.setBetragTotalZahlung(totalZahlung);
			totalAuftrag = MathUtil.DEFAULT.add(totalAuftrag, totalZahlung);
		}
		zahlungsauftrag.setBetragTotalAuftrag(totalAuftrag);
	}

	/**
	 * Ermittelt die aktuell gueltigen Verfuegungszeitabschnitte fuer die normale monatliche Zahlung (keine Korrekturen).
	 */
	@Nonnull
	private Collection<VerfuegungZeitabschnitt> getGueltigeVerfuegungZeitabschnitte(@Nonnull LocalDate zeitabschnittVon, @Nonnull LocalDate zeitabschnittBis) {
		Objects.requireNonNull(zeitabschnittVon, "zeitabschnittVon muss gesetzt sein");
		Objects.requireNonNull(zeitabschnittBis, "zeitabschnittBis muss gesetzt sein");

		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<VerfuegungZeitabschnitt> query = cb.createQuery(VerfuegungZeitabschnitt.class);
		Root<VerfuegungZeitabschnitt> root = query.from(VerfuegungZeitabschnitt.class);
		Join<VerfuegungZeitabschnitt, Verfuegung> joinVerfuegung = root.join(VerfuegungZeitabschnitt_.verfuegung);
		Join<Verfuegung, Betreuung> joinBetreuung = joinVerfuegung.join(Verfuegung_.betreuung);

		List<Predicate> predicates = new ArrayList<>();

		// Datum Von
		Predicate predicateStart = cb.lessThanOrEqualTo(root.get(VerfuegungZeitabschnitt_.gueltigkeit).get(DateRange_.gueltigAb), zeitabschnittBis);
		predicates.add(predicateStart);
		// Datum Bis
		Predicate predicateEnd = cb.greaterThanOrEqualTo(root.get(VerfuegungZeitabschnitt_.gueltigkeit).get(DateRange_.gueltigBis), zeitabschnittVon);
		predicates.add(predicateEnd);
		// Nur Angebot KITA
		Predicate predicateAngebot = cb.equal(joinBetreuung.get(Betreuung_.institutionStammdaten).get(InstitutionStammdaten_.betreuungsangebotTyp), BetreuungsangebotTyp.KITA);
		predicates.add(predicateAngebot);
		// Nur neueste Verfuegung jedes Falls beachten
		Predicate predicateGueltig = cb.equal(joinBetreuung.get(Betreuung_.gueltig), Boolean.TRUE);
		predicates.add(predicateGueltig);
		// Status der Betreuung muss VERFUEGT oder STORINERT sein
		Predicate predicateStatus = joinBetreuung.get(Betreuung_.betreuungsstatus).in(Betreuungsstatus.VERFUEGT, Betreuungsstatus.STORNIERT);
		predicates.add(predicateStatus);

		query.where(CriteriaQueryHelper.concatenateExpressions(cb, predicates));
		return persistence.getCriteriaResults(query);
	}

	/**
	 * Ermittelt alle Zeitabschnitte, welche zu Antraegen gehoeren, die seit dem letzten Zahlungslauf verfuegt wurden.
	 */
	@Nonnull
	private Collection<VerfuegungZeitabschnitt> getVerfuegungsZeitabschnitteNachVerfuegungDatum(@Nonnull LocalDateTime datumVerfuegtVon, @Nonnull LocalDateTime datumVerfuegtBis, @Nonnull LocalDate zeitabschnittBis) {
		Objects.requireNonNull(datumVerfuegtVon, "datumVerfuegtVon muss gesetzt sein");
		Objects.requireNonNull(datumVerfuegtBis, "datumVerfuegtBis muss gesetzt sein");
		Objects.requireNonNull(zeitabschnittBis, "zeitabschnittBis muss gesetzt sein");

		LOGGER.info("Ermittle Korrekturzahlungen:");
		LOGGER.info("Zeitabschnitt endet vor: " + Constants.DATE_FORMATTER.format(zeitabschnittBis));
		LOGGER.info("Gesuch verfuegt zwischen: " + Constants.DATE_FORMATTER.format(datumVerfuegtVon) + " - " + Constants.DATE_FORMATTER.format(datumVerfuegtBis));

		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<VerfuegungZeitabschnitt> query = cb.createQuery(VerfuegungZeitabschnitt.class);
		Root<VerfuegungZeitabschnitt> root = query.from(VerfuegungZeitabschnitt.class);
		Join<VerfuegungZeitabschnitt, Verfuegung> joinVerfuegung = root.join(VerfuegungZeitabschnitt_.verfuegung);
		Join<Verfuegung, Betreuung> joinBetreuung = joinVerfuegung.join(Verfuegung_.betreuung);
		Join<Betreuung, KindContainer> joinKindContainer = joinBetreuung.join(Betreuung_.kind);
		Join<KindContainer, Gesuch> joinGesuch = joinKindContainer.join(KindContainer_.gesuch);

		List<Predicate> predicates = new ArrayList<>();

		// Datum Bis muss VOR dem regulaeren Auszahlungszeitraum sein (sonst ist es keine Korrektur und schon im obigen Statement enthalten)
		Predicate predicateStart = cb.lessThanOrEqualTo(root.get(VerfuegungZeitabschnitt_.gueltigkeit).get(DateRange_.gueltigBis), zeitabschnittBis);
		predicates.add(predicateStart);
		// Nur Angebot KITA
		Predicate predicateAngebot = cb.equal(joinBetreuung.get(Betreuung_.institutionStammdaten).get(InstitutionStammdaten_.betreuungsangebotTyp), BetreuungsangebotTyp.KITA);
		predicates.add(predicateAngebot);
		// Gesuche, welche seit dem letzten Zahlungslauf verfuegt wurden. Nur neueste Verfuegung jedes Falls beachten
		Predicate predicateDatum = cb.between(joinGesuch.get(Gesuch_.timestampVerfuegt), cb.literal(datumVerfuegtVon), cb.literal(datumVerfuegtBis));
		predicates.add(predicateDatum);
		// Nur neueste Verfuegung jedes Falls beachten
		Predicate predicateGueltig = cb.equal(joinBetreuung.get(Betreuung_.gueltig), Boolean.TRUE);
		predicates.add(predicateGueltig);
		// Status der Betreuung muss VERFUEGT oder STORINERT sein
		Predicate predicateStatus = joinBetreuung.get(Betreuung_.betreuungsstatus).in(Betreuungsstatus.VERFUEGT, Betreuungsstatus.STORNIERT);
		predicates.add(predicateStatus);

		query.where(CriteriaQueryHelper.concatenateExpressions(cb, predicates));
		return persistence.getCriteriaResults(query);
	}

	/**
	 * Erstellt eine Zahlungsposition fuer den uebergebenen Zeitabschnitt. Normalfall bei "Erstbuchung"
	 */
	@Nonnull
	private Zahlungsposition createZahlungsposition(@Nonnull VerfuegungZeitabschnitt zeitabschnitt, @Nonnull Zahlungsauftrag zahlungsauftrag, @Nonnull Map<String, Zahlung> zahlungProInstitution) {
		Zahlungsposition zahlungsposition = new Zahlungsposition();
		zahlungsposition.setVerfuegungZeitabschnitt(zeitabschnitt);
		zahlungsposition.setBetrag(zeitabschnitt.getVerguenstigung());
		zahlungsposition.setStatus(ZahlungspositionStatus.NORMAL);
		Zahlung zahlung = findZahlungForInstitution(zeitabschnitt, zahlungsauftrag, zahlungProInstitution);
		zahlungsposition.setZahlung(zahlung);
		zahlung.getZahlungspositionen().add(zahlungsposition);
		zeitabschnitt.setZahlungsstatus(VerfuegungsZeitabschnittZahlungsstatus.VERRECHNET);
		return zahlungsposition;
	}

	/**
	 * Erstellt alle notwendigen Zahlungspositionen fuer die Korrektur des uebergebenen Zeitabschnitts.
	 * Bisherige Positionen werden in Abzug gebracht und die neuen hinzugefuegt
	 */
	private void createZahlungspositionenKorrekturUndNachzahlung(@Nonnull VerfuegungZeitabschnitt zeitabschnittNeu, @Nonnull Zahlungsauftrag zahlungsauftrag, @Nonnull Map<String, Zahlung> zahlungProInstitution) {
		// Ermitteln, ob die Vollkosten geaendert haben, seit der letzten Verfuegung, die auch verrechnet wurde!
		List<VerfuegungZeitabschnitt> zeitabschnittOnVorgaengerVerfuegung = verfuegungService
			.findVerrechnetenZeitabschnittOnVorgaengerVerfuegung(zeitabschnittNeu, zeitabschnittNeu.getVerfuegung().getBetreuung());
		if (!zeitabschnittOnVorgaengerVerfuegung.isEmpty()) { // Korrekturen
			BigDecimal vollkostenVorgaenger = BigDecimal.ZERO;
			BigDecimal elternbeitragVorgaenger = BigDecimal.ZERO;
			// Eventuell gab es fuer diesen Zeitabschnitt in der Vorgaengerverfuegung mehrere Abschnitte:
			// Total der Vollkosten bzw. Elternbeitraege zusammenzaehlen
			for (VerfuegungZeitabschnitt zeitabschnittBisher : zeitabschnittOnVorgaengerVerfuegung) {
				vollkostenVorgaenger = MathUtil.DEFAULT.add(vollkostenVorgaenger, zeitabschnittBisher.getVollkosten());
				elternbeitragVorgaenger = MathUtil.DEFAULT.add(elternbeitragVorgaenger, zeitabschnittBisher.getElternbeitrag());
			}
			// Und ermitteln, ob es sich im Vergleich nur neuen Verfuegung geaendert hat
			// Falls keine Aenderung -> Keine KorrekturZahlung notwendig!
			boolean vollkostenChanged = vollkostenVorgaenger != null && vollkostenVorgaenger.compareTo(zeitabschnittNeu.getVollkosten()) != 0;
			boolean elternbeitragChanged = elternbeitragVorgaenger != null && elternbeitragVorgaenger.compareTo(zeitabschnittNeu.getElternbeitrag()) != 0;
			if (vollkostenChanged || elternbeitragChanged) {
				// Irgendetwas hat geaendert, wir brauchen eine Korrekturzahlung
				Zahlung zahlung = findZahlungForInstitution(zeitabschnittNeu, zahlungsauftrag, zahlungProInstitution);
				createZahlungspositionKorrekturNeuerWert(zeitabschnittNeu, zahlung, vollkostenChanged);
				for (VerfuegungZeitabschnitt verfuegungZeitabschnitt : zeitabschnittOnVorgaengerVerfuegung) {
					// Fuer die "alten" Verfuegungszeitabschnitte muessen Korrekturbuchungen erstellt werden
					// Wenn die neuen Zeitabschnitte ignoriert sind, setzen wir die alten Zeitabschnitte auch als ignoriert
					createZahlungspositionKorrekturAlterWert(verfuegungZeitabschnitt, zahlung, vollkostenChanged,
						zeitabschnittNeu.getZahlungsstatus().isIgnoriertIgnorierend());
				}
			}
		} else { // Nachzahlungen bzw. Erstgesuche die rueckwirkend ausbezahlt werden muessen
			createZahlungsposition(zeitabschnittNeu, zahlungsauftrag, zahlungProInstitution);
		}
	}

	/**
	 * Erstellt eine Zahlungsposition fuer eine Korrekturzahlung mit dem *neu gueltigen* Wert
	 */
	private void createZahlungspositionKorrekturNeuerWert(@Nonnull VerfuegungZeitabschnitt zeitabschnitt, @Nonnull Zahlung zahlung, boolean vollkostenChanged) {
		Zahlungsposition zahlungsposition = new Zahlungsposition();
		zahlungsposition.setVerfuegungZeitabschnitt(zeitabschnitt);
		zahlungsposition.setBetrag(zeitabschnitt.getVerguenstigung());
		zahlungsposition.setZahlung(zahlung);
		zahlungsposition.setIgnoriert(zeitabschnitt.getZahlungsstatus().isIgnoriertIgnorierend());
		ZahlungspositionStatus status = vollkostenChanged ? ZahlungspositionStatus.KORREKTUR_VOLLKOSTEN : ZahlungspositionStatus.KORREKTUR_ELTERNBEITRAG;
		zahlungsposition.setStatus(status);
		if (!zeitabschnitt.getZahlungsstatus().isIgnoriertIgnorierend()) {
			zeitabschnitt.setZahlungsstatus(VerfuegungsZeitabschnittZahlungsstatus.VERRECHNET);
		}
		else if (zeitabschnitt.getZahlungsstatus().isIgnorierend()) {
			zeitabschnitt.setZahlungsstatus(VerfuegungsZeitabschnittZahlungsstatus.IGNORIERT);
		}
		zahlung.getZahlungspositionen().add(zahlungsposition);
	}

	/**
	 * Erstellt eine Zahlungsposition fuer eine Korrekturzahlung mit der Korrektur des *alten Wertes* (negiert)
	 */
	private void createZahlungspositionKorrekturAlterWert(@Nonnull VerfuegungZeitabschnitt verfuegungZeitabschnitt, @Nonnull Zahlung zahlung, boolean vollkostenChanged,
														  boolean ignoriert) {
		Zahlungsposition korrekturPosition = new Zahlungsposition();
		korrekturPosition.setVerfuegungZeitabschnitt(verfuegungZeitabschnitt);
		korrekturPosition.setBetrag(verfuegungZeitabschnitt.getVerguenstigung().negate());
		korrekturPosition.setZahlung(zahlung);
		korrekturPosition.setIgnoriert(ignoriert); // ignoriert kommt vom neuen Zeitabschnitt
		ZahlungspositionStatus status = vollkostenChanged ? ZahlungspositionStatus.KORREKTUR_VOLLKOSTEN : ZahlungspositionStatus.KORREKTUR_ELTERNBEITRAG;
		korrekturPosition.setStatus(status);
		zahlung.getZahlungspositionen().add(korrekturPosition);
	}

	/**
	 * Ermittelt das Zahlungsobjekt fuer die Institution des uebergebenen Zeitabschnitts. Falls im uebergebenen Auftrag schon eine Zahlung
	 * fuer diese Institution vorhanden ist, wird diese zurueckgegeben, ansonsten eine neue erstellt.
	 */
	@Nonnull
	private Zahlung findZahlungForInstitution(@Nonnull VerfuegungZeitabschnitt zeitabschnitt, @Nonnull Zahlungsauftrag zahlungsauftrag, @Nonnull Map<String, Zahlung> zahlungProInstitution) {
		InstitutionStammdaten institution = zeitabschnitt.getVerfuegung().getBetreuung().getInstitutionStammdaten();
		if (zahlungProInstitution.containsKey(institution.getId())) {
			return zahlungProInstitution.get(institution.getId());
		} else {
			Zahlung zahlung = createZahlung(institution, zahlungsauftrag);
			zahlungProInstitution.put(institution.getId(), zahlung);
			return zahlung;
		}
	}

	/**
	 * Erstellt eine Zahlung fuer eine bestimmte Institution, welche zum uebergebenen Auftrag gehoert
	 */
	@Nonnull
	private Zahlung createZahlung(@Nonnull InstitutionStammdaten institution, @Nonnull Zahlungsauftrag zahlungsauftrag) {
		Zahlung zahlung = new Zahlung();
		zahlung.setStatus(ZahlungStatus.ENTWURF);
		zahlung.setInstitutionStammdaten(institution);
		zahlung.setZahlungsauftrag(zahlungsauftrag);
		zahlungsauftrag.getZahlungen().add(zahlung);
		return zahlung;
	}

	/**
	 * Ermittelt den zuletzt durchgefuehrten Zahlungsauftrag
	 */
	@Override
	@Nonnull
	public Optional<Zahlungsauftrag> findLastZahlungsauftrag() {
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Zahlungsauftrag> query = cb.createQuery(Zahlungsauftrag.class);
		Root<Zahlungsauftrag> root = query.from(Zahlungsauftrag.class);
		query.orderBy(cb.desc(root.get(Zahlungsauftrag_.timestampErstellt)));
		List<Zahlungsauftrag> criteriaResults = persistence.getCriteriaResults(query, 1);
		if (!criteriaResults.isEmpty()) {
			return Optional.of(criteriaResults.get(0));
		}
		return Optional.empty();
	}

	@Override
	@Nonnull
	public Zahlungsauftrag zahlungsauftragAktualisieren(@Nonnull String auftragId, @Nonnull LocalDate datumFaelligkeit, @Nonnull String beschreibung) {
		Objects.requireNonNull(auftragId, "auftragId muss gesetzt sein");
		Objects.requireNonNull(datumFaelligkeit, "datumFaelligkeit muss gesetzt sein");
		Objects.requireNonNull(beschreibung, "beschreibung muss gesetzt sein");

		Optional<Zahlungsauftrag> auftragOptional = findZahlungsauftrag(auftragId);
		if (auftragOptional.isPresent()) {
			Zahlungsauftrag auftrag = auftragOptional.get();
			// Auftrag kann nur im Status ENTWURF veraendert werden
			if (auftrag.getStatus().isEntwurf()) {
				auftrag.setBeschrieb(beschreibung);
				auftrag.setDatumFaellig(datumFaelligkeit);
				return persistence.merge(auftrag);
			} else {
				throw new IllegalStateException("Auftrag kann nicht mehr veraendert werden: " + auftragId);
			}
		} else {
			throw new EbeguEntityNotFoundException("zahlungsauftragAktualisieren", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, auftragId);
		}
	}

	@Override
	@Nonnull
	@RolesAllowed(value = {SUPER_ADMIN, ADMIN})
	public Zahlungsauftrag zahlungsauftragAusloesen(@Nonnull String auftragId) {
		Objects.requireNonNull(auftragId, "auftragId muss gesetzt sein");
		Zahlungsauftrag zahlungsauftrag = persistence.find(Zahlungsauftrag.class, auftragId);
		zahlungsauftrag.setStatus(ZahlungauftragStatus.AUSGELOEST);
		// Jetzt muss noch das PAIN File erstellt werden. Nach dem Ausloesen kann dieses nicht mehr veraendert werden
		try {
			generatedDokumentService.getPain001DokumentAccessTokenGeneratedDokument(zahlungsauftrag, true);
		} catch (MimeTypeParseException e) {
			throw new IllegalStateException("Pain-File konnte nicht erstellt werden: " + auftragId, e);
		}
		for (Zahlung zahlung : zahlungsauftrag.getZahlungen()) {
			if (!ZahlungStatus.ENTWURF.equals(zahlung.getStatus())) {
				throw new IllegalArgumentException("Zahlung muss im Status ENTWURF sein, wenn der Auftrag ausgelöst wird: " + zahlung.getId());
			}
			zahlung.setStatus(ZahlungStatus.AUSGELOEST);
			persistence.merge(zahlung);
		}
		return persistence.merge(zahlungsauftrag);
	}

	@Override
	@Nonnull
	@RolesAllowed(value = {SUPER_ADMIN, ADMIN, SACHBEARBEITER_JA, SACHBEARBEITER_INSTITUTION, SACHBEARBEITER_TRAEGERSCHAFT, JURIST, REVISOR})
	public Optional<Zahlungsauftrag> findZahlungsauftrag(@Nonnull String auftragId) {
		Objects.requireNonNull(auftragId, "auftragId muss gesetzt sein");
		Zahlungsauftrag zahlungsauftrag = persistence.find(Zahlungsauftrag.class, auftragId);
		return Optional.ofNullable(zahlungsauftrag);
	}

	@Override
	@Nonnull
	@RolesAllowed(value = {SUPER_ADMIN, ADMIN, SACHBEARBEITER_JA, SACHBEARBEITER_INSTITUTION, SACHBEARBEITER_TRAEGERSCHAFT, JURIST, REVISOR})
	public Optional<Zahlung> findZahlung(@Nonnull String zahlungId) {
		Objects.requireNonNull(zahlungId, "zahlungId muss gesetzt sein");
		Zahlung zahlung = persistence.find(Zahlung.class, zahlungId);
		return Optional.ofNullable(zahlung);
	}

	@Override
	@RolesAllowed(value = {SUPER_ADMIN})
	public void deleteZahlungsauftrag(@Nonnull String auftragId) {
		Objects.requireNonNull(auftragId, "auftragId muss gesetzt sein");
		Optional<Zahlungsauftrag> auftragOptional = findZahlungsauftrag(auftragId);
		if (auftragOptional.isPresent()) {
			// Alle Pain-Files loeschen
			Optional<Pain001Dokument> painDokumentOptional = criteriaQueryHelper.getEntityByUniqueAttribute(Pain001Dokument.class, auftragOptional.get(), Pain001Dokument_.zahlungsauftrag);
			painDokumentOptional.ifPresent(pain001Dokument -> {
				LOGGER.info("Pain001Dokument removed: " + pain001Dokument.getId() + ". Filename (file not removed): " + pain001Dokument.getFilename());
				persistence.remove(Pain001Dokument.class, pain001Dokument.getId());
			});
			// Alle verknuepften Zeitabschnitte wieder auf "unbezahlt" setzen
			Zahlungsauftrag auftrag = auftragOptional.get();
			for (Zahlung zahlung : auftrag.getZahlungen()) {
				for (Zahlungsposition zahlungsposition : zahlung.getZahlungspositionen()) {
					if (zahlungsposition.getVerfuegungZeitabschnitt().getZahlungsstatus().isVerrechnet()) {
						zahlungsposition.getVerfuegungZeitabschnitt().setZahlungsstatus(VerfuegungsZeitabschnittZahlungsstatus.NEU);
						persistence.merge(zahlungsposition.getVerfuegungZeitabschnitt());
					}
				}
			}
		}
		// Dann erst den Auftrag loeschen
		Zahlungsauftrag auftragToRemove = auftragOptional.orElseThrow(() -> new EbeguEntityNotFoundException("deleteZahlungsauftrag", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, auftragId));
		LOGGER.info("Zahlungsauftrag and its Zahlungen removed: " + auftragToRemove.getId());
		persistence.remove(auftragToRemove);
	}

	@Override
	@Nonnull
	@RolesAllowed(value = {SUPER_ADMIN, ADMIN, SACHBEARBEITER_JA, SACHBEARBEITER_INSTITUTION, SACHBEARBEITER_TRAEGERSCHAFT, JURIST, REVISOR})
	public Collection<Zahlungsauftrag> getAllZahlungsauftraege() {
		return new ArrayList<>(criteriaQueryHelper.getAll(Zahlungsauftrag.class));
	}


	@Override
	@Nonnull
	@RolesAllowed(value = {SUPER_ADMIN, SACHBEARBEITER_INSTITUTION, SACHBEARBEITER_TRAEGERSCHAFT})
	public Zahlung zahlungBestaetigen(@Nonnull String zahlungId) {
		Objects.requireNonNull(zahlungId, "zahlungId muss gesetzt sein");
		Zahlung zahlung = persistence.find(Zahlung.class, zahlungId);
		zahlung.setStatus(ZahlungStatus.BESTAETIGT);
		Zahlung persistedZahlung = persistence.merge(zahlung);
		zahlungauftragBestaetigenIfAllZahlungenBestaetigt(zahlung.getZahlungsauftrag());
		return persistedZahlung;
	}

	@Override
	public Collection<Zahlungsauftrag> getZahlungsauftraegeInPeriode(LocalDate von, @Nonnull LocalDate bis) {

		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Zahlungsauftrag> query = cb.createQuery(Zahlungsauftrag.class);

		Root<Zahlungsauftrag> root = query.from(Zahlungsauftrag.class);
		Predicate predicates = cb.between(root.get(Zahlungsauftrag_.datumGeneriert), cb.literal(von.atStartOfDay()), cb.literal(bis.atTime(LocalTime.MAX)));

		query.where(CriteriaQueryHelper.concatenateExpressions(cb, predicates));
		return persistence.getCriteriaResults(query);

	}

	@Override
	@RolesAllowed(value = {SUPER_ADMIN})
	public void deleteZahlungspositionenOfGesuch(@Nonnull Gesuch gesuch) {
		Objects.requireNonNull(gesuch, "gesuch muss gesetzt sein");
		final CriteriaBuilder cb = persistence.getCriteriaBuilder();
		final CriteriaQuery<Zahlungsposition> query = cb.createQuery(Zahlungsposition.class);

		Root<Zahlungsposition> root = query.from(Zahlungsposition.class);
		Predicate predicates = cb.equal(root.get(Zahlungsposition_.verfuegungZeitabschnitt)
			.get(VerfuegungZeitabschnitt_.verfuegung)
			.get(Verfuegung_.betreuung)
			.get(Betreuung_.kind)
			.get(KindContainer_.gesuch), gesuch);

		query.where(predicates);
		List<Zahlungsposition> zahlungspositionList = persistence.getCriteriaResults(query);

		//remove Zahlungspositionen
		Set<Zahlung> potenziellZuLoeschenZahlungenList = new HashSet<>();
		for (Zahlungsposition zahlungsposition : zahlungspositionList) {
			potenziellZuLoeschenZahlungenList.add(zahlungsposition.getZahlung()); // add the Zahlung to the set
			zahlungsposition.getZahlung().getZahlungspositionen().remove(zahlungsposition);
			persistence.remove(Zahlungsposition.class, zahlungsposition.getId());
		}
		Set<Zahlungsauftrag> zahlungsauftraegeList = removeAllEmptyZahlungen(potenziellZuLoeschenZahlungenList);
		removeAllEmptyZahlungsauftraege(zahlungsauftraegeList);
	}

	/**
	 * Goes through the given list and check whether the given Zahlungsauftrag is empty or not.
	 * All empty Zahlungsauftraege are removed.
	 */
	private void removeAllEmptyZahlungsauftraege(Set<Zahlungsauftrag> zahlungsauftraegeList) {
		for (Zahlungsauftrag zahlungsauftrag : zahlungsauftraegeList) {
			if (zahlungsauftrag.getZahlungen().isEmpty()) {
				removePAIN001FromZahlungsauftrag(zahlungsauftrag);
				persistence.remove(Zahlungsauftrag.class, zahlungsauftrag.getId());
			}
		}
	}

	/**
	 * Removes the Pain001Dokument that is linked with the given Zahlungsauftrag if it exists.
	 */
	private void removePAIN001FromZahlungsauftrag(Zahlungsauftrag zahlungsauftrag) {
		final Collection<Pain001Dokument> pain001Dokument = criteriaQueryHelper.getEntitiesByAttribute(Pain001Dokument.class, zahlungsauftrag, Pain001Dokument_.zahlungsauftrag);
		pain001Dokument.forEach(pain -> {
			fileSaverService.removeAllFromSubfolder(pain.getZahlungsauftrag().getId());
            persistence.remove(Pain001Dokument.class, pain.getId());
		});
	}

	/**
	 * Goes through the given list and check whether the given Zahlung is empty or not.
	 * All empty Zahlungen are removed and all corresponding Zahlungsauftraege are added to the
	 * Set that will be returned at the end of the function
	 */
	@Nonnull
	private Set<Zahlungsauftrag> removeAllEmptyZahlungen(Set<Zahlung> potenziellZuLoeschenZahlungenList) {
		Set<Zahlungsauftrag> potenziellZuLoeschenZahlungsauftraegeList = new HashSet<>();
		for (Zahlung zahlung : potenziellZuLoeschenZahlungenList) {
			if (zahlung.getZahlungspositionen().isEmpty()) {
				potenziellZuLoeschenZahlungsauftraegeList.add(zahlung.getZahlungsauftrag());
				zahlung.getZahlungsauftrag().getZahlungen().remove(zahlung);
				persistence.remove(Zahlung.class, zahlung.getId());
			}
		}
		return potenziellZuLoeschenZahlungsauftraegeList;
	}

	@Nonnull
	private Zahlungsauftrag zahlungauftragBestaetigenIfAllZahlungenBestaetigt(@Nonnull Zahlungsauftrag zahlungsauftrag) {
		Objects.requireNonNull(zahlungsauftrag, "zahlungsauftrag darf nicht null sein");
		if (zahlungsauftrag.getZahlungen().stream().allMatch(zahlung -> zahlung.getStatus().equals(ZahlungStatus.BESTAETIGT))) {
			zahlungsauftrag.setStatus(ZahlungauftragStatus.BESTAETIGT);
			return persistence.merge(zahlungsauftrag);
		}
		return zahlungsauftrag;
	}

}


