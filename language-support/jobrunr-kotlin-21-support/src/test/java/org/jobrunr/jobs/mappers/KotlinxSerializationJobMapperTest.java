package org.jobrunr.jobs.mappers;

import org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper;
import org.jobrunr.utils.mapper.JsonMapper;

public class KotlinxSerializationJobMapperTest extends JobMapperTest {
	@Override
	protected JsonMapper getJsonMapper() {
		return new KotlinxSerializationJsonMapper(TestKotlinxSerializationModuleKt.getTestModule());
	}
}
