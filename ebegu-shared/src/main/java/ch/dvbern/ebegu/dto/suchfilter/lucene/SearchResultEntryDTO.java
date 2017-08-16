package ch.dvbern.ebegu.dto.suchfilter.lucene;

import ch.dvbern.ebegu.dto.JaxAntragDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resultatdto fuer ein einzelnes Resultat aus dem Lucene index
 */
public class SearchResultEntryDTO implements Serializable {

	private static final long serialVersionUID = 1633427122097823502L;

	@Nonnull
	private final SearchEntityType entity;
	@Nonnull
	private final String resultId;
	@Nonnull
	private final String text;
	@Nullable
	private final String additionalInformation;
	@Nullable
	private String gesuchID;
	@Nullable
	private String fallID;

	@Nullable
	private JaxAntragDTO antragDTO; //dto mit detailinfos


	public SearchResultEntryDTO(
		@Nonnull SearchEntityType entity,
		@Nonnull String resultId,
		@Nonnull String text,
		@Nullable String additionalInformation,
		@Nullable String gesuchID,
		@Nullable String fallID) {

		this.entity = entity;
		this.resultId = resultId;
		this.text = text;
		this.additionalInformation = additionalInformation;
		this.gesuchID = gesuchID;
		this.fallID = fallID;
	}

	@Nonnull
	public SearchEntityType getEntity() {
		return entity;
	}

	@Nonnull
	public String getResultId() {
		return resultId;
	}

	@Nonnull
	public String getText() {
		return text;
	}

	@Nullable
	public String getAdditionalInformation() {
		return additionalInformation;
	}

	@SuppressFBWarnings("NM_CONFUSING")
	public String getGesuchID() {
		return gesuchID;
	}

	@SuppressFBWarnings("NM_CONFUSING")
	public void setGesuchID(@Nullable String gesuchID) {
		this.gesuchID = gesuchID;
	}

	@Nullable
	public String getFallID() {
		return fallID;
	}

	public void setFallID(@Nullable String fallID) {
		this.fallID = fallID;
	}

	public static List<SearchResultEntryDTO> convertSearchResult(SearchFilter filter, List<Searchable> results) {
		return results.stream().map(f -> new SearchResultEntryDTO(
			filter.getSearchEntityType(),
			f.getSearchResultId(),
			f.getSearchResultSummary(),
			f.getSearchResultAdditionalInformation(),
			f.getOwningGesuchId(),
			f.getOwningFallId())
		)
			.collect(Collectors.toList());
	}

	@Nullable
	public JaxAntragDTO getAntragDTO() {
		return antragDTO;
	}

	public void setAntragDTO(@Nullable JaxAntragDTO antragDTO) {
		this.antragDTO = antragDTO;
	}
}
