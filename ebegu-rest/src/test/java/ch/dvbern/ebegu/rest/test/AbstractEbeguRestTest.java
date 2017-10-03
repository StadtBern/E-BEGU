package ch.dvbern.ebegu.rest.test;

import java.io.File;

import javax.annotation.Nullable;
import javax.inject.Inject;

import ch.dvbern.ebegu.api.dtos.JaxGesuchsperiode;
import ch.dvbern.ebegu.api.resource.GesuchsperiodeResource;
import ch.dvbern.ebegu.api.resource.authentication.AuthResource;
import ch.dvbern.ebegu.entities.AbstractEntity;
import ch.dvbern.ebegu.enums.GesuchsperiodeStatus;
import ch.dvbern.ebegu.tets.util.LoginmoduleAndCacheSetupTask;
import ch.dvbern.lib.cdipersistence.ISessionContextService;
import ch.dvbern.lib.cdipersistence.Persistence;
import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.persistence.UsingDataSet;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.RejectDependenciesStrategy;

/**
 * Diese Klasse implementiert die Methode "Deployment" fuer die Arquillian Tests und muss
 * von allen Testklassen in REST modul erweitert werden. Es verhaelt sich leicht anders als die Basisklasse in
 * AbstractEbeguTest
 */
@ArquillianSuiteDeployment
@UsingDataSet("datasets/empty.xml")
@Transactional(TransactionMode.DISABLED)
@ServerSetup(LoginmoduleAndCacheSetupTask.class)
public abstract class AbstractEbeguRestTest {


	@Inject
	private GesuchsperiodeResource gesuchsperiodeResource;


	@Deployment
	@OverProtocol("Servlet 3.0")
	public static Archive<?> createTestArchive() {

		return createTestArchive(null);
	}

	public static Archive<?> createTestArchive(@Nullable Class[] classesToAdd) {

		PomEquippedResolveStage pom = Maven.resolver().loadPomFromFile("pom.xml");
		File[] runtimeDeps = pom.importRuntimeDependencies().resolve()
			.using(new RejectDependenciesStrategy(false, "ch.dvbern.ebegu:ebegu-dbschema")) //wir wollen flyway nicht im test
			.asFile();
		File[] testDeps = pom.importTestDependencies().resolve().withoutTransitivity().asFile();


		// wir fuegen die packages einzeln hinzu weil sonst klassen die im shared sind und das gleiche package haben doppelt eingefuegt werden
		WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "rest-test.war")

			.addClasses(AbstractEbeguRestLoginTest.class, Persistence.class,
				ISessionContextService.class, AbstractEntity.class)

			.addPackages(true, "ch/dvbern/ebegu/api")
			.addPackages(true, "ch/dvbern/ebegu/rest/test")
			.addAsLibraries(runtimeDeps)
			.addAsLibraries(testDeps)
			.addAsManifestResource("META-INF/TEST-MANIFEST.MF", "MANIFEST.MF")

			// entfernt unnoetige Klassen, die vielleicht Dependency-Konflikten ergeben wuerden, login erfolgt im test nicht ueber openam
			.deleteClass(AuthResource.class)

			.addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
			.addAsWebInfResource("META-INF/test-beans.xml", "beans.xml")
			.addAsResource("META-INF/test-orm.xml", "META-INF/orm.xml")
			//deploy our test loginmodule
			.addAsResource("testogin-users.properties","users.properties")
			.addAsResource("testlogin-roles.properties", "roles.properties")
			.addAsWebInfResource("META-INF/test-jboss-web.xml",  "jboss-web.xml")
			// Deploy our test datasource
			.addAsWebInfResource("test-ds.xml");

		if (classesToAdd != null) {
			webArchive.addClasses(classesToAdd);
		}
		//Folgende Zeile gibt im /tmp dir das archiv aus zum debuggen nuetzlich
		new ZipExporterImpl(webArchive).exportTo(new File(System.getProperty("java.io.tmpdir"), "myWebRestArchive.war"), true);
		return webArchive;
	}

	public JaxGesuchsperiode saveGesuchsperiodeInStatusEntwurf(JaxGesuchsperiode gesuchsperiode) {
		gesuchsperiode.setStatus(GesuchsperiodeStatus.ENTWURF);
		return gesuchsperiodeResource.saveGesuchsperiode(gesuchsperiode, null, null);
	}

	public JaxGesuchsperiode saveGesuchsperiodeInStatusAktiv(JaxGesuchsperiode gesuchsperiode) {
		gesuchsperiode.setStatus(GesuchsperiodeStatus.ENTWURF);
		gesuchsperiode = gesuchsperiodeResource.saveGesuchsperiode(gesuchsperiode, null, null);
		gesuchsperiode.setStatus(GesuchsperiodeStatus.AKTIV);
		return gesuchsperiodeResource.saveGesuchsperiode(gesuchsperiode, null, null);
	}

	public JaxGesuchsperiode saveGesuchsperiodeInStatusInaktiv(JaxGesuchsperiode gesuchsperiode) {
		gesuchsperiode.setStatus(GesuchsperiodeStatus.ENTWURF);
		gesuchsperiode = gesuchsperiodeResource.saveGesuchsperiode(gesuchsperiode, null, null);
		gesuchsperiode.setStatus(GesuchsperiodeStatus.AKTIV);
		gesuchsperiode = gesuchsperiodeResource.saveGesuchsperiode(gesuchsperiode, null, null);
		gesuchsperiode.setStatus(GesuchsperiodeStatus.INAKTIV);
		return gesuchsperiodeResource.saveGesuchsperiode(gesuchsperiode, null, null);
	}

	public JaxGesuchsperiode saveGesuchsperiodeInStatusGesperrt(JaxGesuchsperiode gesuchsperiode) {
		gesuchsperiode.setStatus(GesuchsperiodeStatus.ENTWURF);
		gesuchsperiode = gesuchsperiodeResource.saveGesuchsperiode(gesuchsperiode, null, null);
		gesuchsperiode.setStatus(GesuchsperiodeStatus.AKTIV);
		gesuchsperiode = gesuchsperiodeResource.saveGesuchsperiode(gesuchsperiode, null, null);
		gesuchsperiode.setStatus(GesuchsperiodeStatus.INAKTIV);
		gesuchsperiode = gesuchsperiodeResource.saveGesuchsperiode(gesuchsperiode, null, null);
		gesuchsperiode.setStatus(GesuchsperiodeStatus.GESCHLOSSEN);
		return gesuchsperiodeResource.saveGesuchsperiode(gesuchsperiode, null, null);
	}
}
