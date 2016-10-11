/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.connection.jedis;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.redis.connection.AbstractConnectionUnitTestBase;
import org.springframework.data.redis.connection.RedisServerCommands.ShutdownOption;
import org.springframework.data.redis.connection.jedis.JedisConnectionUnitTestSuite.JedisConnectionPipelineUnitTests;
import org.springframework.data.redis.connection.jedis.JedisConnectionUnitTestSuite.JedisConnectionUnitTests;

import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;

/**
 * @author Christoph Strobl
 */
@RunWith(Suite.class)
@SuiteClasses({ JedisConnectionUnitTests.class, JedisConnectionPipelineUnitTests.class })
public class JedisConnectionUnitTestSuite {

	public static class JedisConnectionUnitTests extends AbstractConnectionUnitTestBase<Client> {

		protected JedisConnection connection;
		private Jedis jedisSpy;

		@Before
		public void setUp() {

			jedisSpy = spy(new MockedClientJedis("http://localhost:1234", getNativeRedisConnectionMock()));
			connection = new JedisConnection(jedisSpy);
		}

		@Test // DATAREDIS-184
		public void shutdownWithNullShouldDelegateCommandCorrectly() {

			connection.shutdown(null);

			verifyNativeConnectionInvocation().shutdown();
		}

		@Test // DATAREDIS-184
		public void shutdownNosaveShouldBeSentCorrectlyUsingLuaScript() {

			connection.shutdown(ShutdownOption.NOSAVE);

			ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
			verifyNativeConnectionInvocation().eval(captor.capture(), any(byte[].class), any(byte[][].class));

			assertThat(captor.getValue()).isEqualTo("return redis.call('SHUTDOWN','NOSAVE')".getBytes());
		}

		@Test // DATAREDIS-184
		public void shutdownSaveShouldBeSentCorrectlyUsingLuaScript() {

			connection.shutdown(ShutdownOption.SAVE);

			ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
			verifyNativeConnectionInvocation().eval(captor.capture(), any(byte[].class), any(byte[][].class));

			assertThat(captor.getValue()).isEqualTo("return redis.call('SHUTDOWN','SAVE')".getBytes());
		}

		@Test // DATAREDIS-267
		public void killClientShouldDelegateCallCorrectly() {

			connection.killClient("127.0.0.1", 1001);
			verifyNativeConnectionInvocation().clientKill(eq("127.0.0.1:1001"));
		}

		@Test // DATAREDIS-270
		public void getClientNameShouldSendRequestCorrectly() {

			connection.getClientName();
			verifyNativeConnectionInvocation().clientGetname();
		}

		@Test(expected = IllegalArgumentException.class) // DATAREDIS-277
		public void slaveOfShouldThrowExectpionWhenCalledForNullHost() {
			connection.slaveOf(null, 0);
		}

		@Test // DATAREDIS-277
		public void slaveOfShouldBeSentCorrectly() {

			connection.slaveOf("127.0.0.1", 1001);
			verifyNativeConnectionInvocation().slaveof(eq("127.0.0.1"), eq(1001));
		}

		@Test // DATAREDIS-277
		public void slaveOfNoOneShouldBeSentCorrectly() {

			connection.slaveOfNoOne();
			verifyNativeConnectionInvocation().slaveofNoOne();
		}

		@Test(expected = InvalidDataAccessResourceUsageException.class) // DATAREDIS-330
		public void shouldThrowExceptionWhenAccessingRedisSentinelsCommandsWhenNoSentinelsConfigured() {
			connection.getSentinelConnection();
		}

		@Test(expected = IllegalArgumentException.class) // DATAREDIS-472
		public void restoreShouldThrowExceptionWhenTtlInMillisExceedsIntegerRange() {
			connection.restore("foo".getBytes(), new Long(Integer.MAX_VALUE) + 1L, "bar".getBytes());
		}

		@Test(expected = IllegalArgumentException.class) // DATAREDIS-472
		public void setExShouldThrowExceptionWhenTimeExceedsIntegerRange() {
			connection.setEx("foo".getBytes(), new Long(Integer.MAX_VALUE) + 1L, "bar".getBytes());
		}

		@Test(expected = IllegalArgumentException.class) // DATAREDIS-472
		public void getRangeShouldThrowExceptionWhenStartExceedsIntegerRange() {
			connection.getRange("foo".getBytes(), new Long(Integer.MAX_VALUE) + 1L, Integer.MAX_VALUE);
		}

		@Test(expected = IllegalArgumentException.class) // DATAREDIS-472
		public void getRangeShouldThrowExceptionWhenEndExceedsIntegerRange() {
			connection.getRange("foo".getBytes(), Integer.MAX_VALUE, new Long(Integer.MAX_VALUE) + 1L);
		}

		@Test(expected = IllegalArgumentException.class) // DATAREDIS-472
		public void sRandMemberShouldThrowExceptionWhenCountExceedsIntegerRange() {
			connection.sRandMember("foo".getBytes(), new Long(Integer.MAX_VALUE) + 1L);
		}

		@Test(expected = IllegalArgumentException.class) // DATAREDIS-472
		public void zRangeByScoreShouldThrowExceptionWhenOffsetExceedsIntegerRange() {
			connection.zRangeByScore("foo".getBytes(), "foo", "bar", new Long(Integer.MAX_VALUE) + 1L, Integer.MAX_VALUE);
		}

		@Test(expected = IllegalArgumentException.class) // DATAREDIS-472
		public void zRangeByScoreShouldThrowExceptionWhenCountExceedsIntegerRange() {
			connection.zRangeByScore("foo".getBytes(), "foo", "bar", Integer.MAX_VALUE, new Long(Integer.MAX_VALUE) + 1L);
		}

	}

	public static class JedisConnectionPipelineUnitTests extends JedisConnectionUnitTests {

		@Before
		public void setUp() {
			super.setUp();
			connection.openPipeline();
		}

		@Override
		@Test(expected = UnsupportedOperationException.class) // DATAREDIS-184
		public void shutdownNosaveShouldBeSentCorrectlyUsingLuaScript() {
			super.shutdownNosaveShouldBeSentCorrectlyUsingLuaScript();
		}

		@Override
		@Test(expected = UnsupportedOperationException.class) // DATAREDIS-184
		public void shutdownSaveShouldBeSentCorrectlyUsingLuaScript() {
			super.shutdownSaveShouldBeSentCorrectlyUsingLuaScript();
		}

		@Test(expected = UnsupportedOperationException.class) // DATAREDIS-267
		public void killClientShouldDelegateCallCorrectly() {
			super.killClientShouldDelegateCallCorrectly();
		}

		@Override
		@Test(expected = UnsupportedOperationException.class) // DATAREDIS-270
		public void getClientNameShouldSendRequestCorrectly() {
			super.getClientNameShouldSendRequestCorrectly();
		}

		@Override
		@Test(expected = UnsupportedOperationException.class) // DATAREDIS-277
		public void slaveOfShouldBeSentCorrectly() {
			super.slaveOfShouldBeSentCorrectly();
		}

		@Test(expected = UnsupportedOperationException.class) // DATAREDIS-277
		public void slaveOfNoOneShouldBeSentCorrectly() {
			super.slaveOfNoOneShouldBeSentCorrectly();
		}

	}

	/**
	 * {@link Jedis} extension allowing to use mocked object as {@link Client}.
	 */
	private static class MockedClientJedis extends Jedis {

		public MockedClientJedis(String host, Client client) {
			super(host);
			this.client = client;
		}

	}
}
