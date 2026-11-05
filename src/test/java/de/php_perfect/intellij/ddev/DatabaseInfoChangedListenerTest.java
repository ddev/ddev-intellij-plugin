package de.php_perfect.intellij.ddev;

import com.intellij.util.messages.Topic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class DatabaseInfoChangedListenerTest {
    @Test
    void topicCarriesDatabaseInfoListeners() {
        final Topic<DatabaseInfoChangedListener> topic = DatabaseInfoChangedListener.DATABASE_INFO_CHANGED_TOPIC;

        Assertions.assertEquals(DatabaseInfoChangedListener.class, topic.getListenerClass());
        Assertions.assertEquals("DDEV Database Info Changed", topic.getDisplayName());
    }
}
