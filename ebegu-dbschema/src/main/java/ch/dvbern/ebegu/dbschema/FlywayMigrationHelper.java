package ch.dvbern.ebegu.dbschema;

import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ch.dvbern.ebegu.enums.UserRoleName;

/**
 * Unterstuetz die Java basierte Migration von Datenbestaenden.
 *
 * Es ist wichtig pro FlyWay Migration nur einen Call von {@link #migrate} zu verwenden,
 * da die Migration sonst gegebenenfalls nicht ein komplettes Rollback unterstuetzt.
 */
@Stateless
@RunAs(UserRoleName.SUPER_ADMIN)
@PermitAll
public class FlywayMigrationHelper {

	@PersistenceContext(unitName = "ebeguPersistenceUnit")
	EntityManager em;

	@Resource
	private SessionContext ctx;

	/**
	 * Wenn diese Methode ausserhalb eines EJB Contexts ausgefuehrt wird, was bei der Migration von FlyWay der Fall ist,
	 * so wird die @RunAs(SUPER_ADMIN) Annotation ignoriert. Diese Methode holt sich deshalb via SessionContext eine
	 * Instanz des FlywayMigrationHelper, welcher dann die korrekte Rolle erhaelt.
	 *
	 * Mit CDI<Object>.select(MyService.class) koennen Services, welche fuer die Migration benoetigt werden injected werden.
	 *
	 * @param consumer die Funktion, welche die Migrierung durchfuert
	 */
	public void migrate(@Nonnull BiConsumer<CDI<Object>, EntityManager> consumer) {
		ctx.getBusinessObject(FlywayMigrationHelper.class).migrateInternal(consumer);
	}

	// Muss leider public sein, sollte aber nicht verwendet werden
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void migrateInternal(@Nonnull BiConsumer<CDI<Object>, EntityManager> consumer) {
		consumer.accept(CDI.current(), em);
	}
}
