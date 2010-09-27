// Modified or written by Luca Marrocco for inclusion with hoptoad.
// Copyright (c) 2009 Luca Marrocco.
// Licensed under the Apache License, Version 2.0 (the "License")

package code.lucamarrocco.hoptoad;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static code.lucamarrocco.hoptoad.Exceptions.ERROR_MESSAGE;
import static code.lucamarrocco.hoptoad.Exceptions.newException;
import static code.lucamarrocco.hoptoad.Slurp.read;
import static code.lucamarrocco.hoptoad.Slurp.slurp;
import static code.lucamarrocco.hoptoad.Slurp.strings;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class HoptoadNotifierTest {
	public static final String API_KEY = "a7bad952a319d10540fbbd64b597260d";

	protected static final Backtrace BACKTRACE = new Backtrace(asList("backtrace is empty"));;
	protected static final Map<String, Object> REQUEST = new HashMap<String, Object>();
	protected static final Map<String, Object> SESSION = new HashMap<String, Object>();
	protected static final Map<String, Object> ENVIRONMENT = new HashMap<String, Object>();

	private final Log logger = LogFactory.getLog(getClass());

	private final Map<String, Object> EC2 = new HashMap<String, Object>();

	private HoptoadNotify notifierV2;

	private <T> Matcher<T> internalServerError() {
		return new BaseMatcher<T>() {
			public void describeTo(final Description description) {
				description.appendText("internal server error");
			}

			public boolean matches(final Object item) {
				return item.equals(500);
			}
		};
	}

	private int notifing(final String string) {
		return new HoptoadNotifier().notify(new HoptoadNoticeBuilder(API_KEY, ERROR_MESSAGE) {
			{
				backtrace(new Backtrace(asList(string)));
			}
		}.newNotice(), false);
	}

	@Before
	public void setUp() {
		ENVIRONMENT.put("A_KEY", "test");
		EC2.put("AWS_SECRET", "AWS_SECRET");
		EC2.put("EC2_PRIVATE_KEY", "EC2_PRIVATE_KEY");
		EC2.put("AWS_ACCESS", "AWS_ACCESS");
		EC2.put("EC2_CERT", "EC2_CERT");
		notifierV2 = new HoptoadNotifierV2();
	}

	@Test
	public void testHowBacktraceHoptoadNotInternalServerError() {
		assertThat(notifing(ERROR_MESSAGE), not(internalServerError()));
		assertThat(notifing("java.lang.RuntimeException: an expression is not valid"), not(internalServerError()));
		assertThat(notifing("Caused by: java.lang.NullPointerException"), not(internalServerError()));
		assertThat(notifing("at code.lucamarrocco.notifier.Exceptions.newException(Exceptions.java:11)"), not(internalServerError()));
		assertThat(notifing("... 23 more"), not(internalServerError()));
	}

	@Test
	public void testLogErrorWithException() {
		logger.error("error", newException(ERROR_MESSAGE));
	}

	@Test
	public void testLogErrorWithoutException() {
		logger.error("error");
	}

	@Test
	public void testLogThresholdLesserThatErrorWithExceptionDoNotNotifyToHoptoad() {
		logger.info("info", newException(ERROR_MESSAGE));
		logger.warn("warn", newException(ERROR_MESSAGE));
	}

	@Test
	public void testLogThresholdLesserThatErrorWithoutExceptionDoNotNotifyToHoptoad() {
		logger.info("info");
		logger.warn("warn");
	}

	@Test
	public void testNotifyToHoptoadUsingBuilderNoticeFromExceptionInEnv() {
		final Exception EXCEPTION = newException(ERROR_MESSAGE);
		final HoptoadNotice notice = new HoptoadNoticeBuilder(API_KEY, EXCEPTION, "test").newNotice();

		assertThat(notifierV2.notify(notice, false), is(200));
	}

	@Test
	public void testNotifyToHoptoadUsingBuilderNoticeFromExceptionInEnvAndSystemProperties() {
		final Exception EXCEPTION = newException(ERROR_MESSAGE);
		final HoptoadNotice notice = new HoptoadNoticeBuilder(API_KEY, EXCEPTION, "test") {
			{
				filteredSystemProperties();
			}

		}.newNotice();

		assertThat(notifierV2.notify(notice, false), is(200));
	}

	@Test
	public void testNotifyToHoptoadUsingBuilderNoticeInEnv() {
		final HoptoadNotice notice = new HoptoadNoticeBuilder(API_KEY, ERROR_MESSAGE, "test").newNotice();

		assertThat(notifierV2.notify(notice, false), is(200));
	}

	@Test
	public void testSendExceptionNoticeWithFilteredBacktrace() {
		final Exception EXCEPTION = newException(ERROR_MESSAGE);
		final HoptoadNotice notice = new HoptoadNoticeBuilder(API_KEY, new QuietRubyBacktrace(), EXCEPTION, "test").newNotice();

		assertThat(notifierV2.notify(notice, false), is(200));
	}

	@Test
	public void testSendExceptionToHoptoad() {
		final Exception EXCEPTION = newException(ERROR_MESSAGE);
		final HoptoadNotice notice = new HoptoadNoticeBuilder(API_KEY, EXCEPTION).newNotice();

		assertThat(notifierV2.notify(notice, false), is(200));
	}

	@Test
	public void testSendExceptionToHoptoadUsingRubyBacktrace() {
		final Exception EXCEPTION = newException(ERROR_MESSAGE);
		final HoptoadNotice notice = new HoptoadNoticeBuilder(API_KEY, new RubyBacktrace(), EXCEPTION, "test").newNotice();

		assertThat(notifierV2.notify(notice, false), is(200));
	}

	@Test
	public void testSendExceptionToHoptoadUsingRubyBacktraceAndFilteredSystemProperties() {
		final Exception EXCEPTION = newException(ERROR_MESSAGE);
		final HoptoadNotice notice = new HoptoadNoticeBuilderUsingFilterdSystemProperties(API_KEY, new RubyBacktrace(), EXCEPTION, "test").newNotice();

		assertThat(notifierV2.notify(notice, false), is(200));
	}

	@Test
	public void testSendNoticeToHoptoad() {
		final HoptoadNotice notice = new HoptoadNoticeBuilder(API_KEY, ERROR_MESSAGE).newNotice();

		assertThat(notifierV2.notify(notice, false), is(200));
	}

	@Test
	public void testSendNoticeWithFilteredBacktrace() {
		final HoptoadNotice notice = new HoptoadNoticeBuilder(API_KEY, ERROR_MESSAGE) {
			{
				backtrace(new QuietRubyBacktrace(strings(slurp(read("backtrace.txt")))));
			}
		}.newNotice();

		assertThat(notifierV2.notify(notice, false), is(200));
	}

	@Test
	public void testSendNoticeWithLargeBacktrace() {
		final HoptoadNotice notice = new HoptoadNoticeBuilder(API_KEY, ERROR_MESSAGE) {
			{
				backtrace(new Backtrace(strings(slurp(read("backtrace.txt")))));
			}
		}.newNotice();

		assertThat(notifierV2.notify(notice, false), is(200));
	}
}
