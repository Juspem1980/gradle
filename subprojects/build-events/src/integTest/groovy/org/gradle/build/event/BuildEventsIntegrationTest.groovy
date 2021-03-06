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

package org.gradle.build.event

import org.gradle.api.services.BuildServiceParameters
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.InstantExecutionRunner
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import org.junit.runner.RunWith

@RunWith(InstantExecutionRunner)
class BuildEventsIntegrationTest extends AbstractIntegrationSpec {
    def "listener can subscribe to task completion events"() {
        loggingListener()
        registeringPlugin()
        buildFile << """
            apply plugin: LoggingPlugin

            task a {}
            task b {}
            task c { dependsOn a, b }
        """

        when:
        run("a")

        then:
        output.count("EVENT:") == 1
        outputContains("EVENT: finish :a")

        when:
        run("a")

        then:
        output.count("EVENT:") == 1
        outputContains("EVENT: finish :a")

        when:
        run("c")

        then:
        output.count("EVENT:") == 3
        outputContains("EVENT: finish :a")
        outputContains("EVENT: finish :b")
        outputContains("EVENT: finish :c")

        when:
        run("c")

        then:
        output.count("EVENT:") == 3
        outputContains("EVENT: finish :a")
        outputContains("EVENT: finish :b")
        outputContains("EVENT: finish :c")
    }

    def "plugin applied to multiple projects can register a shared listener"() {
        settingsFile << """
            include 'a', 'b'
        """
        loggingListener()
        registeringPlugin()
        buildFile << """
            subprojects {
                apply plugin: LoggingPlugin
                task thing { }
            }
        """

        when:
        run("a:thing")

        then:
        output.count("EVENT:") == 1
        outputContains("EVENT: finish :a:thing")

        when:
        run("a:thing")

        then:
        output.count("EVENT:") == 1
        outputContains("EVENT: finish :a:thing")

        when:
        run("thing")

        then:
        output.count("EVENT:") == 2
        outputContains("EVENT: finish :a:thing")
        outputContains("EVENT: finish :b:thing")

        when:
        run("thing")

        then:
        output.count("EVENT:") == 2
        outputContains("EVENT: finish :a:thing")
        outputContains("EVENT: finish :b:thing")
    }

    def loggingListener() {
        buildFile << """
            import ${OperationCompletionListener.name}
            import ${FinishEvent.name}
            import ${TaskFinishEvent.name}
            import ${BuildServiceParameters.name}

            abstract class LoggingListener implements OperationCompletionListener, BuildService<BuildServiceParameters.None> {
                @Override
                void onFinish(FinishEvent event) {
                    if (event instanceof TaskFinishEvent) {
                        println("EVENT: finish \${event.descriptor.taskPath}")
                    } else {
                        throw IllegalArgumentException()
                    }
                }
            }
        """
    }

    def registeringPlugin() {
        buildFile << """
            import ${BuildEventsListenerRegistry.name}
            import javax.inject.Inject

            abstract class LoggingPlugin implements Plugin<Project> {
                @Inject
                abstract BuildEventsListenerRegistry getListenerRegistry()

                void apply(Project project) {
                    def listener = project.gradle.sharedServices.registerIfAbsent("listener", LoggingListener) { }
                    listenerRegistry.subscribe(listener)
                }
            }
        """
    }
}
