/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.internal.build.event

import org.gradle.api.internal.provider.Providers
import org.gradle.initialization.BuildEventConsumer
import org.gradle.internal.build.event.types.AbstractTaskResult
import org.gradle.internal.build.event.types.DefaultTaskDescriptor
import org.gradle.internal.build.event.types.DefaultTaskFinishedProgressEvent
import org.gradle.internal.event.ListenerManager
import org.gradle.internal.operations.BuildOperationListenerManager
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import spock.lang.Specification

class DefaultBuildEventsListenerRegistryTest extends Specification {
    def factory = new MockBuildEventListenerFactory()
    def registry = new DefaultBuildEventsListenerRegistry([factory], Stub(ListenerManager), Stub(BuildOperationListenerManager))

    def "listener receives task finish events"() {
        def listener = Mock(OperationCompletionListener)
        def provider = Providers.of(listener)
        def event = taskFinishEvent()

        when:
        registry.subscribe(provider)

        then:
        registry.subscriptions.size() == 1
        0 * listener._

        when:
        factory.fire(event)

        then:
        1 * listener.onFinish({ it instanceof TaskFinishEvent })
        0 * listener._
    }

    def "does nothing when listener is already subscribed"() {
        def listener = Mock(OperationCompletionListener)
        def provider = Providers.of(listener)

        when:
        registry.subscribe(provider)
        registry.subscribe(provider)

        then:
        registry.subscriptions.size() == 1
    }

    private DefaultTaskFinishedProgressEvent taskFinishEvent() {
        new DefaultTaskFinishedProgressEvent(123L, Stub(DefaultTaskDescriptor), Stub(AbstractTaskResult))
    }

    class MockBuildEventListenerFactory implements BuildEventListenerFactory {
        private BuildEventConsumer consumer

        def fire(Object event) {
            consumer.dispatch(event)
        }

        @Override
        Iterable<Object> createListeners(BuildEventSubscriptions subscriptions, BuildEventConsumer consumer) {
            this.consumer = consumer
            return []
        }
    }
}
