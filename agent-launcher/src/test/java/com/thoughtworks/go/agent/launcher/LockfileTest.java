/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.agent.launcher;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
public class LockfileTest {

    private static final File LOCK_FILE = new File("LockFile.txt");

    @SystemStub
    private SystemProperties systemProperties;

    @BeforeEach
    public void setUp() {
        systemProperties.set(Lockfile.SLEEP_TIME_FOR_LAST_MODIFIED_CHECK_PROPERTY, "0");
    }

    @AfterEach
    public void tearDown() throws IOException {
        Files.deleteIfExists(LOCK_FILE.toPath());
    }

    @Test
    public void shouldNotExistIfNotChangedRecently() {
        File mockfile = mock(File.class);
        Lockfile lockfile = new Lockfile(mockfile);
        Lockfile spy = spy(lockfile);
        doReturn(false).when(spy).lockFileChangedWithinMinutes(Duration.ofMinutes(10));
        when(lockfile.exists()).thenReturn(true);
        assertThat(spy.exists()).isFalse();
    }

    @Test
    public void shouldExistIfFileExistsAndChangedRecently() {
        File mockfile = mock(File.class);
        Lockfile lockfile = new Lockfile(mockfile);
        Lockfile spy = spy(lockfile);
        doReturn(true).when(spy).lockFileChangedWithinMinutes(Duration.ofMinutes(10));
        when(lockfile.exists()).thenReturn(true);
        assertThat(spy.exists()).isTrue();
    }


    @Test
    public void shouldNotAttemptToDeleteLockFileIfItDoesNotExist() {
        File mockfile = mock(File.class);
        Lockfile lockfile = new Lockfile(mockfile);
        when(mockfile.exists()).thenReturn(false);
        lockfile.delete();
        verify(mockfile, never()).delete();
    }

    @Test
    public void shouldSpawnTouchLoopOnSet() throws IOException {
        Lockfile lockfile = mock(Lockfile.class);
        doCallRealMethod().when(lockfile).setHooks();
        doNothing().when(lockfile).touch();
        doNothing().when(lockfile).spawnTouchLoop();
        lockfile.setHooks();
        verify(lockfile).spawnTouchLoop();
    }

    @Test
    public void shouldReturnFalseIfLockFileAlreadyExists() {
        File mockfile = mock(File.class);
        Lockfile lockfile = new Lockfile(mockfile);
        when(mockfile.exists()).thenReturn(true);
        when(mockfile.lastModified()).thenReturn(System.currentTimeMillis());
        assertThat(lockfile.tryLock()).isFalse();
        verify(mockfile).exists();
    }

    @Test
    public void shouldReturnFalseIfUnableToSetLock() throws IOException {
        File mockfile = mock(File.class);
        Lockfile lockfile = spy(new Lockfile(mockfile));
        when(mockfile.exists()).thenReturn(false);
        when(mockfile.getAbsolutePath()).thenReturn("/abcd/dummyFile");
        doThrow(new IOException("dummy")).when(lockfile).setHooks();
        assertThat(lockfile.tryLock()).isFalse();
        verify(mockfile).exists();
    }

    @Test
    public void shouldReturnTrueIfCanSetLockAndDeleteLockFileWhenDeleteIsCalled() {
        Lockfile lockfile = new Lockfile(LOCK_FILE);
        assertThat(lockfile.tryLock()).isTrue();
        lockfile.delete();
        assertThat(LOCK_FILE.exists()).isFalse();
    }

    @Test
    public void shouldNotDeleteLockFileIfTryLockDidntWork() throws IOException {
        FileUtils.touch(LOCK_FILE);
        Lockfile lockfile = new Lockfile(LOCK_FILE);
        assertThat(lockfile.tryLock()).isFalse();
        lockfile.delete();
        assertThat(LOCK_FILE.exists()).isTrue();
    }


}
