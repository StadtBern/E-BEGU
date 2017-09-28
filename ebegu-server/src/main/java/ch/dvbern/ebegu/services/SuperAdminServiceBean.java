package ch.dvbern.ebegu.services;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;

import ch.dvbern.ebegu.entities.Benutzer;
import ch.dvbern.ebegu.entities.Fall;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.enums.UserRoleName;

import static ch.dvbern.ebegu.enums.UserRoleName.ADMIN;
import static ch.dvbern.ebegu.enums.UserRoleName.GESUCHSTELLER;
import static ch.dvbern.ebegu.enums.UserRoleName.SACHBEARBEITER_JA;
import static ch.dvbern.ebegu.enums.UserRoleName.SUPER_ADMIN;

/**
 * Interface um gewisse Services als SUPER_ADMIN aufrufen zu koennen
 */
@Stateless
@Local(SuperAdminService.class)
@RunAs(value = UserRoleName.SUPER_ADMIN)
public class SuperAdminServiceBean implements SuperAdminService {

	@Inject
	private GesuchService gesuchService;

	@Inject
	private FallService fallService;

	@Override
	@RolesAllowed({ GESUCHSTELLER, SUPER_ADMIN, ADMIN })
	public void removeGesuch(@Nonnull String gesuchId) {
		gesuchService.removeGesuch(gesuchId);
	}

	@Override
	@RolesAllowed({ SUPER_ADMIN, ADMIN })
	public void removeFall(@Nonnull Fall fall) {
		fallService.removeFall(fall);
	}

	@Override
	@Nonnull
	@RolesAllowed(value = {SUPER_ADMIN, ADMIN, SACHBEARBEITER_JA})
	public Gesuch updateGesuch(@Nonnull Gesuch gesuch, boolean saveInStatusHistory, Benutzer saveAsUser) {
		return gesuchService.updateGesuch(gesuch, saveInStatusHistory, saveAsUser);
	}
}
