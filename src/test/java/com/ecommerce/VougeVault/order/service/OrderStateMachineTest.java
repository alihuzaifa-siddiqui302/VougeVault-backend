package com.ecommerce.VougeVault.order.service;

import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    private OrderStateMachine stateMachine;
    private Order order;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
        order = new Order();
    }

    @Nested
    @DisplayName("transition() - Valid Transitions")
    class ValidTransitionTests {

        @ParameterizedTest(name = "From {0} to {1}")
        @CsvSource({
                "CREATED, PAYMENT_PENDING",
                "CREATED, CANCELLED",
                "PAYMENT_PENDING, PAID",
                "PAYMENT_PENDING, CANCELLED",
                "PAID, CONFIRMED",
                "PAID, REFUNDED",
                "CONFIRMED, SHIPPED",
                "CONFIRMED, CANCELLED",
                "SHIPPED, DELIVERED"
        })
        @DisplayName("Should successfully transition between allowed states")
        void transition_ShouldUpdateStatus_WhenTransitionIsValid(OrderStatus from, OrderStatus to) {
            order.setStatus(from);

            stateMachine.transition(order, to);

            assertThat(order.getStatus()).isEqualTo(to);
        }
    }

    @Nested
    @DisplayName("transition() - Invalid & Terminal Transitions")
    class InvalidTransitionTests {

        @Test
        @DisplayName("Should throw IllegalStateException when jumping states illegally")
        void transition_ShouldThrowException_WhenJumpingStates() {
            order.setStatus(OrderStatus.CREATED);

            assertThatThrownBy(() -> stateMachine.transition(order, OrderStatus.DELIVERED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot transition order from CREATED to DELIVERED");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when attempting backward transition")
        void transition_ShouldThrowException_WhenAttemptingBackwardTransition() {
            order.setStatus(OrderStatus.SHIPPED);

            assertThatThrownBy(() -> stateMachine.transition(order, OrderStatus.CONFIRMED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot transition order from SHIPPED to CONFIRMED");
        }

        @ParameterizedTest(name = "Terminal state: {0}")
        @EnumSource(value = OrderStatus.class, names = {"DELIVERED", "CANCELLED", "REFUNDED"})
        @DisplayName("Should throw IllegalStateException when attempting to transition out of terminal states")
        void transition_ShouldThrowException_FromTerminalStates(OrderStatus terminalStatus) {
            order.setStatus(terminalStatus);

            assertThatThrownBy(() -> stateMachine.transition(order, OrderStatus.CREATED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot transition order from " + terminalStatus + " to CREATED");
        }
    }

    @Nested
    @DisplayName("canCancel() Tests")
    class CanCancelTests {

        @ParameterizedTest(name = "Status {0} should be cancellable")
        @EnumSource(value = OrderStatus.class, names = {"CREATED", "PAYMENT_PENDING", "CONFIRMED"})
        @DisplayName("Should return true for cancellable statuses")
        void canCancel_ShouldReturnTrue_ForCancellableStatuses(OrderStatus status) {
            boolean result = stateMachine.canCancel(status);

            assertThat(result).isTrue();
        }

        @ParameterizedTest(name = "Status {0} should NOT be cancellable")
        @EnumSource(value = OrderStatus.class, names = {"PAID", "SHIPPED", "DELIVERED", "CANCELLED", "REFUNDED"})
        @DisplayName("Should return false for non-cancellable statuses")
        void canCancel_ShouldReturnFalse_ForNonCancellableStatuses(OrderStatus status) {
            boolean result = stateMachine.canCancel(status);

            assertThat(result).isFalse();
        }
    }
}