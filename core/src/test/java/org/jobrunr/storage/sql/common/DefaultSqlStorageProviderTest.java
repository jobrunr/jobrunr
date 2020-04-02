package org.jobrunr.storage.sql.common;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.StorageException;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultSqlStorageProviderTest {

    @Mock
    private DataSource datasource;
    @Mock
    private Connection connection;
    @Mock
    private Statement statement;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;

    private DefaultSqlStorageProvider jobStorageProvider;

    @BeforeEach
    public void setUp() throws SQLException {
        when(connection.createStatement()).thenReturn(statement);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        lenient().when(connection.prepareStatement(anyString(), eq(ResultSet.TYPE_FORWARD_ONLY), eq(ResultSet.CONCUR_READ_ONLY))).thenReturn(preparedStatement);
        when(datasource.getConnection()).thenReturn(connection);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        jobStorageProvider = new DefaultSqlStorageProvider(datasource);
        jobStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
    }

    @Test
    public void testGetJobById() throws SQLException {
        when(resultSet.next()).thenReturn(false);

        assertThatThrownBy(() -> jobStorageProvider.getJobById(randomUUID())).isInstanceOf(JobNotFoundException.class);
    }

    @Test
    public void testGetJobById_WhenSqlExceptionOccursAJobStorageExceptionIsThrown() throws SQLException {
        doThrow(new SQLException("Boem")).when(resultSet).next();

        assertThatThrownBy(() -> jobStorageProvider.getJobById(randomUUID())).isInstanceOf(StorageException.class);
    }

    @Test
    public void saveJob_WhenSqlExceptionOccursAJobStorageExceptionIsThrown() throws SQLException {
        doThrow(new SQLException("Boem")).when(preparedStatement).executeUpdate();

        assertThatThrownBy(() -> jobStorageProvider.save(anEnqueuedJob().build())).isInstanceOf(StorageException.class);
    }

    @Test
    public void saveJob() throws SQLException {
        when(preparedStatement.executeUpdate()).thenReturn(1);

        assertThatCode(() -> jobStorageProvider.save(anEnqueuedJob().build())).doesNotThrowAnyException();
    }

    @Test
    public void saveJob_WhenJobIsNotSavedDueToOtherConcurrentModificationThenThrowConcurrentJobModificationException() throws SQLException {
        when(preparedStatement.executeUpdate()).thenReturn(0);

        assertThatThrownBy(() -> jobStorageProvider.save(anEnqueuedJob().build())).isInstanceOf(ConcurrentJobModificationException.class);
    }

    @Test
    public void saveJobs() throws SQLException {
        when(preparedStatement.executeBatch()).thenReturn(new int[]{1});

        assertThatCode(() -> jobStorageProvider.save(asList(anEnqueuedJob().build()))).doesNotThrowAnyException();
    }

    @Test
    public void saveJobs_WhenSqlExceptionOccursAJobStorageExceptionIsThrown() throws SQLException {
        doThrow(new SQLException("Boem")).when(preparedStatement).executeBatch();

        assertThatThrownBy(() -> jobStorageProvider.save(asList(anEnqueuedJob().build()))).isInstanceOf(StorageException.class);
    }

    @Test
    public void saveJobs_NotAllJobsAreSavedThenThrowConcurrentJobModificationException() throws SQLException {
        when(preparedStatement.executeBatch()).thenReturn(new int[]{});

        assertThatThrownBy(() -> jobStorageProvider.save(asList(anEnqueuedJob().build()))).isInstanceOf(ConcurrentJobModificationException.class);
    }

    @Test
    public void delete_WhenSqlExceptionOccursAJobStorageExceptionIsThrown() throws SQLException {
        doThrow(new SQLException("Boem")).when(preparedStatement).executeUpdate();

        assertThatThrownBy(() -> jobStorageProvider.delete(randomUUID())).isInstanceOf(StorageException.class);
    }

}