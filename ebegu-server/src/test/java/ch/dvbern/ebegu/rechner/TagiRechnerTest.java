package ch.dvbern.ebegu.rechner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import ch.dvbern.ebegu.entities.Verfuegung;
import ch.dvbern.ebegu.entities.VerfuegungZeitabschnitt;
import org.junit.Assert;
import org.junit.Test;

/**
 * Testet den Tagi-Rechner
 */
public class TagiRechnerTest extends AbstractBGRechnerTest {

	private final BGRechnerParameterDTO parameterDTO = getParameter();
	private final TagiRechner tagiRechner = new TagiRechner();

	@Test
	public void testEinTagHohesEinkommenAnspruch15() {
		Verfuegung verfuegung = prepareVerfuegungTagiUndTageseltern(
			LocalDate.of(2016, Month.JANUARY, 21), LocalDate.of(2016, Month.JANUARY, 21),
			15, new BigDecimal("234567"));

		VerfuegungZeitabschnitt calculate = tagiRechner.calculate(verfuegung.getZeitabschnitte().get(0), verfuegung, parameterDTO);
		Assert.assertEquals(new BigDecimal("11.90"), calculate.getVollkosten());
		Assert.assertEquals(new BigDecimal("11.90"), calculate.getElternbeitrag());
		Assert.assertEquals(new BigDecimal("0.00"), calculate.getVerguenstigung());
	}

	@Test
	public void testTeilmonatMittleresEinkommen() {
		Verfuegung verfuegung = prepareVerfuegungTagiUndTageseltern(
			LocalDate.of(2016, Month.JANUARY, 21), LocalDate.of(2016, Month.JANUARY, 31),
			100, new BigDecimal("87654"));

		VerfuegungZeitabschnitt calculate = tagiRechner.calculate(verfuegung.getZeitabschnitte().get(0), verfuegung, parameterDTO);
		Assert.assertEquals(new BigDecimal("555.80"), calculate.getVollkosten());
		Assert.assertEquals(new BigDecimal("237.30"), calculate.getElternbeitrag());
		Assert.assertEquals(new BigDecimal("318.50"), calculate.getVerguenstigung());
	}

	@Test
	public void testTeilmonatMittleresEinkommen50() {
		Verfuegung verfuegung = prepareVerfuegungTagiUndTageseltern(
			LocalDate.of(2016, Month.JANUARY, 21), LocalDate.of(2016, Month.JANUARY, 31),
			50, new BigDecimal("87654"));

		VerfuegungZeitabschnitt calculate = tagiRechner.calculate(verfuegung.getZeitabschnitte().get(0), verfuegung, parameterDTO);
		Assert.assertEquals(new BigDecimal("277.90"), calculate.getVollkosten());
		Assert.assertEquals(new BigDecimal("118.65"), calculate.getElternbeitrag());
		Assert.assertEquals(new BigDecimal("159.25"), calculate.getVerguenstigung());
	}

	@Test
	public void testGanzerMonatZuWenigEinkommen() {
		Verfuegung verfuegung = prepareVerfuegungTagiUndTageseltern(
			LocalDate.of(2016, Month.JANUARY, 1), LocalDate.of(2016, Month.JANUARY, 31),
			100, new BigDecimal("27750"));

		VerfuegungZeitabschnitt calculate = tagiRechner.calculate(verfuegung.getZeitabschnitte().get(0), verfuegung, parameterDTO);
		Assert.assertEquals(new BigDecimal("1667.40"), calculate.getVollkosten());
		Assert.assertEquals(new BigDecimal("105.00"), calculate.getElternbeitrag());
		Assert.assertEquals(new BigDecimal("1562.40"), calculate.getVerguenstigung());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testZeitraumUeberMonatsende() {
		Verfuegung verfuegung = prepareVerfuegungTagiUndTageseltern(
			LocalDate.of(2016, Month.JANUARY, 10), LocalDate.of(2016, Month.FEBRUARY, 5),
			100, new BigDecimal("27750"));

		tagiRechner.calculate(verfuegung.getZeitabschnitte().get(0), verfuegung, parameterDTO);
	}
}
