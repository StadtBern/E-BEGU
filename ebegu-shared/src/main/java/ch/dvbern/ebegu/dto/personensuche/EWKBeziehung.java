
package ch.dvbern.ebegu.dto.personensuche;

import java.time.LocalDate;


/**
 * DTO für Adressen aus dem EWK
 */
public class EWKBeziehung {

    protected String beziehungstyp;
    protected String beziehungstypTxt;
    protected String personID;
    protected String nachname;
    protected String ledigname;
    protected String vorname;
    protected String rufname;
    protected LocalDate geburtsdatum;
    protected EWKAdresse adresse;


	public String getBeziehungstyp() {
		return beziehungstyp;
	}

	public void setBeziehungstyp(String beziehungstyp) {
		this.beziehungstyp = beziehungstyp;
	}

	public String getBeziehungstypTxt() {
		return beziehungstypTxt;
	}

	public void setBeziehungstypTxt(String beziehungstypTxt) {
		this.beziehungstypTxt = beziehungstypTxt;
	}

	public String getPersonID() {
		return personID;
	}

	public void setPersonID(String personID) {
		this.personID = personID;
	}

	public String getNachname() {
		return nachname;
	}

	public void setNachname(String nachname) {
		this.nachname = nachname;
	}

	public String getLedigname() {
		return ledigname;
	}

	public void setLedigname(String ledigname) {
		this.ledigname = ledigname;
	}

	public String getVorname() {
		return vorname;
	}

	public void setVorname(String vorname) {
		this.vorname = vorname;
	}

	public String getRufname() {
		return rufname;
	}

	public void setRufname(String rufname) {
		this.rufname = rufname;
	}

	public LocalDate getGeburtsdatum() {
		return geburtsdatum;
	}

	public void setGeburtsdatum(LocalDate geburtsdatum) {
		this.geburtsdatum = geburtsdatum;
	}

	public EWKAdresse getAdresse() {
		return adresse;
	}

	public void setAdresse(EWKAdresse adresse) {
		this.adresse = adresse;
	}
}
