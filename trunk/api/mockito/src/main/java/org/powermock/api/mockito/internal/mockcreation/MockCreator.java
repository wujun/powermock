/*
 * Copyright 2009 the original author or authors.
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
package org.powermock.api.mockito.internal.mockcreation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.internal.MockHandler;
import org.mockito.internal.configuration.GlobalConfiguration;
import org.mockito.internal.creation.MethodInterceptorFilter;
import org.mockito.internal.creation.MockSettingsImpl;
import org.mockito.internal.creation.jmock.ClassImposterizer;
import org.mockito.internal.progress.ThreadSafeMockingProgress;
import org.mockito.internal.util.reflection.LenientCopyTool;
import org.powermock.api.mockito.internal.invocationcontrol.MockitoMethodInvocationControl;
import org.powermock.api.support.ClassLoaderUtil;
import org.powermock.core.ClassReplicaCreator;
import org.powermock.core.DefaultFieldValueGenerator;
import org.powermock.core.MockRepository;
import org.powermock.reflect.Whitebox;

public class MockCreator {
	
	@SuppressWarnings("unchecked")
	public static <T> T mock(Class<T> type, boolean isStatic, boolean isSpy, Object delegator,
			MockSettings mockSettings, Method... methods) {
		if (type == null) {
			throw new IllegalArgumentException("The class to mock cannot be null");
		}

		T mock = null;
		final String mockName = toInstanceName(type);

        MockRepository.addAfterMethodRunner(new MockitoStateCleaner());

		final Class<T> typeToMock;
		if (isFinalJavaSystemClass(type)) {
			typeToMock = (Class<T>) new ClassReplicaCreator().createClassReplica(type);
		} else {
			typeToMock = type;
		}

		final MockData<T> mockData = createMethodInvocationControl(mockName, typeToMock, methods, isSpy, (T) delegator,
				mockSettings);

		mock = mockData.getMock();
		if (isFinalJavaSystemClass(type) && !isStatic) {
			mock = Whitebox.newInstance(type);
			DefaultFieldValueGenerator.fillWithDefaultValues(mock);
		}

		if (isStatic) {
			MockRepository.putStaticMethodInvocationControl(type, mockData.getMethodInvocationControl());
		} else {
			MockRepository.putInstanceMethodInvocationControl(mock, mockData.getMethodInvocationControl());
		}

		if (isSpy) {
			new LenientCopyTool().copyToMock(delegator, mock);
		}

		return mock;
	}

	private static <T> boolean isFinalJavaSystemClass(Class<T> type) {
		return type.getName().startsWith("java.") && Modifier.isFinal(type.getModifiers());
	}

	private static <T> MockData<T> createMethodInvocationControl(final String mockName, Class<T> type,
			Method[] methods, boolean isSpy, Object delegator, MockSettings mockSettings) {
		final MockSettingsImpl settings;
		if (mockSettings == null) {
			settings = (MockSettingsImpl) Mockito.withSettings();
		} else {
			settings = (MockSettingsImpl) mockSettings;
		}

		if (isSpy) {
			settings.defaultAnswer(Mockito.CALLS_REAL_METHODS);
		}

		settings.initiateMockName(type);
		MockHandler<T> mockHandler = new MockHandler<T>(settings);
		MethodInterceptorFilter filter = new MethodInterceptorFilter(mockHandler, settings);
		final T mock = (T) ClassImposterizer.INSTANCE.imposterise(filter, type);
		final MockitoMethodInvocationControl invocationControl = new MockitoMethodInvocationControl(filter,
                isSpy && delegator == null ? new Object() : delegator, mock, methods);

		return new MockData<T>(invocationControl, mock);
	}

	private static String toInstanceName(Class<?> clazz) {
		String className = clazz.getSimpleName();
		if (className.length() == 0) {
			return clazz.getName();
		}
		// lower case first letter
		return className.substring(0, 1).toLowerCase() + className.substring(1);
	}

	/**
	 * Class that encapsulate a mock and its corresponding invocation control.
	 */
	private static class MockData<T> {
		private final MockitoMethodInvocationControl methodInvocationControl;

		private final T mock;

		MockData(MockitoMethodInvocationControl methodInvocationControl, T mock) {
			this.methodInvocationControl = methodInvocationControl;
			this.mock = mock;
		}

		public MockitoMethodInvocationControl getMethodInvocationControl() {
			return methodInvocationControl;
		}

		public T getMock() {
			return mock;
		}
	}

    /**
     * Clear state in Mockito that retains memory between tests
     */
    private static class MockitoStateCleaner implements Runnable {
        public void run() {
            clearMockProgress();
            clearConfiguration();
        }

        private void clearMockProgress() {
            clearThreadLocalIn(ThreadSafeMockingProgress.class);
        }

        private void clearConfiguration() {
            clearThreadLocalIn(GlobalConfiguration.class);
        }

        private void clearThreadLocalIn(Class<?> cls) {
            Whitebox.getInternalState(cls, ThreadLocal.class).set(null);
            final Class<?> clazz = ClassLoaderUtil.loadClass(cls, ClassLoader.getSystemClassLoader());
            Whitebox.getInternalState(clazz, ThreadLocal.class).set(null);
        }
    }
}
