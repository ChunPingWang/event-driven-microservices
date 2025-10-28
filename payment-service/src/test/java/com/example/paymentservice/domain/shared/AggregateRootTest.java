package com.example.paymentservice.domain.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 聚合根基類單元測試
 */
class AggregateRootTest {
    
    private TestAggregateRoot aggregateRoot;
    
    @BeforeEach
    void setUp() {
        aggregateRoot = new TestAggregateRoot();
    }
    
    @Test
    void shouldStartWithNoDomainEvents() {
        // When
        List<DomainEvent> events = aggregateRoot.getDomainEvents();
        
        // Then
        assertTrue(events.isEmpty());
        assertFalse(aggregateRoot.hasDomainEvents());
    }
    
    @Test
    void shouldAddDomainEvent() {
        // Given
        TestDomainEvent event = new TestDomainEvent("test-data");
        
        // When
        aggregateRoot.addTestEvent(event);
        
        // Then
        List<DomainEvent> events = aggregateRoot.getDomainEvents();
        assertEquals(1, events.size());
        assertEquals(event, events.get(0));
        assertTrue(aggregateRoot.hasDomainEvents());
    }
    
    @Test
    void shouldAddMultipleDomainEvents() {
        // Given
        TestDomainEvent event1 = new TestDomainEvent("test-data-1");
        TestDomainEvent event2 = new TestDomainEvent("test-data-2");
        
        // When
        aggregateRoot.addTestEvent(event1);
        aggregateRoot.addTestEvent(event2);
        
        // Then
        List<DomainEvent> events = aggregateRoot.getDomainEvents();
        assertEquals(2, events.size());
        assertEquals(event1, events.get(0));
        assertEquals(event2, events.get(1));
        assertTrue(aggregateRoot.hasDomainEvents());
    }
    
    @Test
    void shouldReturnUnmodifiableListOfDomainEvents() {
        // Given
        TestDomainEvent event = new TestDomainEvent("test-data");
        aggregateRoot.addTestEvent(event);
        
        // When
        List<DomainEvent> events = aggregateRoot.getDomainEvents();
        
        // Then
        assertThrows(UnsupportedOperationException.class, () -> {
            events.add(new TestDomainEvent("another-event"));
        });
    }
    
    @Test
    void shouldClearAllDomainEvents() {
        // Given
        TestDomainEvent event1 = new TestDomainEvent("test-data-1");
        TestDomainEvent event2 = new TestDomainEvent("test-data-2");
        aggregateRoot.addTestEvent(event1);
        aggregateRoot.addTestEvent(event2);
        
        // When
        aggregateRoot.clearDomainEvents();
        
        // Then
        List<DomainEvent> events = aggregateRoot.getDomainEvents();
        assertTrue(events.isEmpty());
        assertFalse(aggregateRoot.hasDomainEvents());
    }
    
    @Test
    void shouldReturnTrueWhenHasDomainEvents() {
        // Given
        TestDomainEvent event = new TestDomainEvent("test-data");
        
        // When
        aggregateRoot.addTestEvent(event);
        
        // Then
        assertTrue(aggregateRoot.hasDomainEvents());
    }
    
    @Test
    void shouldReturnFalseWhenNoDomainEvents() {
        // When & Then
        assertFalse(aggregateRoot.hasDomainEvents());
    }
    
    @Test
    void shouldReturnFalseAfterClearingDomainEvents() {
        // Given
        TestDomainEvent event = new TestDomainEvent("test-data");
        aggregateRoot.addTestEvent(event);
        
        // When
        aggregateRoot.clearDomainEvents();
        
        // Then
        assertFalse(aggregateRoot.hasDomainEvents());
    }
    
    // Test implementation of AggregateRoot
    private static class TestAggregateRoot extends AggregateRoot {
        public void addTestEvent(DomainEvent event) {
            addDomainEvent(event);
        }
    }
    
    // Test implementation of DomainEvent
    private static class TestDomainEvent extends DomainEvent {
        private final String data;
        
        public TestDomainEvent(String data) {
            super();
            this.data = data;
        }
        
        public String getData() {
            return data;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestDomainEvent that = (TestDomainEvent) o;
            return data.equals(that.data);
        }
        
        @Override
        public int hashCode() {
            return data.hashCode();
        }
    }
}