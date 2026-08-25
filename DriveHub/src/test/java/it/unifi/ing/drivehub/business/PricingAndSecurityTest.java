package it.unifi.ing.drivehub.business;

import it.unifi.ing.drivehub.business.security.Pbkdf2PasswordHasher;
import it.unifi.ing.drivehub.business.strategies.StandardPricingStrategy;
import it.unifi.ing.drivehub.domain.sales.Discount;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PricingAndSecurityTest {
    @Test
    @DisplayName("RF-PRICE-02: a discount applies only to its exact target vehicle")
    void discountTargetsOneVehicle() {
        Vehicle target = TestFixtures.saleVehicle(30L, "AB123CD");
        Vehicle other = TestFixtures.saleVehicle(31L, "CD456EF");
        LocalDate today = LocalDate.of(2026, 8, 23);
        Discount discount = Discount.create("Target promotion", target, new BigDecimal("10"),
                today.minusDays(1), today.plusDays(1));
        StandardPricingStrategy strategy = new StandardPricingStrategy();

        assertEquals(new BigDecimal("36000.00"), strategy.salePrice(target, List.of(discount), today));
        assertEquals(new BigDecimal("40000.00"), strategy.salePrice(other, List.of(discount), today));
    }

    @Test
    @DisplayName("RF-PRICE-03: promotions do not stack; the best applicable one wins")
    void bestPromotionWins() {
        Vehicle target = TestFixtures.rentalVehicle(30L, "AB123CD");
        LocalDate today = LocalDate.of(2026, 8, 23);
        Discount five = Discount.create("Five", target, new BigDecimal("5"), today, today);
        Discount twenty = Discount.create("Twenty", target, new BigDecimal("20"), today, today);

        BigDecimal quoted = new StandardPricingStrategy().rentalPrice(
                target, 3, List.of(five, twenty), today);

        assertEquals(new BigDecimal("240.00"), quoted);
    }

    @Test
    @DisplayName("RF-PRICE-04: the ER discount row can be disabled and replaced")
    void discountCanBeReactivatedWithNewTerms() {
        Vehicle target = TestFixtures.saleVehicle(30L, "AB123CD");
        LocalDate today = LocalDate.of(2026, 8, 23);
        Discount discount = Discount.create("Summer", target, new BigDecimal("10"), today, today);
        discount.disable();

        discount.replace("Autumn", new BigDecimal("15"), today.plusDays(1), today.plusDays(30));

        assertTrue(discount.enabled());
        assertEquals("Autumn", discount.name());
        assertEquals(new BigDecimal("15"), discount.percentage());
        assertEquals(today.plusDays(1), discount.startsOn());
    }

    @Test
    @DisplayName("SEC-AUTH-01: PBKDF2 stores salt and verifies without retaining plaintext")
    void pbkdf2RoundTrip() {
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(10_000, 16, 128, new SecureRandom());

        String first = hasher.hash("correct horse".toCharArray());
        String second = hasher.hash("correct horse".toCharArray());

        assertNotEquals(first, second, "fresh random salts must produce different encodings");
        assertTrue(hasher.verify("correct horse".toCharArray(), first));
        assertFalse(hasher.verify("wrong password".toCharArray(), first));
        assertFalse(hasher.verify("correct horse".toCharArray(), "malformed"));
    }
}
