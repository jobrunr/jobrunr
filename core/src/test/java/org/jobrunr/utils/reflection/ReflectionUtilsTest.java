package org.jobrunr.utils.reflection;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.reflection.ReflectionTestClasses.GenericJobRequest;
import org.jobrunr.utils.reflection.ReflectionTestClasses.GenericJobRequestHandler;
import org.jobrunr.utils.reflection.ReflectionTestClasses.Level0JobRequest;
import org.jobrunr.utils.reflection.ReflectionTestClasses.Level0JobRequestHandler;
import org.jobrunr.utils.reflection.ReflectionTestClasses.Level1JobRequest;
import org.jobrunr.utils.reflection.ReflectionTestClasses.Level1JobRequestHandler;
import org.jobrunr.utils.reflection.ReflectionTestClasses.Level2JobRequest;
import org.jobrunr.utils.reflection.ReflectionTestClasses.Level2JobRequestHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.sql.Clob;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.utils.reflection.ReflectionUtils.getValueFromFieldOrProperty;
import static org.jobrunr.utils.reflection.ReflectionUtils.objectContainsFieldOrProperty;

@ExtendWith(MockitoExtension.class)
class ReflectionUtilsTest {

    @Test
    void testToClass() {
        assertThat(ReflectionUtils.toClass("boolean")).isEqualTo(boolean.class);
        assertThat(ReflectionUtils.toClass("byte")).isEqualTo(byte.class);
        assertThat(ReflectionUtils.toClass("short")).isEqualTo(short.class);
        assertThat(ReflectionUtils.toClass("int")).isEqualTo(int.class);
        assertThat(ReflectionUtils.toClass("long")).isEqualTo(long.class);
        assertThat(ReflectionUtils.toClass("float")).isEqualTo(float.class);
        assertThat(ReflectionUtils.toClass("double")).isEqualTo(double.class);
        assertThat(ReflectionUtils.toClass("char")).isEqualTo(char.class);
        assertThat(ReflectionUtils.toClass("void")).isEqualTo(void.class);

        assertThat(ReflectionUtils.toClass("java.lang.String")).isEqualTo(String.class);
        assertThatThrownBy(() -> ReflectionUtils.toClass("class.that.does.not.exist")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testObjectContainsFieldOrProperty() {
        final TestObject test = new TestObject("test");

        assertThat(objectContainsFieldOrProperty(test, "field")).isTrue();
        assertThat(objectContainsFieldOrProperty(test, "anotherField")).isTrue();
        assertThat(objectContainsFieldOrProperty(test, "doesNotExist")).isFalse();
    }

    @Test
    void testGetValueFromFieldOrProperty() {
        final TestObject test = new TestObject("test");

        assertThat(getValueFromFieldOrProperty(test, "field")).isEqualTo("test");
        assertThat(getValueFromFieldOrProperty(test, "anotherField")).isEqualTo("test");
        assertThatThrownBy(() -> getValueFromFieldOrProperty(test, "doesNotExist")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testAutobox() {
        assertThat(ReflectionUtils.autobox(null, String.class)).isEqualTo(null);
        assertThat(ReflectionUtils.autobox("string", String.class)).isEqualTo("string");
        assertThat(ReflectionUtils.autobox(1, int.class)).isEqualTo(1);
        assertThat(ReflectionUtils.autobox(1, Integer.class)).isEqualTo(1);
        assertThat(ReflectionUtils.autobox("1", int.class)).isEqualTo(1);
        assertThat(ReflectionUtils.autobox("1", Integer.class)).isEqualTo(1);
        assertThat(ReflectionUtils.autobox(1L, long.class)).isEqualTo(1L);
        assertThat(ReflectionUtils.autobox(1, Long.class)).isEqualTo(1L);
        assertThat(ReflectionUtils.autobox("1", long.class)).isEqualTo(1L);
        assertThat(ReflectionUtils.autobox("1", Long.class)).isEqualTo(1L);
        assertThat(ReflectionUtils.autobox("1.1", Double.class)).isEqualTo(1.1);
        assertThat(ReflectionUtils.autobox("1.1", double.class)).isEqualTo(1.1);
        assertThat(ReflectionUtils.autobox("true", Boolean.class)).isEqualTo(true);
        assertThat(ReflectionUtils.autobox("true", boolean.class)).isEqualTo(true);
        assertThat(ReflectionUtils.autobox("6ec8044c-ad95-4416-a29e-e946c72a37b0", UUID.class)).isEqualTo(UUID.fromString("6ec8044c-ad95-4416-a29e-e946c72a37b0"));
        assertThat(ReflectionUtils.autobox("A", TestEnum.class)).isEqualTo(TestEnum.A);
        assertThat(ReflectionUtils.autobox("PT8H6M12.345S", Duration.class)).isEqualTo(Duration.parse("PT8H6M12.345S"));
    }

    @Test
    void testAutoboxClob() throws SQLException {
        Clob clob = Mockito.mock(Clob.class);
        String result = "the result";

        Mockito.when(clob.length()).thenReturn((long) result.length());
        Mockito.when(clob.getSubString(1, result.length())).thenReturn(result);

        assertThat(ReflectionUtils.autobox(clob, String.class)).isEqualTo(result);
    }

    @Test
    void testFindMethodOnInterface() {
        final Optional<Method> doWork = ReflectionUtils.findMethod(TestInterface.class, "doWork");
        assertThat(doWork).isPresent();
    }

    @Test
    void testFindMethodOnSuperInterface() {
        final Optional<Method> doWorkFromParentInterfaceA = ReflectionUtils.findMethod(TestInterface.class, "doWorkFromParentInterfaceA");
        assertThat(doWorkFromParentInterfaceA).isPresent();

        final Optional<Method> doWorkFromParentInterfaceB = ReflectionUtils.findMethod(TestInterface.class, "doWorkFromParentInterfaceB");
        assertThat(doWorkFromParentInterfaceB).isPresent();
    }

    @Test
    void testFindMethodOnClassWithPrimitivesAndWrapper() {
        final Optional<Method> doWorkWithPrimitive = ReflectionUtils.findMethod(TestService.class, "doWork", Integer.class, Integer.class);
        assertThat(doWorkWithPrimitive).isPresent();

        final Optional<Method> doWorkWithWrapper = ReflectionUtils.findMethod(TestService.class, "doWork", Integer.class);
        assertThat(doWorkWithWrapper).isPresent();
    }

    @Test
    void testFindMethodOnClassWithInheritance() {
        final Optional<Method> doWorkWithSameType = ReflectionUtils.findMethod(TestService.class, "doWorkAndReturnResult", CharSequence.class);
        assertThat(doWorkWithSameType).isPresent();

        // JobRunr uses the JobParameter class which has the className (CharSequence) and actualClassName (String). To find the job method, CharSequence is used
        final Optional<Method> doWorkWithInheritedType = ReflectionUtils.findMethod(TestService.class, "doWorkAndReturnResult", String.class);
        assertThat(doWorkWithInheritedType).isPresent();

        final Optional<Method> doWorkWithWrongType = ReflectionUtils.findMethod(TestService.class, "doWorkAndReturnResult", UUID.class);
        assertThat(doWorkWithWrongType).isNotPresent();
    }

    @Test
    void testFindMethodOnClassWithMultipleInheritance() {
        final Optional<Method> level2JobRequestHandlerUsingLevel2JobRequest = ReflectionUtils.findMethod(Level2JobRequestHandler.class, "run", Level2JobRequest.class);
        assertThat(level2JobRequestHandlerUsingLevel2JobRequest).isPresent();

        final Optional<Method> level2JobRequestHandlerUsingLevel1JobRequest = ReflectionUtils.findMethod(Level2JobRequestHandler.class, "run", Level1JobRequest.class);
        assertThat(level2JobRequestHandlerUsingLevel1JobRequest).isPresent();

        final Optional<Method> level2JobRequestHandlerUsingLevel0JobRequest = ReflectionUtils.findMethod(Level2JobRequestHandler.class, "run", Level0JobRequest.class);
        assertThat(level2JobRequestHandlerUsingLevel0JobRequest).isPresent();

        final Optional<Method> level1JobRequestHandlerUsingLevel1JobRequest = ReflectionUtils.findMethod(Level1JobRequestHandler.class, "run", Level1JobRequest.class);
        assertThat(level1JobRequestHandlerUsingLevel1JobRequest).isPresent();

        final Optional<Method> level1JobRequestHandlerUsingLevel0JobRequest = ReflectionUtils.findMethod(Level1JobRequestHandler.class, "run", Level0JobRequest.class);
        assertThat(level1JobRequestHandlerUsingLevel0JobRequest).isPresent();

        final Optional<Method> level1JobRequestHandlerUsingJobRequest = ReflectionUtils.findMethod(Level1JobRequestHandler.class, "run", JobRequest.class);
        assertThat(level1JobRequestHandlerUsingJobRequest).isPresent();

        final Optional<Method> level0JobRequestHandlerUsingLevel2JobRequest = ReflectionUtils.findMethod(Level0JobRequestHandler.class, "run", Level2JobRequest.class);
        assertThat(level0JobRequestHandlerUsingLevel2JobRequest).isPresent();

        final Optional<Method> level0JobRequestHandlerUsingLevel1JobRequest = ReflectionUtils.findMethod(Level0JobRequestHandler.class, "run", Level1JobRequest.class);
        assertThat(level0JobRequestHandlerUsingLevel1JobRequest).isPresent();

        final Optional<Method> level0JobRequestHandlerUsingLevel0JobRequest = ReflectionUtils.findMethod(Level0JobRequestHandler.class, "run", Level0JobRequest.class);
        assertThat(level0JobRequestHandlerUsingLevel0JobRequest).isPresent();
    }

    @Test
    void testFindMethodOnGenericClass() {
        final Optional<Method> level1JobRequestHandlerUsingLevel1JobRequest = ReflectionUtils.findMethod(GenericJobRequestHandler.class, "run", GenericJobRequest.class);
        assertThat(level1JobRequestHandlerUsingLevel1JobRequest).isPresent();

        final Optional<Method> level1JobRequestHandlerUsingLevel0JobRequest = ReflectionUtils.findMethod(GenericJobRequestHandler.class, "run", JobRequest.class);
        assertThat(level1JobRequestHandlerUsingLevel0JobRequest).isPresent();
    }

    @Test
    void testFindMethodOnClassWithPrimitivesAndObjects() {
        final Optional<Method> doWorkWithMatchingTypes1 = ReflectionUtils.findMethod(TestService.class, "doWork", UUID.class, int.class, Instant.class);
        assertThat(doWorkWithMatchingTypes1).isPresent();

        final Optional<Method> doWorkWithMatchingTypes2 = ReflectionUtils.findMethod(TestService.class, "doWork", UUID.class, Integer.class, Instant.class);
        assertThat(doWorkWithMatchingTypes2).isPresent();
    }

    public static class TestObject {

        private final String field;

        public TestObject(String field) {
            this.field = field;
        }

        public String getAnotherField() {
            return field;
        }
    }

    public enum TestEnum {
        A,
        B
    }

    public interface TestInterface extends TestInterfaceParentA, TestInterfaceParentB {

        void doWork();

    }

    public interface TestInterfaceParentA {

        void doWorkFromParentInterfaceA();

    }

    public interface TestInterfaceParentB {

        void doWorkFromParentInterfaceB();

    }
}