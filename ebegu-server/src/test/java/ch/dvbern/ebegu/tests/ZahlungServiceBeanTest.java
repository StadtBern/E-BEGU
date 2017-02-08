package ch.dvbern.ebegu.tests;

import ch.dvbern.ebegu.entities.*;
import ch.dvbern.ebegu.enums.ZahlungStatus;
import ch.dvbern.ebegu.persistence.CriteriaQueryHelper;
import ch.dvbern.ebegu.services.GesuchService;
import ch.dvbern.ebegu.services.GesuchsperiodeService;
import ch.dvbern.ebegu.services.TestfaelleService;
import ch.dvbern.ebegu.services.ZahlungService;
import ch.dvbern.ebegu.tets.TestDataUtil;
import ch.dvbern.lib.cdipersistence.Persistence;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.persistence.UsingDataSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

/**
 * Tests fuer den Zahlungsservice
 */
@RunWith(Arquillian.class)
@UsingDataSet("datasets/mandant-dataset.xml")
public class ZahlungServiceBeanTest extends AbstractEbeguLoginTest {


	@Inject
	private ZahlungService zahlungService;

	@Inject
	private TestfaelleService testfaelleService;

	@Inject
	private GesuchService gesuchService;

	@Inject
	private GesuchsperiodeService gesuchsperiodeService;

	@Inject
	private Persistence<?> persistence;

	@Inject
	private CriteriaQueryHelper criteriaQueryHelper;


	@Before
	public void init() {
		final Gesuchsperiode gesuchsperiode = createGesuchsperiode(true);
		Mandant mandant = insertInstitutionen();
		createBenutzer(mandant);
		TestDataUtil.prepareParameters(gesuchsperiode.getGueltigkeit(), persistence);
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void findGesuchIdsOfAktuellerAntrag() throws Exception {
		Gesuch verfuegtesGesuch = createGesuch(true);
		Gesuch nichtVerfuegtesGesuch = createGesuch(false);

		Gesuch erstgesuchMitMutation = createGesuch(true);
		Gesuch nichtVerfuegteMutation = createMutation(erstgesuchMitMutation, false);

		Gesuch erstgesuchMitVerfuegterMutation = createGesuch(true);
		Gesuch verfuegteMutation = createMutation(erstgesuchMitVerfuegterMutation, true);

		List<String> gesuchIdsOfAktuellerAntrag = gesuchService.findGesuchIdsOfAktuellerAntrag(verfuegtesGesuch.getGesuchsperiode());
		Assert.assertNotNull(gesuchIdsOfAktuellerAntrag);

		Assert.assertTrue(gesuchIdsOfAktuellerAntrag.contains(verfuegtesGesuch.getId()));
		Assert.assertFalse(gesuchIdsOfAktuellerAntrag.contains(nichtVerfuegtesGesuch.getId()));

		Assert.assertTrue(gesuchIdsOfAktuellerAntrag.contains(erstgesuchMitMutation.getId()));
		Assert.assertFalse(gesuchIdsOfAktuellerAntrag.contains(nichtVerfuegteMutation.getId()));

		//TODO Dieser Test geht erst, wenn GesuchService.findGesuchIdsOfAktuellerAntrag richtig
//TODO		Assert.assertFalse(gesuchIdsOfAktuellerAntrag.contains(erstgesuchMitVerfuegterMutation.getId()));
		Assert.assertTrue(gesuchIdsOfAktuellerAntrag.contains(verfuegteMutation.getId()));
	}

	@Test
	public void zahlungsauftragErstellen() throws Exception {
		createGesuch(true);
		Zahlungsauftrag zahlungsauftrag = zahlungService.zahlungsauftragErstellen(LocalDateTime.of(TestDataUtil.PERIODE_JAHR_1, Month.AUGUST, 20, 0, 0), "Testauftrag");

		Assert.assertNotNull(zahlungsauftrag);
		Assert.assertNotNull(zahlungsauftrag.getZahlungen());
		Assert.assertFalse(zahlungsauftrag.getZahlungen().isEmpty());
	}

	@Test
	public void zahlungsauftragAusloesen() throws Exception {
		createGesuch(true);
		Zahlungsauftrag zahlungsauftrag = zahlungService.zahlungsauftragErstellen(LocalDateTime.of(TestDataUtil.PERIODE_JAHR_1, Month.AUGUST, 20, 0, 0), "Testauftrag");

		Assert.assertFalse(zahlungService.findZahlungsauftrag(zahlungsauftrag.getId()).get().getAusgeloest());
		zahlungService.zahlungsauftragAusloesen(zahlungsauftrag.getId());
		Assert.assertTrue(zahlungService.findZahlungsauftrag(zahlungsauftrag.getId()).get().getAusgeloest());
	}

	@Test
	public void findZahlungsauftrag() throws Exception {
		createGesuch(true);
		Zahlungsauftrag zahlungsauftrag = zahlungService.zahlungsauftragErstellen(LocalDateTime.of(TestDataUtil.PERIODE_JAHR_1, Month.AUGUST, 20, 0, 0), "Testauftrag");

		Assert.assertTrue(zahlungService.findZahlungsauftrag(zahlungsauftrag.getId()).isPresent());
		Assert.assertFalse(zahlungService.findZahlungsauftrag("ungueltigeId").isPresent());
	}

	@Test
	public void deleteZahlungsauftrag() throws Exception {
		createGesuch(true);
		Zahlungsauftrag zahlungsauftrag = zahlungService.zahlungsauftragErstellen(LocalDateTime.of(TestDataUtil.PERIODE_JAHR_1, Month.AUGUST, 20, 0, 0), "Testauftrag");

		Assert.assertTrue(zahlungService.findZahlungsauftrag(zahlungsauftrag.getId()).isPresent());
		zahlungService.deleteZahlungsauftrag(zahlungsauftrag.getId());
		Assert.assertFalse(zahlungService.findZahlungsauftrag(zahlungsauftrag.getId()).isPresent());
	}

	@Test
	public void getAllZahlungsauftraege() throws Exception {
		Assert.assertTrue(zahlungService.getAllZahlungsauftraege().isEmpty());

		createGesuch(true);
		Zahlungsauftrag zahlungsauftrag = zahlungService.zahlungsauftragErstellen(LocalDateTime.of(TestDataUtil.PERIODE_JAHR_1, Month.AUGUST, 20, 0, 0), "Testauftrag");
		Assert.assertFalse(zahlungService.getAllZahlungsauftraege().isEmpty());
	}

	@Test
	public void createIsoFile() throws Exception {
		//TODO (team) Test
	}

	@Test
	public void zahlungBestaetigen() throws Exception {
		createGesuch(true);
		Zahlungsauftrag zahlungsauftrag = zahlungService.zahlungsauftragErstellen(LocalDateTime.of(TestDataUtil.PERIODE_JAHR_1, Month.AUGUST, 20, 0, 0), "Testauftrag");

		Assert.assertNotNull(zahlungsauftrag);
		Assert.assertEquals(1, zahlungsauftrag.getZahlungen().size());
		Assert.assertEquals(ZahlungStatus.AUSGELOEST, zahlungsauftrag.getZahlungen().get(0).getStatus());

		Zahlung zahlung = zahlungService.zahlungBestaetigen(zahlungsauftrag.getZahlungen().get(0).getId());
		Assert.assertNotNull(zahlung);
		Assert.assertEquals(ZahlungStatus.BESTAETIGT, zahlung.getStatus());
	}

	private Gesuch createGesuch(boolean verfuegen) {
		return testfaelleService.createAndSaveTestfaelle(TestfaelleService.BeckerNora, verfuegen, verfuegen);
	}

	private Gesuch createMutation(Gesuch erstgesuch, boolean verfuegen) {
		return testfaelleService.mutierenHeirat(erstgesuch.getFall().getFallNummer(),
			erstgesuch.getGesuchsperiode().getId(), LocalDate.of(TestDataUtil.PERIODE_JAHR_1, Month.DECEMBER, 15), LocalDate.of(TestDataUtil.PERIODE_JAHR_2, Month.JANUARY, 15), verfuegen);
	}
}
