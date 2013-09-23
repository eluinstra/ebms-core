/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.mule.ebms.stub;

public class Constants
{
	public static final String CPA_ID = "EBMS.CPA_ID";
	public static final String EBMS_MESSAGE_ID = "EBMS.EBMS_MESSAGE_ID";
	public static final String EBMS_MESSAGE_STATUS = "EBMS.EBMS_MESSAGE_STATUS";

	public enum AfleverberichtError
  {
		NO_ERROR(0), PROCESSING_ERROR(1), XSD_ERROR(2), BERICHTSOORT_ERROR(3);
		
		private final int id;
		
		AfleverberichtError(int id) { this.id = id; }
		
		public final int id() { return id; }
		
		public final static AfleverberichtError get(int id)
		{
			switch (id)
			{
				case 0:
					return AfleverberichtError.NO_ERROR;
				case 1:
					return AfleverberichtError.PROCESSING_ERROR;
				case 2:
					return AfleverberichtError.XSD_ERROR;
				case 3:
					return AfleverberichtError.BERICHTSOORT_ERROR;
				default:
					return null;
			}
		}
  };

  public enum AfleverServiceErrorCode
  {
  	AFS100("AFS100","Het verzoek voldoet niet aan de koppelvlakspecificaties en kan hierdoor niet door de Procesinfrastructuur worden verwerkt.\n\nDe volgende fout is opgetreden:\n\n<melding>"),
  	AFS400("AFS400","Er is een technische fout in de afleverservice van de Procesinfrastructuur opgetreden. Probeer het later opnieuw of neem contact op met de beheerder van de Procesinfrastructuur.\n\n<contactgegevens>"),
  	AFS600("AFS600","De verantwoordingsinformatie kan niet worden afgeleverd bij de betreffende uitvragende partij.\nDe volgende fout is geconstateerd:\n\n<melding>\n\nNeem contact op met de beheerder van de Procesinfrastructuur.\n\n<contactgegeven>");
		
		private final String foutCode;
		private final String foutBeschrijving;
		
		AfleverServiceErrorCode(String foutCode, String foutBeschrijving) { this.foutCode = foutCode; this.foutBeschrijving = foutBeschrijving; }
		
		public final String foutCode() { return foutCode; }

		public final String foutBeschrijving() { return foutBeschrijving; }
		
  };

	public static final String AFLEVERBERICHT_BERICHTSOORT_EFACTUUR = "e-factuur";

	public static final String AANLEVERBERICHT_BERICHTSOORT_EFACTUUR = "e-factuur";
	public static final String AANLEVERBERICHT_BERICHTSOORT_ORDER = "order";

}
