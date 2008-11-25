package demo.org.powermock.examples.simple;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.expect;
import static org.powermock.PowerMock.createMock;
import static org.powermock.PowerMock.expectLastCall;
import static org.powermock.PowerMock.expectNew;
import static org.powermock.PowerMock.mockStatic;
import static org.powermock.PowerMock.replay;
import static org.powermock.PowerMock.verify;
import static org.powermock.Whitebox.invokeMethod;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("demo.org.powermock.examples.simple.SimpleConfig")
@PrepareForTest( { Greeter.class, Logger.class })
public class GreeterTest {

	@Test
	public void testGetMessage() throws Exception {
		mockStatic(SimpleConfig.class);
		expect(SimpleConfig.getGreeting()).andReturn("Hi");
		expect(SimpleConfig.getTarget()).andReturn("All");
		replay(SimpleConfig.class);

		assertEquals("Hi All", invokeMethod(Greeter.class, "getMessage"));

		verify(SimpleConfig.class);
	}

	@Test
	public void testRun() throws Exception {
		Logger logger = createMock(Logger.class);
		
		expectNew(Logger.class).andReturn(logger);
		logger.log("Hello");
		expectLastCall().times(10);
		replay(logger, Logger.class);

		invokeMethod(new Greeter(), "run", 10, "Hello");

		verify(logger, Logger.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRunWhenLoggerThrowsUnexpectedRuntimeExeception() throws Exception {
		expectNew(Logger.class).andThrow(new IllegalArgumentException("Unexpected exeception"));
		replay(Logger.class);

		invokeMethod(new Greeter(), "run", 10, "Hello");

		verify(Logger.class);
	}

}