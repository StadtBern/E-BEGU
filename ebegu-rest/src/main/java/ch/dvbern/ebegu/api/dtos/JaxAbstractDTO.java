package ch.dvbern.ebegu.api.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ch.dvbern.lib.date.converters.LocalDateTimeXMLConverter;

@XmlTransient
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class JaxAbstractDTO implements Serializable, Comparable<JaxAbstractDTO> {

	private static final long serialVersionUID = 7069586216789441112L;

	@Nullable
	private String id = null;

	@Nullable
	@XmlJavaTypeAdapter(LocalDateTimeXMLConverter.class)
	private LocalDateTime timestampErstellt;

	@Nullable
	@XmlJavaTypeAdapter(LocalDateTimeXMLConverter.class)
	private LocalDateTime timestampMutiert;

	@Nullable
	private String vorgaengerId;

	protected JaxAbstractDTO() {
		// nop
	}

	protected JaxAbstractDTO(@Nonnull String id) {
		this.id = Objects.requireNonNull(id);
	}

	@Nullable
	public String getId() {
		return id;
	}

	public void setId(@Nonnull String id) {
		this.id = Objects.requireNonNull(id);
	}

	@Nullable
	public LocalDateTime getTimestampErstellt() {
		return timestampErstellt;
	}

	public void setTimestampErstellt(@Nullable LocalDateTime timestampErstellt) {
		this.timestampErstellt = timestampErstellt;
	}

	@Nullable
	public LocalDateTime getTimestampMutiert() {
		return timestampMutiert;
	}

	public void setTimestampMutiert(@Nullable LocalDateTime timestampMutiert) {
		this.timestampMutiert = timestampMutiert;
	}

	@Nullable
	public String getVorgaengerId() {
		return vorgaengerId;
	}

	public void setVorgaengerId(@Nullable String vorgaengerId) {
		this.vorgaengerId = vorgaengerId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof JaxAbstractDTO)) {
			return false;
		}

		JaxAbstractDTO that = (JaxAbstractDTO) o;
		return Objects.equals(getId(), that.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}

	/**
	 * Most simple default-ordering: sort by id
	 */
	@Override
	public int compareTo(@Nonnull JaxAbstractDTO o) {
		Objects.requireNonNull(getTimestampErstellt());
		Objects.requireNonNull(o.getTimestampErstellt());
		return getTimestampErstellt().compareTo(o.getTimestampErstellt());
	}

}
