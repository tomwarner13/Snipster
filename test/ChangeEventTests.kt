import com.okta.demo.ktor.schema.ChangeType
import com.okta.demo.ktor.helper.SnipChangeEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChangeEventTests {
    @Test
    internal fun validPropertyNameParses() {
        val event = SnipChangeEvent.fromPropertyName("1:Created:test@test:12345")
        assertEquals(event.id, 1)
        assertEquals(event.type, ChangeType.Created)
        assertEquals(event.username, "test@test")
        assertEquals(event.sessionId, "12345")
    }

    @Test
    internal fun invalidPropertyNameThrows() {
        assertThrows<IllegalArgumentException> {
            SnipChangeEvent.fromPropertyName("Created:test@test:12345")
        }
        assertThrows<NumberFormatException> {
            SnipChangeEvent.fromPropertyName("NotANum:Created:test@test:12345")
        }
        assertThrows<java.lang.IllegalArgumentException> {
            SnipChangeEvent.fromPropertyName("1:Bothered:test@test:12345")
        }
    }

    @Test
    internal fun propertyNameGenerates() {
        val event = SnipChangeEvent(1, ChangeType.Edited, "tom", "aaaaa")
        assertEquals(event.toPropertyName(), "1:Edited:tom:aaaaa")
    }
}