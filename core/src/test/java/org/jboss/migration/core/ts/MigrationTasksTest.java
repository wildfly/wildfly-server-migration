/*
 * Copyright 2016 Red Hat, Inc.
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
package org.jboss.migration.core.ts;

import org.jboss.migration.core.MigrationData;
import org.jboss.migration.core.ServerMigration;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskExecution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MigrationTasksTest {
    static MigrationData migrationData(boolean shouldFail) {
        MigrationEnvironment env = new MigrationEnvironment();
        env.setProperty("unused.property", "foobar");
        env.setProperty("test.property.key", "test.property.value");
        env.setProperty("test.should.fail", "" + shouldFail);

        return new ServerMigration()
                .from(TestSourceServerProvider.SERVER.getBaseDir())
                .to(TestTargetServerProvider.SERVER.getBaseDir())
                .userEnvironment(env)
                .run();
    }

    @Test
    public void success() {
        MigrationData migrationData = migrationData(false);

        assertEquals(ServerMigrationTaskResult.Status.SUCCESS, migrationData.getRootTask().getResult().getStatus());

        List<TaskExecution> subtasks = migrationData.getRootTask().getSubtasks();
        assertEquals(2, subtasks.size());
        checkCommonSubtasks(subtasks);
    }

    @Test
    public void fail() {
        MigrationData migrationData = migrationData(true);

        assertEquals(ServerMigrationTaskResult.Status.FAIL, migrationData.getRootTask().getResult().getStatus());

        List<TaskExecution> subtasks = migrationData.getRootTask().getSubtasks();
        assertEquals(3, subtasks.size());
        checkCommonSubtasks(subtasks);

        {
            TaskExecution subtask3 = subtasks.get(2);
            assertEquals("subtask 3", subtask3.getTaskName().getName());
            assertTrue(subtask3.getTaskName().getAttributes().containsKey("always"));
            assertEquals("fails", subtask3.getTaskName().getAttributes().get("always"));
            assertEquals(ServerMigrationTaskResult.Status.FAIL, subtask3.getResult().getStatus());
        }
    }

    private void checkCommonSubtasks(List<TaskExecution> subtasks) {
        assertTrue(subtasks.size() >= 2);

        {
            TaskExecution subtask1 = subtasks.get(0);
            assertEquals("subtask 1", subtask1.getTaskName().getName());
            assertEquals(ServerMigrationTaskResult.Status.SUCCESS, subtask1.getResult().getStatus());
            assertEquals(2, subtask1.getSubtasks().size());
        }

        {
            TaskExecution subtask11 = subtasks.get(0).getSubtasks().get(0);
            assertEquals("subtask 1.1", subtask11.getTaskName().getName());
            assertTrue(subtask11.getTaskName().getAttributes().containsKey("config"));
            assertEquals("foobar", subtask11.getTaskName().getAttributes().get("config"));
            assertEquals(ServerMigrationTaskResult.Status.SUCCESS, subtask11.getResult().getStatus());
            assertEquals(0, subtask11.getSubtasks().size());
        }

        {
            TaskExecution subtask12 = subtasks.get(0).getSubtasks().get(1);
            assertEquals("subtask 1.2", subtask12.getTaskName().getName());
            assertTrue(subtask12.getTaskName().getAttributes().containsKey("config"));
            assertEquals("quux", subtask12.getTaskName().getAttributes().get("config"));
            assertEquals(ServerMigrationTaskResult.Status.SUCCESS, subtask12.getResult().getStatus());
            assertEquals(0, subtask12.getSubtasks().size());
        }

        {
            TaskExecution subtask2 = subtasks.get(1);
            assertEquals("subtask 2", subtask2.getTaskName().getName());
            assertTrue(subtask2.getTaskName().getAttributes().containsKey("source"));
            assertEquals("abcd xyzw", subtask2.getTaskName().getAttributes().get("source"));
            assertEquals(ServerMigrationTaskResult.Status.SKIPPED, subtask2.getResult().getStatus());
            assertEquals(0, subtask2.getSubtasks().size());
        }
    }
}
