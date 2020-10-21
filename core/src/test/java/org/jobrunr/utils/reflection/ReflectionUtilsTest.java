package org.jobrunr.utils.reflection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Clob;
import java.sql.SQLException;
import java.time.Duration;
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
        assertThat(ReflectionUtils.autobox("string", String.class)).isEqualTo("string");
        assertThat(ReflectionUtils.autobox(1, int.class)).isEqualTo(1);
        assertThat(ReflectionUtils.autobox(1, Integer.class)).isEqualTo(1);
        assertThat(ReflectionUtils.autobox("1", int.class)).isEqualTo(1);
        assertThat(ReflectionUtils.autobox("1", Integer.class)).isEqualTo(1);
        assertThat(ReflectionUtils.autobox(1L, long.class)).isEqualTo(1L);
        assertThat(ReflectionUtils.autobox(1, Long.class)).isEqualTo(1L);
        assertThat(ReflectionUtils.autobox("1", long.class)).isEqualTo(1L);
        assertThat(ReflectionUtils.autobox("1", Long.class)).isEqualTo(1L);
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
}