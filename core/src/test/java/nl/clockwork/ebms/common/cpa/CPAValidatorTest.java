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
package nl.clockwork.ebms.common.cpa;

import static nl.clockwork.ebms.api.cpa.CPATestUtils.loadCPA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@TestInstance(value = Lifecycle.PER_CLASS)
class CPAValidatorTest
{
	private static final String DEFAULT_CPA_ID = "cpaStubEBF.rm.https.signed";
	private static final String ENCRYPTED_CPA_ID = "cpaStubEBF.rm.https.signed.encrypted";
	private static final String SYNC_CPA_ID = "cpaStubEBF.rm.https.signed.sync";
	private static final String NOT_EXISTING_CPA_ID = "cpaStubEBF.rm.https.signed.not.existing";

	@Mock
	CPARepository cpaRepository;
	CPAValidator cpaValidator;

	@BeforeAll
	void init()
	{
		MockitoAnnotations.openMocks(this);
		when(cpaRepository.getCPA(DEFAULT_CPA_ID)).thenReturn(loadCPA(DEFAULT_CPA_ID));
		when(cpaRepository.getCPA(ENCRYPTED_CPA_ID)).thenReturn(loadCPA(ENCRYPTED_CPA_ID));
		when(cpaRepository.getCPA(SYNC_CPA_ID)).thenReturn(loadCPA(SYNC_CPA_ID));
		cpaValidator = new CPAValidator(cpaRepository);
	}

	@ParameterizedTest
	@ValueSource(strings = {"2011-01-01T00:00:00Z", "2020-01-01T00:00:00.00Z", "2021-01-01T00:00:00Z"})
	void isValid(String timestamp)
	{
		assertThat(cpaValidator.isValid(DEFAULT_CPA_ID, Instant.parse(timestamp))).isTrue();
	}

	@ParameterizedTest
	@MethodSource
	void isNotValid(String cpaId, String timestamp)
	{
		assertThat(cpaValidator.isValid(cpaId, Instant.parse(timestamp))).isFalse();
	}

	private static Stream<Arguments> isNotValid()
	{
		return Stream.of(
				arguments(DEFAULT_CPA_ID, "2010-12-31T23:59:59Z"),
				arguments(DEFAULT_CPA_ID, "2031-01-01T00:00:01Z"),
				arguments(NOT_EXISTING_CPA_ID, "2020-01-01T00:00:00.00Z"));
	}

}
