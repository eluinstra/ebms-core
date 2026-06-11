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
package nl.clockwork.ebms.server.embedded.web;

import io.vavr.Function2;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.common.EbMSMessageStatus;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	@Getter
	private enum Status
	{
		SUCCESS(EnumSet.of(EbMSMessageStatus.PROCESSED, EbMSMessageStatus.FORWARDED, EbMSMessageStatus.DELIVERED), "success", "text-success"),
		WARNING(EnumSet.of(EbMSMessageStatus.RECEIVED, EbMSMessageStatus.CREATED), "warning", "text-warning"),
		DANGER(
				EnumSet.of(
						EbMSMessageStatus.UNAUTHORIZED,
						EbMSMessageStatus.NOT_RECOGNIZED,
						EbMSMessageStatus.FAILED,
						EbMSMessageStatus.DELIVERY_FAILED,
						EbMSMessageStatus.EXPIRED),
				"danger",
				"text-danger");

		EnumSet<EbMSMessageStatus> statuses;
		String rowClass;
		String cellClass;

		private static final Function2<EbMSMessageStatus, Function<Status, String>, String> GET_CSS_CLASS =
				(status, getClass) -> Arrays.stream(Status.values()).filter(s -> s.statuses.contains(status)).map(getClass::apply).findFirst().orElse(null);

		public static String getCssClass(EbMSMessageStatus status, Function<Status, String> getClass)
		{
			return GET_CSS_CLASS.apply(status, getClass);
		}
	}

	public static String getContentType(String pathInfo)
	{
		val result = URLConnection.guessContentTypeFromName(pathInfo);
		// val result = new MimetypesFileTypeMap().getContentType(pathInfo);
		// val result = URLConnection.getFileNameMap().getContentTypeFor(pathInfo);
		return result == null ? "application/octet-stream" : result;
	}

	public static String getFileExtension(String contentType)
	{
		if (StringUtils.isEmpty(contentType))
			return "";
		if (contentType.contains("text"))
			return ".txt";
		val parts = contentType.split("/");
		return parts.length > 1 ? "." + parts[1] : "";
	}

	public static String getTableCellCssClass(EbMSMessageStatus ebMSMessageStatus)
	{
		return Status.getCssClass(ebMSMessageStatus, Status::getCellClass);
	}

	public static String getTableRowCssClass(EbMSMessageStatus ebMSMessageStatus)
	{
		return Status.getCssClass(ebMSMessageStatus, Status::getRowClass);
	}

	public static String getErrorList(String content)
	{
		return content.replaceFirst("(?ms)^.*(<[^<>]*:?ErrorList.*ErrorList>).*$", "$1");
	}
}
