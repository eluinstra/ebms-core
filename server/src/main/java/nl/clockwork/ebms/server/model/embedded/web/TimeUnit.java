/*
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.server.message.model.embedded.web;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.server.embedded.startup.Constants;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public enum TimeUnit
{
	HOUR("Minutes", Duration.ofMinutes(1), Duration.ofHours(1), DateTimeFormatter.ofPattern(Constants.DATETIME_HOUR_FORMAT), DateTimeFormatter.ofPattern("mm")),
	DAY("Hours", Duration.ofHours(1), Duration.ofDays(1), DateTimeFormatter.ofPattern(Constants.DATE_FORMAT), DateTimeFormatter.ofPattern("HH")),
	/*
	 * WEEK("Days",Period.ofDays(1),Period.ofWeeks(1),DateTimeFormatter.ofPattern(Constants.DATE_FORMAT),DateTimeFormatter.ofPattern("dd")),
	 * MONTH("Weeks",Period.ofWeeks(1),Period.ofMonths(1),DateTimeFormatter.ofPattern(Constants.DATE_FORMAT),DateTimeFormatter.ofPattern("ww")),
	 */
	MONTH("Days", Period.ofDays(1), Period.ofMonths(1), DateTimeFormatter.ofPattern(Constants.DATE_MONTH_FORMAT), DateTimeFormatter.ofPattern("dd")),
	YEAR("Months", Period.ofMonths(1), Period.ofYears(1), DateTimeFormatter.ofPattern(Constants.DATE_YEAR_FORMAT), DateTimeFormatter.ofPattern("MM"));

	String units;
	TemporalAmount unitStep;
	TemporalAmount period;
	DateTimeFormatter dateFormatter;
	DateTimeFormatter timeUnitDateFormat;

	public TemporalAmount getTimeUnit()
	{
		return unitStep;
	}

	public LocalDateTime getFrom()
	{
		return getFrom(LocalDateTime.now());
	}

	public LocalDateTime getFrom(LocalDateTime dateTime)
	{
		switch (this)
		{
			case HOUR:
				return dateTime.truncatedTo(ChronoUnit.HOURS).plusHours(1).minus(period);
			case DAY:
				return dateTime.truncatedTo(ChronoUnit.DAYS).plusDays(1).minus(period);
			// case WEEK:
			// return dateTime.truncatedTo(ChronoUnit.DAYS).withDayOfWeek(1).plusWeeks(1).minus(period);
			case MONTH:
				return dateTime.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).plusMonths(1).minus(period);
			case YEAR:
				return dateTime.truncatedTo(ChronoUnit.DAYS).withDayOfYear(1).plusYears(1).minus(period);
			default:
				return null;
		}
	}
}