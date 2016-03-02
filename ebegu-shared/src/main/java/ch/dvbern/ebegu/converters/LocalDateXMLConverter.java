/*
 * Copyright (c) 2015 DV Bern AG, Switzerland
 *
 * Das vorliegende Dokument, einschliesslich aller seiner Teile, ist urheberrechtlich
 * geschuetzt. Jede Verwertung ist ohne Zustimmung der DV Bern AG unzulaessig. Dies gilt
 * insbesondere fuer Vervielfaeltigungen, die Einspeicherung und Verarbeitung in
 * elektronischer Form. Wird das Dokument einem Kunden im Rahmen der Projektarbeit zur
 * Ansicht uebergeben ist jede weitere Verteilung durch den Kunden an Dritte untersagt.
 */
package ch.dvbern.ebegu.converters;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.persistence.Converter;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDate;

/**
 * Konvertiert ein LocalDate Java 8 Objekt in einen String fuer JSON
 */
@Converter(autoApply = true)
@XmlJavaTypeAdapter(value = LocalDateXMLConverter.class, type = LocalDate.class)
public class LocalDateXMLConverter extends XmlAdapter<String, LocalDate>  {

	@Nullable
	@Override
	public LocalDate unmarshal(String v) {
		return StringUtils.isEmpty(v) ? null : LocalDate.parse(v);
	}

	@Nullable
	@Override
	public String marshal(LocalDate v) {
		return v == null ? null : v.toString();
	}
}