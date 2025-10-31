package com.example.orderservice.domain.event;

import com.example.logging.annotation.LogMessageEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 日誌功能驗證測試 - 確保所有事件處理方法都正確配置了日誌記錄
 */
class LoggingFunctionalityValidationTest {

    @Test
    void shouldVerifyAllEventHandlingMethodsHaveLoggingAnnotations() throws NoSuchMethodException {
        // 驗證DomainEventPublisher的所有關鍵方法都有@LogMessageEvent註解
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;
        
        // 檢查公共方法
        Method publishEventsMethod = publisherClass.getMethod("publishEvents", List.class);
        LogMessageEvent publishEventsAnnotation = publishEventsMethod.getAnnotation(LogMessageEvent.class);
        assertThat(publishEventsAnnotation).isNotNull();
        assertThat(publishEventsAnnotation.eventType()).isEqualTo("DOMAIN_EVENTS_PUBLISHED");
        assertThat(publishEventsAnnotation.logPayload()).isTrue();

        Method publishEventMethod = publisherClass.getMethod("publishEvent", 
            com.example.orderservice.domain.shared.DomainEvent.class);
        LogMessageEvent publishEventAnnotation = publishEventMethod.getAnnotation(LogMessageEvent.class);
        assertThat(publishEventAnnotation).isNotNull();
        assertThat(publishEventAnnotation.eventType()).isEqualTo("DOMAIN_EVENT_PUBLISHED");
        assertThat(publishEventAnnotation.logPayload()).isTrue();
    }

    @Test
    void shouldVerifyPrivateEventHandlerMethodsHaveLoggingAnnotations() throws NoSuchMethodException {
        // 驗證私有事件處理方法的日誌註解
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;
        
        Method handlePaymentRequestedMethod = publisherClass.getDeclaredMethod("handlePaymentRequestedEvent", 
            com.example.orderservice.domain.order.event.PaymentRequestedEvent.class);
        LogMessageEvent handlePaymentRequestedAnnotation = handlePaymentRequestedMethod.getAnnotation(LogMessageEvent.class);
        assertThat(handlePaymentRequestedAnnotation).isNotNull();
        assertThat(handlePaymentRequestedAnnotation.eventType()).isEqualTo("PAYMENT_REQUESTED_EVENT_HANDLED");
        assertThat(handlePaymentRequestedAnnotation.logPayload()).isTrue();

        Method handlePaymentConfirmedMethod = publisherClass.getDeclaredMethod("handlePaymentConfirmedEvent", 
            com.example.orderservice.domain.order.event.PaymentConfirmedEvent.class);
        LogMessageEvent handlePaymentConfirmedAnnotation = handlePaymentConfirmedMethod.getAnnotation(LogMessageEvent.class);
        assertThat(handlePaymentConfirmedAnnotation).isNotNull();
        assertThat(handlePaymentConfirmedAnnotation.eventType()).isEqualTo("PAYMENT_CONFIRMED_EVENT_HANDLED");
        assertThat(handlePaymentConfirmedAnnotation.logPayload()).isTrue();

        Method handlePaymentFailedMethod = publisherClass.getDeclaredMethod("handlePaymentFailedEvent", 
            com.example.orderservice.domain.order.event.PaymentFailedEvent.class);
        LogMessageEvent handlePaymentFailedAnnotation = handlePaymentFailedMethod.getAnnotation(LogMessageEvent.class);
        assertThat(handlePaymentFailedAnnotation).isNotNull();
        assertThat(handlePaymentFailedAnnotation.eventType()).isEqualTo("PAYMENT_FAILED_EVENT_HANDLED");
        assertThat(handlePaymentFailedAnnotation.logPayload()).isTrue();
    }

    @Test
    void shouldVerifyLoggingAnnotationConfiguration() throws NoSuchMethodException {
        // 驗證日誌註解的配置是否正確
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;
        
        Method[] methods = publisherClass.getDeclaredMethods();
        List<String> expectedEventTypes = Arrays.asList(
            "DOMAIN_EVENTS_PUBLISHED",
            "DOMAIN_EVENT_PUBLISHED", 
            "PAYMENT_REQUESTED_EVENT_HANDLED",
            "PAYMENT_CONFIRMED_EVENT_HANDLED",
            "PAYMENT_FAILED_EVENT_HANDLED"
        );
        
        int annotatedMethodCount = 0;
        for (Method method : methods) {
            LogMessageEvent annotation = method.getAnnotation(LogMessageEvent.class);
            if (annotation != null) {
                annotatedMethodCount++;
                
                // 驗證事件類型是預期的
                assertThat(expectedEventTypes).contains(annotation.eventType());
                
                // 驗證日誌配置
                assertThat(annotation.logPayload()).isTrue();
                assertThat(annotation.eventType()).isNotEmpty();
            }
        }
        
        // 確保有足夠的方法被註解
        assertThat(annotatedMethodCount).isGreaterThanOrEqualTo(5);
    }

    @Test
    void shouldVerifyEventTypeNamingConvention() throws NoSuchMethodException {
        // 驗證事件類型命名約定
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;
        
        Method[] methods = publisherClass.getDeclaredMethods();
        for (Method method : methods) {
            LogMessageEvent annotation = method.getAnnotation(LogMessageEvent.class);
            if (annotation != null) {
                String eventType = annotation.eventType();
                
                // 驗證事件類型命名約定
                assertThat(eventType).isUpperCase();
                assertThat(eventType).contains("_");
                
                // 驗證特定的命名模式
                if (method.getName().startsWith("handle")) {
                    assertThat(eventType).endsWith("_HANDLED");
                } else if (method.getName().startsWith("publish")) {
                    assertThat(eventType).endsWith("_PUBLISHED");
                }
            }
        }
    }

    @Test
    void shouldVerifyLoggingPayloadConfiguration() throws NoSuchMethodException {
        // 驗證所有事件處理方法都啟用了載荷日誌記錄
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;
        
        Method[] methods = publisherClass.getDeclaredMethods();
        for (Method method : methods) {
            LogMessageEvent annotation = method.getAnnotation(LogMessageEvent.class);
            if (annotation != null) {
                // 所有事件處理方法都應該記錄載荷
                assertThat(annotation.logPayload())
                    .as("Method %s should have logPayload=true", method.getName())
                    .isTrue();
            }
        }
    }

    @Test
    void shouldVerifyEventHandlerClassesHaveLoggingSupport() {
        // 驗證事件處理器類是否支持日誌記錄
        Class<?>[] eventHandlerClasses = {
            DomainEventPublisher.class,
            DomainEventToMessageConverter.class
        };
        
        for (Class<?> handlerClass : eventHandlerClasses) {
            Method[] methods = handlerClass.getDeclaredMethods();
            boolean hasLoggingAnnotation = false;
            
            for (Method method : methods) {
                if (method.getAnnotation(LogMessageEvent.class) != null) {
                    hasLoggingAnnotation = true;
                    break;
                }
            }
            
            // 至少應該有一個方法有日誌註解（對於DomainEventPublisher）
            if (handlerClass == DomainEventPublisher.class) {
                assertThat(hasLoggingAnnotation)
                    .as("Class %s should have at least one method with @LogMessageEvent", handlerClass.getSimpleName())
                    .isTrue();
            }
        }
    }

    @Test
    void shouldVerifyLoggingAnnotationConsistency() throws NoSuchMethodException {
        // 驗證日誌註解的一致性
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;
        
        // 檢查所有帶註解的方法
        Method[] methods = publisherClass.getDeclaredMethods();
        for (Method method : methods) {
            LogMessageEvent annotation = method.getAnnotation(LogMessageEvent.class);
            if (annotation != null) {
                // 驗證註解屬性的一致性
                assertThat(annotation.eventType()).isNotNull();
                assertThat(annotation.eventType()).isNotEmpty();
                assertThat(annotation.eventType()).doesNotContainIgnoringCase("null");
                
                // 驗證事件類型格式
                assertThat(annotation.eventType()).matches("^[A-Z_]+$");
            }
        }
    }
}