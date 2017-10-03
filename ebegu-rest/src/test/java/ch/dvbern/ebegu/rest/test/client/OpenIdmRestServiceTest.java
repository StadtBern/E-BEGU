package ch.dvbern.ebegu.rest.test.client;

import java.util.Optional;

import javax.inject.Inject;

import ch.dvbern.ebegu.api.client.JaxOpenIdmResponse;
import ch.dvbern.ebegu.api.client.JaxOpenIdmResult;
import ch.dvbern.ebegu.api.client.OpenIdmRestService;
import ch.dvbern.ebegu.api.util.OpenIDMUtil;
import ch.dvbern.ebegu.entities.Institution;
import ch.dvbern.ebegu.entities.Traegerschaft;
import ch.dvbern.ebegu.rest.test.AbstractEbeguRestLoginTest;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.persistence.UsingDataSet;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testet die Verbindung zum OpenIdm Server.
 * Tests sind default ignored, damit wir zum Testen unserer Applikation nicht auf den OpenIdm Server angewiesen sind
 */

@RunWith(Arquillian.class)
@UsingDataSet("datasets/mandant-dataset.xml")
@Transactional(TransactionMode.DISABLED)
public class OpenIdmRestServiceTest extends AbstractEbeguRestLoginTest {


	private static final String INSTID1 = "b4dc7d30-b63e-4176-a166-00deadbeef01";
	private static final String INSTID2 = "b4dc7d30-b63e-4176-a166-00deadbeef02";
	private static final String TRAEGERSCHAFTID1 = "b4dc7d30-b63e-4176-a166-00deadbeef01";

	@Inject
	private OpenIdmRestService openIdmRestService;

	//Be careful, this test create and delete Institutions on openIdm server
	@Test
	@Ignore
	public void testOpenIdmInterface() {
		try {
			testOpenIdmCreate();
			printOpenIdmGetAll();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			testOpenIdmDelete();
		}
	}

	//Be careful, this test delete all Institutions and Traegerschaften on openIdm server
	@Test
	@Ignore
	public void deleteAllOnOpenIdm() {
		final Optional<JaxOpenIdmResponse> all = openIdmRestService.getAll(true);
		Assert.assertTrue(all.isPresent());

		StringBuilder sb = new StringBuilder();

		for (JaxOpenIdmResult jaxOpenIdmResult : all.get().getResult()) {

			sb.append("Delete: ");
			sb.append(jaxOpenIdmResult.get_id()).append(" ");
			sb.append(jaxOpenIdmResult.getName()).append(" ");
			sb.append(jaxOpenIdmResult.getType()).append(" ");
			final boolean success = openIdmRestService.deleteByOpenIdmUid(jaxOpenIdmResult.get_id(), true);
			if (success) {
				sb.append(" -> success");
			} else {
				sb.append(" -> failed");
			}
			sb.append(System.getProperty("line.separator"));
		}
		System.out.println(sb.toString());
	}

	public void testOpenIdmCreate() {

		//Create
		Institution institution1 = new Institution();
		institution1.setName("testInstitution1");
		institution1.setId(INSTID1);
		final Optional<JaxOpenIdmResult> optInstitution1 = openIdmRestService.createInstitution(institution1, true);
		Assert.assertTrue(optInstitution1.isPresent());

		Institution institution2 = new Institution();
		institution2.setName("testInstitution2");
		institution2.setId(INSTID2);
		final Optional<JaxOpenIdmResult> optInstitution2 = openIdmRestService.createInstitution(institution2, true);
		Assert.assertTrue(optInstitution2.isPresent());

		Traegerschaft traegerschaft = new Traegerschaft();
		traegerschaft.setName("testTraegerschaft1");
		traegerschaft.setId(TRAEGERSCHAFTID1);
		final Optional<JaxOpenIdmResult> optTaegerschaft = openIdmRestService.createTraegerschaft(traegerschaft, true);
		Assert.assertTrue(optTaegerschaft.isPresent());

		//get and check
		final Optional<JaxOpenIdmResult> institutionByUid1 = openIdmRestService.getInstitutionByUid(institution1.getId(), true);
		Assert.assertTrue(institutionByUid1.isPresent());
		Assert.assertEquals(institution1.getId(), OpenIDMUtil.convertToEBEGUID(institutionByUid1.get().get_id()));
		Assert.assertEquals(institution1.getName(), institutionByUid1.get().getName());

		final Optional<JaxOpenIdmResult> institutionByUid2 = openIdmRestService.getInstitutionByUid(institution2.getId(), true);
		Assert.assertTrue(institutionByUid2.isPresent());
		Assert.assertEquals(institution2.getId(), OpenIDMUtil.convertToEBEGUID(institutionByUid2.get().get_id()));
		Assert.assertEquals(institution2.getName(), institutionByUid2.get().getName());

		final Optional<JaxOpenIdmResult> traegerschaftByUid = openIdmRestService.getTraegerschaftByUid(traegerschaft.getId(), true);
		Assert.assertTrue(traegerschaftByUid.isPresent());
		Assert.assertEquals(traegerschaft.getId(), OpenIDMUtil.convertToEBEGUID(traegerschaftByUid.get().get_id()));
		Assert.assertEquals(traegerschaft.getName(), traegerschaftByUid.get().getName());

		//Update
		institution1.setName("testUpdateInstitution1");
		Optional<JaxOpenIdmResult> optUpdateInstitution1 = openIdmRestService.updateInstitution(institution1, true);
		Assert.assertTrue(optUpdateInstitution1.isPresent());

		traegerschaft.setName("testUpdateTraegerschaft");
		Optional<JaxOpenIdmResult> optUpdateTraegerschaft1 = openIdmRestService.updateTraegerschaft(traegerschaft, true);
		Assert.assertTrue(optUpdateTraegerschaft1.isPresent());

		//get and check Updated
		optUpdateInstitution1 = openIdmRestService.getInstitutionByUid(institution1.getId(), true);
		Assert.assertTrue(optUpdateInstitution1.isPresent());
		Assert.assertEquals(institution1.getId(), OpenIDMUtil.convertToEBEGUID(optUpdateInstitution1.get().get_id()));
		Assert.assertEquals(institution1.getName(), optUpdateInstitution1.get().getName());

		optUpdateTraegerschaft1 = openIdmRestService.getTraegerschaftByUid(traegerschaft.getId(), true);
		Assert.assertTrue(optUpdateTraegerschaft1.isPresent());
		Assert.assertEquals(traegerschaft.getId(), OpenIDMUtil.convertToEBEGUID(optUpdateTraegerschaft1.get().get_id()));
		Assert.assertEquals(traegerschaft.getName(), optUpdateTraegerschaft1.get().getName());
	}

	public void printOpenIdmGetAll() {
		final Optional<JaxOpenIdmResponse> all = openIdmRestService.getAll(true);
		Assert.assertTrue(all.isPresent());

		StringBuilder sb = new StringBuilder();
		for (JaxOpenIdmResult jaxOpenIdmResult : all.get().getResult()) {
			sb.append(jaxOpenIdmResult.get_id()).append(", ");
			sb.append(jaxOpenIdmResult.getName()).append(", ");
			sb.append(jaxOpenIdmResult.getType()).append(", ");
			sb.append(jaxOpenIdmResult.getMail());
			sb.append(System.getProperty("line.separator"));
		}
		System.out.println(sb.toString());
	}

	public void testOpenIdmDelete() {
		boolean sucess = openIdmRestService.deleteInstitution(INSTID1, true);
		Assert.assertTrue(sucess);

		sucess = openIdmRestService.deleteInstitution(INSTID2, true);
		Assert.assertTrue(sucess);

		sucess = openIdmRestService.deleteTraegerschaft(TRAEGERSCHAFTID1, true);
		Assert.assertTrue(sucess);
	}

}
